import json

import joblib
import pytest
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from xgboost import XGBClassifier

from app.data import load_dataset
from app.explain import Explainer
from app.model_store import ModelNotLoadedError, ModelService
from app.preprocessing import build_pipeline, selected_feature_names, transform_for_model


def test_training_writes_artifacts(settings, trained):
    assert settings.model_path.exists()
    metrics = json.loads(settings.metrics_path.read_text())
    assert metrics["best_model"] == "logistic_regression"
    test_scores = metrics["models"]["logistic_regression"]["test"]
    assert test_scores["recall"] > 0.85
    assert set(test_scores["confusion_matrix"]) == {"tn", "fp", "fn", "tp"}


def test_bundle_contents(settings, trained):
    bundle = joblib.load(settings.model_path)
    assert bundle["model_version"] == "test-1"
    assert len(bundle["selected_features"]) == settings.n_selected_features


def test_service_predicts_with_explanations(model_service, sample_rows):
    results = model_service.predict([sample_rows["malignant"], sample_rows["benign"]])
    malignant, benign = results
    assert malignant["probability"] > benign["probability"]
    assert malignant["risk_band"] == "HIGH"
    assert benign["risk_band"] == "LOW"
    assert len(malignant["top_features"]) == model_service.settings.top_features
    first = malignant["top_features"][0]
    assert set(first) == {"feature", "value", "shap_value", "direction"}


def test_service_requires_loaded_model(settings):
    with pytest.raises(ModelNotLoadedError):
        ModelService(settings).predict([{}])


def test_load_rejects_incomplete_bundle(settings, tmp_path):
    path = tmp_path / "model.joblib"
    joblib.dump({"pipeline": None}, path)
    with pytest.raises(ValueError, match="missing keys"):
        ModelService(settings).load(path)


@pytest.mark.parametrize(
    "name,estimator",
    [
        ("random_forest", RandomForestClassifier(n_estimators=30, random_state=0)),
        ("xgboost", XGBClassifier(n_estimators=30, max_depth=3, eval_metric="logloss")),
        ("svm", SVC(probability=True, random_state=0)),
    ],
)
def test_explainer_supports_every_model_family(name, estimator):
    X, y = load_dataset()
    pipe = build_pipeline(estimator, k_features=8).fit(X, y)
    background = transform_for_model(pipe, X.sample(40, random_state=0))
    explainer = Explainer(pipe, name, selected_feature_names(pipe), background)
    rows = explainer.top_features(X.head(2), top_n=3)
    assert len(rows) == 2
    assert all(len(r) == 3 for r in rows)
