import numpy as np
import pytest
from sklearn.linear_model import LogisticRegression

from app.data import FEATURE_NAMES, load_dataset
from app.preprocessing import (
    build_pipeline,
    records_to_frame,
    selected_feature_names,
    split_data,
    transform_for_model,
)


def test_dataset_shape_and_target_encoding():
    X, y = load_dataset()
    assert X.shape == (569, 30)
    assert list(X.columns) == FEATURE_NAMES
    # 212 malignant cases in the Wisconsin dataset, encoded as the positive class
    assert int(y.sum()) == 212


def test_split_is_stratified():
    X, y = load_dataset()
    X_train, X_test, y_train, y_test = split_data(X, y, test_size=0.2, random_state=1)
    assert len(X_test) == 114
    assert abs(y_train.mean() - y_test.mean()) < 0.02


def test_pipeline_selects_k_features_and_scales():
    X, y = load_dataset()
    pipe = build_pipeline(LogisticRegression(max_iter=1000), k_features=10).fit(X, y)
    transformed = transform_for_model(pipe, X)
    assert transformed.shape == (569, 10)
    assert len(selected_feature_names(pipe)) == 10
    scaled = pipe.named_steps["scaler"].transform(X)
    assert np.allclose(scaled.mean(axis=0), 0, atol=1e-8)


def test_smote_is_skipped_at_predict_time():
    X, y = load_dataset()
    pipe = build_pipeline(LogisticRegression(max_iter=1000), k_features=10).fit(X, y)
    assert pipe.predict(X.head(7)).shape == (7,)


def test_records_to_frame_orders_columns():
    X, _ = load_dataset()
    record = X.iloc[0].to_dict()
    shuffled = dict(reversed(list(record.items())))
    frame = records_to_frame([shuffled])
    assert list(frame.columns) == FEATURE_NAMES


def test_records_to_frame_reports_missing_features():
    X, _ = load_dataset()
    record = X.iloc[0].to_dict()
    record.pop("mean_radius")
    with pytest.raises(ValueError, match="mean_radius"):
        records_to_frame([record])
