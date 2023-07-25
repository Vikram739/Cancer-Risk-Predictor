"""Exploratory data analysis. Saves plots and summary tables to reports/.

Run with:  python -m app.eda
"""
import logging

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import seaborn as sns  # noqa: E402

from .config import get_settings  # noqa: E402
from .data import FEATURE_NAMES, TARGET_COLUMN, load_frame  # noqa: E402

log = logging.getLogger(__name__)


def run_eda(reports_dir=None) -> dict:
    reports_dir = reports_dir or get_settings().reports_dir
    reports_dir.mkdir(parents=True, exist_ok=True)
    df = load_frame()
    label = df[TARGET_COLUMN].map({0: "Benign", 1: "Malignant"})

    df.describe().T.round(4).to_csv(reports_dir / "feature_summary.csv")

    fig, ax = plt.subplots(figsize=(5, 4))
    sns.countplot(x=label, order=["Benign", "Malignant"], palette=["#4c9f70", "#d1495b"], ax=ax)
    ax.set_xlabel("")
    ax.set_ylabel("Samples")
    ax.set_title("Class distribution")
    fig.tight_layout()
    fig.savefig(reports_dir / "class_distribution.png", dpi=120)
    plt.close(fig)

    corr = df[FEATURE_NAMES].corr()
    fig, ax = plt.subplots(figsize=(14, 12))
    sns.heatmap(corr, cmap="coolwarm", center=0, square=True, cbar_kws={"shrink": 0.7}, ax=ax)
    ax.set_title("Feature correlation")
    fig.tight_layout()
    fig.savefig(reports_dir / "correlation_heatmap.png", dpi=110)
    plt.close(fig)

    target_corr = df[FEATURE_NAMES].corrwith(df[TARGET_COLUMN]).abs().sort_values(ascending=False)
    top = list(target_corr.index[:6])
    fig, axes = plt.subplots(2, 3, figsize=(13, 7))
    for ax, col in zip(axes.ravel(), top):
        sns.kdeplot(data=df.assign(diagnosis=label), x=col, hue="diagnosis", fill=True, common_norm=False,
                    palette={"Benign": "#4c9f70", "Malignant": "#d1495b"}, ax=ax)
        ax.set_title(col.replace("_", " "))
    fig.suptitle("Distribution of the most target correlated features")
    fig.tight_layout()
    fig.savefig(reports_dir / "top_feature_distributions.png", dpi=110)
    plt.close(fig)

    summary = {
        "samples": int(len(df)),
        "malignant": int(df[TARGET_COLUMN].sum()),
        "benign": int((df[TARGET_COLUMN] == 0).sum()),
        "missing_values": int(df.isna().sum().sum()),
        "top_correlated_features": {k: round(float(v), 4) for k, v in target_corr.head(10).items()},
    }
    log.info("eda summary: %s", summary)
    return summary


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    print(run_eda())
