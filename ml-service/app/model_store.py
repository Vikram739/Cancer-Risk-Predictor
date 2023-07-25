"""Loads the persisted model bundle and serves predictions."""
import json
import logging
import threading
from pathlib import Path
from typing import List, Mapping, Optional

import joblib

from .config import Settings, get_settings
from .data import FEATURE_NAMES, feature_ranges
from .explain import Explainer
from .preprocessing import records_to_frame
from .risk import RiskBands

log = logging.getLogger(__name__)


class ModelNotLoadedError(RuntimeError):
    pass


class ModelService:
    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.bands = RiskBands(self.settings.risk_low_max, self.settings.risk_high_min)
        self._bundle = None
        self._explainer = None
        self._metrics = None
        self._lock = threading.Lock()

    @property
    def loaded(self) -> bool:
        return self._bundle is not None

    def load(self, path: Optional[Path] = None) -> None:
        path = Path(path or self.settings.model_path)
        with self._lock:
            bundle = joblib.load(path)
            missing = {"pipeline", "model_name", "selected_features", "background"} - set(bundle)
            if missing:
                raise ValueError(f"model bundle is missing keys: {sorted(missing)}")
            if bundle.get("feature_names") != FEATURE_NAMES:
                raise ValueError("model was trained on a different feature set")
            self._explainer = Explainer(
                bundle["pipeline"], bundle["model_name"], bundle["selected_features"], bundle["background"]
            )
            self._bundle = bundle
            metrics_path = path.parent / "metrics.json"
            self._metrics = json.loads(metrics_path.read_text()) if metrics_path.exists() else None
        log.info("loaded model %s version %s from %s", bundle["model_name"], bundle.get("model_version"), path)

    def try_load(self) -> bool:
        try:
            self.load()
            return True
        except FileNotFoundError:
            log.warning("no model found at %s, run training first", self.settings.model_path)
        except Exception:  # pragma: no cover - logged for operators
            log.exception("failed to load model")
        return False

    def _require(self):
        if not self.loaded:
            raise ModelNotLoadedError("model is not loaded")
        return self._bundle

    def predict(self, records: List[Mapping[str, float]], references: Optional[List[Optional[str]]] = None) -> List[dict]:
        bundle = self._require()
        frame = records_to_frame(records)
        probabilities = bundle["pipeline"].predict_proba(frame)[:, 1]
        explanations = self._explainer.top_features(frame, self.settings.top_features)
        references = references or [None] * len(records)
        results = []
        for ref, proba, top in zip(references, probabilities, explanations):
            proba = float(proba)
            results.append(
                {
                    "reference": ref,
                    "probability": round(proba, 4),
                    "prediction": "malignant" if proba >= 0.5 else "benign",
                    "risk_band": self.bands.classify(proba),
                    "model_version": bundle.get("model_version"),
                    "model_name": bundle["model_name"],
                    "top_features": top,
                }
            )
        return results

    def info(self) -> dict:
        bundle = self._require()
        return {
            "model_name": bundle["model_name"],
            "model_version": bundle.get("model_version"),
            "trained_at": bundle.get("trained_at"),
            "features": FEATURE_NAMES,
            "selected_features": bundle["selected_features"],
            "feature_ranges": feature_ranges(),
            "risk_thresholds": self.bands.as_dict(),
            "metrics": self._metrics,
        }
