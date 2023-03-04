import pytest

from app.risk import HIGH, LOW, MODERATE, RiskBands


@pytest.mark.parametrize(
    "probability,band",
    [(0.0, LOW), (0.2999, LOW), (0.30, MODERATE), (0.55, MODERATE), (0.70, HIGH), (1.0, HIGH)],
)
def test_default_bands(probability, band):
    assert RiskBands().classify(probability) == band


def test_custom_thresholds():
    bands = RiskBands(low_max=0.1, high_min=0.5)
    assert bands.classify(0.2) == MODERATE
    assert bands.classify(0.5) == HIGH


@pytest.mark.parametrize("low,high", [(0.7, 0.3), (0.0, 0.5), (0.5, 1.0)])
def test_invalid_thresholds(low, high):
    with pytest.raises(ValueError):
        RiskBands(low_max=low, high_min=high)


def test_probability_out_of_range():
    with pytest.raises(ValueError):
        RiskBands().classify(1.2)
