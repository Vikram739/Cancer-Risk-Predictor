"""Map a malignancy probability to a risk band."""
from dataclasses import dataclass

LOW = "LOW"
MODERATE = "MODERATE"
HIGH = "HIGH"


@dataclass(frozen=True)
class RiskBands:
    low_max: float = 0.30
    high_min: float = 0.70

    def __post_init__(self):
        if not 0.0 < self.low_max < self.high_min < 1.0:
            raise ValueError("thresholds must satisfy 0 < low_max < high_min < 1")

    def classify(self, probability: float) -> str:
        if not 0.0 <= probability <= 1.0:
            raise ValueError("probability must be between 0 and 1")
        if probability < self.low_max:
            return LOW
        if probability >= self.high_min:
            return HIGH
        return MODERATE

    def as_dict(self) -> dict:
        return {"low_max": self.low_max, "high_min": self.high_min}
