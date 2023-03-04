from dataclasses import replace

from app.api import create_app
from app.model_store import ModelService


def test_health(client):
    res = client.get("/health")
    assert res.status_code == 200
    assert res.get_json() == {"status": "ok", "model_loaded": True}


def test_health_degraded_without_model(settings, tmp_path):
    app = create_app(ModelService(replace(settings, model_dir=tmp_path)))
    res = app.test_client().get("/health")
    assert res.status_code == 503
    assert res.get_json()["status"] == "degraded"


def test_predict_returns_risk_band(client, sample_rows):
    res = client.post("/predict", json={"features": sample_rows["malignant"], "reference": "p-001"})
    assert res.status_code == 200
    body = res.get_json()
    assert body["reference"] == "p-001"
    assert body["risk_band"] == "HIGH"
    assert body["prediction"] == "malignant"
    assert body["model_version"] == "test-1"
    assert len(body["top_features"]) > 0


def test_predict_rejects_missing_feature(client, sample_rows):
    features = dict(sample_rows["benign"])
    features.pop("worst_area")
    res = client.post("/predict", json={"features": features})
    assert res.status_code == 422
    error = res.get_json()["error"]
    assert error["code"] == "validation_error"
    assert any(d["field"] == "features.worst_area" for d in error["details"])


def test_predict_rejects_negative_and_unknown_values(client, sample_rows):
    features = dict(sample_rows["benign"], mean_radius=-1, unknown=3)
    res = client.post("/predict", json={"features": features})
    assert res.status_code == 422
    fields = {d["field"] for d in res.get_json()["error"]["details"]}
    assert {"features.mean_radius", "features.unknown"} <= fields


def test_predict_rejects_non_json(client):
    res = client.post("/predict", data="nope", content_type="text/plain")
    assert res.status_code == 400
    assert res.get_json()["error"]["code"] == "invalid_json"


def test_batch_predict(client, sample_rows):
    payload = {
        "records": [
            {"features": sample_rows["malignant"], "reference": "a"},
            {"features": sample_rows["benign"], "reference": "b"},
        ]
    }
    res = client.post("/predict/batch", json=payload)
    assert res.status_code == 200
    body = res.get_json()
    assert body["count"] == 2
    assert [r["reference"] for r in body["results"]] == ["a", "b"]


def test_batch_rejects_empty(client):
    res = client.post("/predict/batch", json={"records": []})
    assert res.status_code == 422


def test_model_info(client):
    res = client.get("/model/info")
    assert res.status_code == 200
    body = res.get_json()
    assert body["model_name"] == "logistic_regression"
    assert len(body["features"]) == 30
    assert body["risk_thresholds"] == {"low_max": 0.3, "high_min": 0.7}
    assert "mean_radius" in body["feature_ranges"]
    assert body["metrics"]["best_model"] == "logistic_regression"


def test_unknown_route_uses_json_error(client):
    res = client.get("/nope")
    assert res.status_code == 404
    assert res.get_json()["error"]["code"] == "not_found"
