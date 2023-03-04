import pytest

from app.api import create_app
from app.config import Settings
from app.data import load_dataset
from app.model_store import ModelService
from app.train import run_training


@pytest.fixture(scope="session")
def settings(tmp_path_factory):
    root = tmp_path_factory.mktemp("ml")
    return Settings(model_dir=root / "artifacts", reports_dir=root / "reports", model_version="test-1")


@pytest.fixture(scope="session")
def trained(settings):
    return run_training(settings, fast=True, make_plots=False)


@pytest.fixture(scope="session")
def model_service(settings, trained):
    service = ModelService(settings)
    service.load()
    return service


@pytest.fixture()
def client(model_service):
    app = create_app(model_service)
    app.config.update(TESTING=True)
    return app.test_client()


@pytest.fixture(scope="session")
def sample_rows():
    X, y = load_dataset()
    malignant = X[y == 1].iloc[0].to_dict()
    benign = X[y == 0].iloc[0].to_dict()
    return {"malignant": malignant, "benign": benign}
