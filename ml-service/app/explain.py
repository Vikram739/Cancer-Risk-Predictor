"""SHAP based explanations for individual predictions."""
import logging
from typing import List

import numpy as np
import pandas as pd

# shap 0.41 still references the numpy scalar aliases that numpy 1.24 removed,
# restore them before shap is imported
for _alias, _type in (("bool", bool), ("int", int), ("float", float), ("object", object)):
    if _alias not in np.__dict__:
        setattr(np, _alias, _type)

import shap  # noqa: E402

from .preprocessing import transform_for_model  # noqa: E402

log = logging.getLogger(__name__)

TREE_MODELS = {"random_forest", "xgboost"}
LINEAR_MODELS = {"logistic_regression"}


def _positive_class(values):
    """Some explainers return one array per class, keep the malignant one."""
    if isinstance(values, list):
        return np.asarray(values[1])
    values = np.asarray(values)
    if values.ndim == 3:
        return values[:, :, 1]
    return values


class Explainer:
    """Wraps the right SHAP explainer for the trained model type."""

    def __init__(self, pipeline, model_name: str, selected_features: List[str], background: np.ndarray):
        self.pipeline = pipeline
        self.model_name = model_name
        self.selected_features = selected_features
        clf = pipeline.named_steps["clf"]
        if model_name in TREE_MODELS:
            self._explainer = shap.TreeExplainer(clf)
            self._kernel = False
        elif model_name in LINEAR_MODELS:
            self._explainer = shap.LinearExplainer(clf, background)
            self._kernel = False
        else:
            # model agnostic fallback, summarise the background to keep it fast
            summary = shap.kmeans(background, min(10, len(background)))
            self._explainer = shap.KernelExplainer(clf.predict_proba, summary)
            self._kernel = True
        log.info("initialised %s for %s", type(self._explainer).__name__, model_name)

    def shap_values(self, X: pd.DataFrame) -> np.ndarray:
        transformed = transform_for_model(self.pipeline, X)
        if self._kernel:
            raw = self._explainer.shap_values(transformed, nsamples=100, silent=True)
        else:
            raw = self._explainer.shap_values(transformed)
        return _positive_class(raw)

    def top_features(self, X: pd.DataFrame, top_n: int = 5) -> List[List[dict]]:
        """Return the top_n features by absolute SHAP value for every row of X."""
        values = self.shap_values(X)
        results = []
        for row_idx in range(values.shape[0]):
            row = values[row_idx]
            order = np.argsort(-np.abs(row))[:top_n]
            items = []
            for i in order:
                name = self.selected_features[i]
                contribution = float(row[i])
                items.append(
                    {
                        "feature": name,
                        "value": float(X.iloc[row_idx][name]),
                        "shap_value": round(contribution, 6),
                        "direction": "increases_risk" if contribution >= 0 else "decreases_risk",
                    }
                )
            results.append(items)
        return results
