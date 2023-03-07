"""Request validation models (pydantic v1)."""
from typing import List, Optional

from pydantic import BaseModel, Extra, Field, conlist, create_model

from .data import FEATURE_NAMES

# every Wisconsin feature is a non negative measurement; the upper bound only
# rejects obviously broken input such as a misplaced decimal point
_feature_fields = {name: (float, Field(..., ge=0, le=10000)) for name in FEATURE_NAMES}


class _StrictConfig:
    extra = Extra.forbid
    anystr_strip_whitespace = True


Features = create_model("Features", __config__=_StrictConfig, **_feature_fields)


class PredictRequest(BaseModel):
    features: Features
    reference: Optional[str] = Field(None, max_length=64, description="caller supplied id echoed back")

    class Config:
        extra = Extra.forbid


class BatchPredictRequest(BaseModel):
    records: conlist(PredictRequest, min_items=1)

    class Config:
        extra = Extra.forbid


def format_validation_errors(errors: List[dict]) -> List[dict]:
    return [
        {"field": ".".join(str(p) for p in err["loc"]), "message": err["msg"]}
        for err in errors
    ]
