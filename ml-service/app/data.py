"""Dataset loading helpers.

Uses the Breast Cancer Wisconsin (Diagnostic) dataset bundled with
scikit-learn, so no external download is needed.
"""
from typing import List, Tuple

import pandas as pd
from sklearn.datasets import load_breast_cancer

TARGET_COLUMN = "malignant"


def _snake(name: str) -> str:
    return name.strip().lower().replace(" ", "_")


def _raw_dataset():
    return load_breast_cancer(as_frame=True)


FEATURE_NAMES: List[str] = [_snake(n) for n in _raw_dataset().feature_names]


def load_dataset() -> Tuple[pd.DataFrame, pd.Series]:
    """Return features and a binary target where 1 means malignant.

    scikit-learn encodes malignant as 0 and benign as 1, which is the opposite
    of what we want when the positive class is the one we must not miss.
    """
    raw = _raw_dataset()
    X = raw.data.copy()
    X.columns = FEATURE_NAMES
    y = (raw.target == 0).astype(int)
    y.name = TARGET_COLUMN
    return X, y


def load_frame() -> pd.DataFrame:
    X, y = load_dataset()
    return pd.concat([X, y], axis=1)


def feature_ranges() -> dict:
    """Observed min, max and median for every feature, used by the UI for defaults."""
    X, _ = load_dataset()
    return {
        col: {
            "min": float(X[col].min()),
            "max": float(X[col].max()),
            "median": float(X[col].median()),
        }
        for col in FEATURE_NAMES
    }
