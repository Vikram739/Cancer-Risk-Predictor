"""Preprocessing building blocks shared by training and inference."""
from typing import Iterable, Mapping, Tuple

import numpy as np
import pandas as pd
from imblearn.over_sampling import SMOTE
from imblearn.pipeline import Pipeline
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

from .data import FEATURE_NAMES


def split_data(X: pd.DataFrame, y: pd.Series, test_size: float = 0.2, random_state: int = 42) -> Tuple:
    """Stratified split so both sets keep the original malignant ratio."""
    return train_test_split(X, y, test_size=test_size, stratify=y, random_state=random_state)


def build_pipeline(estimator, k_features: int = 20, random_state: int = 42) -> Pipeline:
    """Scale, select the k most informative features, oversample with SMOTE, then fit.

    SMOTE lives inside the imblearn pipeline so it is only applied to training
    folds during cross-validation and never at prediction time.
    """
    return Pipeline(
        steps=[
            ("scaler", StandardScaler()),
            ("selector", SelectKBest(score_func=f_classif, k=k_features)),
            ("smote", SMOTE(random_state=random_state)),
            ("clf", estimator),
        ]
    )


def records_to_frame(records: Iterable[Mapping[str, float]]) -> pd.DataFrame:
    """Turn a list of feature dicts into a frame with the training column order."""
    frame = pd.DataFrame(list(records))
    missing = [c for c in FEATURE_NAMES if c not in frame.columns]
    if missing:
        raise ValueError(f"missing features: {', '.join(missing)}")
    return frame[FEATURE_NAMES].astype(float)


def transform_for_model(pipeline: Pipeline, X: pd.DataFrame) -> np.ndarray:
    """Apply scaler and selector only, which is what the classifier sees."""
    scaled = pipeline.named_steps["scaler"].transform(X)
    return pipeline.named_steps["selector"].transform(scaled)


def selected_feature_names(pipeline: Pipeline) -> list:
    mask = pipeline.named_steps["selector"].get_support()
    return [name for name, keep in zip(FEATURE_NAMES, mask) if keep]
