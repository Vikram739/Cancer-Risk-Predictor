"""Central configuration for the ML service.

Values can be overridden with environment variables so the same image can be
used locally, in tests, and inside docker-compose.
"""
import os
from dataclasses import dataclass, field
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent


def _env_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    return float(raw)


@dataclass(frozen=True)
class Settings:
    model_dir: Path = field(default_factory=lambda: Path(os.getenv("MODEL_DIR", BASE_DIR / "artifacts")))
    reports_dir: Path = field(default_factory=lambda: Path(os.getenv("REPORTS_DIR", BASE_DIR / "reports")))
    model_version: str = field(default_factory=lambda: os.getenv("MODEL_VERSION", "1.0.0"))
    random_state: int = 42
    test_size: float = 0.2
    cv_folds: int = 5
    n_selected_features: int = 20
    # probability below low_max is Low, at or above high_min is High, otherwise Moderate
    risk_low_max: float = field(default_factory=lambda: _env_float("RISK_LOW_MAX", 0.30))
    risk_high_min: float = field(default_factory=lambda: _env_float("RISK_HIGH_MIN", 0.70))
    top_features: int = field(default_factory=lambda: int(os.getenv("SHAP_TOP_FEATURES", "5")))
    max_batch_size: int = field(default_factory=lambda: int(os.getenv("MAX_BATCH_SIZE", "500")))

    @property
    def model_path(self) -> Path:
        return self.model_dir / "model.joblib"

    @property
    def metrics_path(self) -> Path:
        return self.model_dir / "metrics.json"


def get_settings() -> Settings:
    return Settings()
