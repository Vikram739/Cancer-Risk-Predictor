"""Train, tune and compare candidate models, then persist the best one.

Run with:  python -m app.train
"""
import argparse
import json
import logging
from datetime import datetime, timezone
from typing import Dict

import joblib
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import seaborn as sns  # noqa: E402
from sklearn.ensemble import RandomForestClassifier  # noqa: E402
from sklearn.linear_model import LogisticRegression  # noqa: E402
from sklearn.metrics import (  # noqa: E402
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
    roc_curve,
)
from sklearn.model_selection import GridSearchCV, StratifiedKFold  # noqa: E402
from sklearn.svm import SVC  # noqa: E402
from xgboost import XGBClassifier  # noqa: E402

from .config import Settings, get_settings  # noqa: E402
from .data import FEATURE_NAMES, load_dataset  # noqa: E402
from .preprocessing import build_pipeline, selected_feature_names, split_data, transform_for_model  # noqa: E402

log = logging.getLogger(__name__)

DISPLAY_NAMES = {
    "logistic_regression": "Logistic Regression",
    "random_forest": "Random Forest",
    "svm": "Support Vector Machine",
    "xgboost": "XGBoost",
}

SCORING = {
    "recall": "recall",
    "precision": "precision",
    "f1": "f1",
    "accuracy": "accuracy",
    "roc_auc": "roc_auc",
}


def candidate_models(random_state: int, fast: bool = False) -> Dict[str, tuple]:
    """Estimators and their search grids. fast=True keeps grids tiny for tests."""
    models = {
        "logistic_regression": (
            LogisticRegression(max_iter=5000, solver="liblinear", random_state=random_state),
            {"clf__C": [0.01, 0.1, 1.0, 10.0], "clf__penalty": ["l1", "l2"]},
        ),
        "random_forest": (
            RandomForestClassifier(random_state=random_state, n_jobs=-1),
            {
                "clf__n_estimators": [200, 400],
                "clf__max_depth": [None, 6, 10],
                "clf__min_samples_leaf": [1, 3],
            },
        ),
        "svm": (
            SVC(probability=True, random_state=random_state),
            {"clf__C": [0.5, 1.0, 5.0], "clf__kernel": ["rbf", "linear"], "clf__gamma": ["scale"]},
        ),
        "xgboost": (
            XGBClassifier(
                eval_metric="logloss",
                random_state=random_state,
                n_jobs=-1,
                tree_method="hist",
            ),
            {
                "clf__n_estimators": [200, 400],
                "clf__max_depth": [3, 5],
                "clf__learning_rate": [0.05, 0.1],
                "clf__subsample": [0.8, 1.0],
            },
        ),
    }
    if fast:
        return {"logistic_regression": (models["logistic_regression"][0], {"clf__C": [1.0]})}
    return models


def evaluate(y_true, y_pred, y_proba) -> dict:
    tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()
    return {
        "accuracy": round(accuracy_score(y_true, y_pred), 4),
        "precision": round(precision_score(y_true, y_pred), 4),
        "recall": round(recall_score(y_true, y_pred), 4),
        "f1": round(f1_score(y_true, y_pred), 4),
        "roc_auc": round(roc_auc_score(y_true, y_proba), 4),
        "confusion_matrix": {"tn": int(tn), "fp": int(fp), "fn": int(fn), "tp": int(tp)},
    }


def _pick_best(results: dict) -> str:
    # recall first because a missed malignant case is the costliest error,
    # then F1 and ROC-AUC to break ties between equally sensitive models
    return max(
        results,
        key=lambda name: (
            results[name]["cv"]["recall"],
            results[name]["cv"]["f1"],
            results[name]["cv"]["roc_auc"],
        ),
    )


def _plot_roc(curves: dict, best: str, path):
    fig, ax = plt.subplots(figsize=(7, 6))
    for name, (fpr, tpr, auc) in curves.items():
        width = 2.6 if name == best else 1.4
        ax.plot(fpr, tpr, lw=width, label=f"{DISPLAY_NAMES[name]} (AUC {auc:.3f})")
    ax.plot([0, 1], [0, 1], "k--", lw=1)
    ax.set_xlabel("False positive rate")
    ax.set_ylabel("True positive rate")
    ax.set_title("ROC curves on held out test set")
    ax.legend(loc="lower right")
    fig.tight_layout()
    fig.savefig(path, dpi=120)
    plt.close(fig)


def _plot_confusion(cm: dict, title: str, path):
    matrix = np.array([[cm["tn"], cm["fp"]], [cm["fn"], cm["tp"]]])
    fig, ax = plt.subplots(figsize=(5, 4.4))
    sns.heatmap(
        matrix,
        annot=True,
        fmt="d",
        cmap="Blues",
        cbar=False,
        xticklabels=["Benign", "Malignant"],
        yticklabels=["Benign", "Malignant"],
        ax=ax,
    )
    ax.set_xlabel("Predicted")
    ax.set_ylabel("Actual")
    ax.set_title(title)
    fig.tight_layout()
    fig.savefig(path, dpi=120)
    plt.close(fig)


def _plot_comparison(results: dict, path):
    metrics = ["accuracy", "precision", "recall", "f1", "roc_auc"]
    names = list(results)
    x = np.arange(len(metrics))
    width = 0.8 / len(names)
    fig, ax = plt.subplots(figsize=(9, 5))
    for i, name in enumerate(names):
        values = [results[name]["test"][m] for m in metrics]
        ax.bar(x + i * width, values, width, label=DISPLAY_NAMES[name])
    ax.set_xticks(x + width * (len(names) - 1) / 2)
    ax.set_xticklabels(["Accuracy", "Precision", "Recall", "F1", "ROC-AUC"])
    ax.set_ylim(0.85, 1.0)
    ax.set_ylabel("Score")
    ax.set_title("Model comparison on held out test set")
    ax.legend(loc="lower right", fontsize=9)
    fig.tight_layout()
    fig.savefig(path, dpi=120)
    plt.close(fig)


def run_training(settings: Settings = None, fast: bool = False, make_plots: bool = True) -> dict:
    settings = settings or get_settings()
    settings.model_dir.mkdir(parents=True, exist_ok=True)
    settings.reports_dir.mkdir(parents=True, exist_ok=True)

    X, y = load_dataset()
    X_train, X_test, y_train, y_test = split_data(X, y, settings.test_size, settings.random_state)
    log.info("train size %d, test size %d, malignant ratio %.3f", len(X_train), len(X_test), y_train.mean())

    cv = StratifiedKFold(n_splits=settings.cv_folds, shuffle=True, random_state=settings.random_state)
    results, fitted, curves = {}, {}, {}

    for name, (estimator, grid) in candidate_models(settings.random_state, fast).items():
        log.info("tuning %s over %d combinations", name, int(np.prod([len(v) for v in grid.values()])))
        pipeline = build_pipeline(estimator, settings.n_selected_features, settings.random_state)
        search = GridSearchCV(pipeline, grid, scoring=SCORING, refit="recall", cv=cv, n_jobs=-1)
        search.fit(X_train, y_train)

        best_idx = search.best_index_
        cv_scores = {m: round(float(search.cv_results_[f"mean_test_{m}"][best_idx]), 4) for m in SCORING}
        model = search.best_estimator_
        proba = model.predict_proba(X_test)[:, 1]
        pred = (proba >= 0.5).astype(int)
        test_scores = evaluate(y_test, pred, proba)

        fpr, tpr, _ = roc_curve(y_test, proba)
        curves[name] = (fpr, tpr, test_scores["roc_auc"])
        results[name] = {
            "display_name": DISPLAY_NAMES[name],
            "best_params": {k.replace("clf__", ""): v for k, v in search.best_params_.items()},
            "cv": cv_scores,
            "test": test_scores,
        }
        fitted[name] = model
        log.info("%s cv recall %.4f, test recall %.4f", name, cv_scores["recall"], test_scores["recall"])

    best = _pick_best(results)
    best_model = fitted[best]
    selected = selected_feature_names(best_model)
    background = transform_for_model(best_model, X_train.sample(n=min(100, len(X_train)), random_state=settings.random_state))
    trained_at = datetime.now(timezone.utc).isoformat(timespec="seconds")

    bundle = {
        "pipeline": best_model,
        "model_name": best,
        "model_version": settings.model_version,
        "feature_names": FEATURE_NAMES,
        "selected_features": selected,
        "background": background,
        "trained_at": trained_at,
    }
    joblib.dump(bundle, settings.model_path)

    metrics = {
        "model_version": settings.model_version,
        "best_model": best,
        "selection_metric": "recall",
        "trained_at": trained_at,
        "dataset": {
            "name": "Breast Cancer Wisconsin (Diagnostic)",
            "samples": int(len(X)),
            "features": len(FEATURE_NAMES),
            "malignant": int(y.sum()),
            "benign": int((y == 0).sum()),
            "train_size": int(len(X_train)),
            "test_size": int(len(X_test)),
        },
        "selected_features": selected,
        "models": results,
    }
    settings.metrics_path.write_text(json.dumps(metrics, indent=2))

    if make_plots:
        _plot_roc(curves, best, settings.reports_dir / "roc_curve.png")
        _plot_confusion(
            results[best]["test"]["confusion_matrix"],
            f"Confusion matrix: {DISPLAY_NAMES[best]}",
            settings.reports_dir / "confusion_matrix.png",
        )
        _plot_comparison(results, settings.reports_dir / "model_comparison.png")
        (settings.reports_dir / "metrics.json").write_text(json.dumps(metrics, indent=2))

    log.info("best model: %s, saved to %s", best, settings.model_path)
    return metrics


def main():
    parser = argparse.ArgumentParser(description="Train cancer risk models")
    parser.add_argument("--fast", action="store_true", help="tiny grid, for smoke testing")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    metrics = run_training(fast=args.fast)
    best = metrics["best_model"]
    print(json.dumps({"best_model": best, "test": metrics["models"][best]["test"]}, indent=2))


if __name__ == "__main__":
    main()
