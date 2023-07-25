"""Flask application exposing the prediction endpoints."""
import logging
import time
from typing import Optional

from flask import Flask, g, jsonify, request
from flask_cors import CORS
from pydantic import ValidationError
from werkzeug.exceptions import HTTPException

from .config import get_settings
from .model_store import ModelNotLoadedError, ModelService
from .schemas import BatchPredictRequest, PredictRequest, format_validation_errors

log = logging.getLogger(__name__)


class ApiError(Exception):
    def __init__(self, status: int, code: str, message: str, details=None):
        super().__init__(message)
        self.status = status
        self.code = code
        self.message = message
        self.details = details


def _error(status: int, code: str, message: str, details=None):
    body = {"error": {"code": code, "message": message}}
    if details:
        body["error"]["details"] = details
    return jsonify(body), status


def _json_body() -> dict:
    payload = request.get_json(silent=True)
    if payload is None:
        raise ApiError(400, "invalid_json", "request body must be a JSON object")
    if not isinstance(payload, dict):
        raise ApiError(400, "invalid_json", "request body must be a JSON object")
    return payload


def create_app(model_service: Optional[ModelService] = None) -> Flask:
    app = Flask(__name__)
    CORS(app)
    settings = get_settings()
    service = model_service or ModelService(settings)
    if not service.loaded:
        service.try_load()
    app.extensions["model_service"] = service

    @app.before_request
    def _start_timer():
        g.started = time.perf_counter()

    @app.after_request
    def _log_request(response):
        elapsed = (time.perf_counter() - g.get("started", time.perf_counter())) * 1000
        log.info("%s %s %s %.1fms", request.method, request.path, response.status_code, elapsed)
        return response

    @app.errorhandler(ApiError)
    def _api_error(err: ApiError):
        return _error(err.status, err.code, err.message, err.details)

    @app.errorhandler(ValidationError)
    def _validation_error(err: ValidationError):
        return _error(422, "validation_error", "request validation failed", format_validation_errors(err.errors()))

    @app.errorhandler(ModelNotLoadedError)
    def _model_missing(err):
        return _error(503, "model_unavailable", str(err))

    @app.errorhandler(HTTPException)
    def _http_error(err: HTTPException):
        return _error(err.code, err.name.lower().replace(" ", "_"), err.description)

    @app.errorhandler(Exception)
    def _unexpected(err):
        log.exception("unhandled error")
        return _error(500, "internal_error", "unexpected server error")

    @app.get("/health")
    def health():
        status = "ok" if service.loaded else "degraded"
        return jsonify({"status": status, "model_loaded": service.loaded}), (200 if service.loaded else 503)

    @app.get("/model/info")
    def model_info():
        return jsonify(service.info())

    @app.post("/predict")
    def predict():
        body = PredictRequest.parse_obj(_json_body())
        result = service.predict([body.features.dict()], [body.reference])[0]
        return jsonify(result)

    @app.post("/predict/batch")
    def predict_batch():
        body = BatchPredictRequest.parse_obj(_json_body())
        if len(body.records) > settings.max_batch_size:
            raise ApiError(413, "batch_too_large", f"at most {settings.max_batch_size} records per request")
        records = [r.features.dict() for r in body.records]
        refs = [r.reference for r in body.records]
        results = service.predict(records, refs)
        return jsonify({"count": len(results), "results": results})

    return app
