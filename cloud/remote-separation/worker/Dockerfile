ARG PYTHON_BASE=python:3.12.14-slim-trixie@sha256:2f17fc044b579bab302c2e8054d3a686e2cb9a83de48e70534b94cd8ebbe06a9

FROM ${PYTHON_BASE} AS builder

ARG TORCH_VERSION=2.14.0+cpu
ARG DEMUCS_VERSION=4.1.0
ARG MODEL_URL=https://dl.fbaipublicfiles.com/demucs/hybrid_transformer/5c90dfd2-34c22ccb.th
ARG MODEL_SHA256=34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd
ARG MODEL_BYTES=54996327

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONHASHSEED=0 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    VIRTUAL_ENV=/opt/venv \
    PATH=/opt/venv/bin:$PATH

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl libgomp1 libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

RUN python -m venv "$VIRTUAL_ENV" \
    && python -m pip install --upgrade "pip==26.2" \
    && python -m pip install --index-url https://download.pytorch.org/whl/cpu "torch==${TORCH_VERSION}"

COPY requirements.txt /tmp/requirements.txt
RUN python -m pip install -r /tmp/requirements.txt \
    && python -m pip check \
    && python - <<'PY'
import importlib.metadata as metadata
import torch
assert metadata.version("demucs") == "4.1.0"
assert metadata.version("numpy") == "1.26.4"
assert metadata.version("torch") == "2.14.0+cpu"
assert torch.__version__ == "2.14.0+cpu"
assert not torch.cuda.is_available()
PY

RUN mkdir -p /opt/demucs/models /opt/rc20 \
    && curl --fail --location --retry 4 --output /opt/demucs/models/5c90dfd2-34c22ccb.th "${MODEL_URL}" \
    && echo "${MODEL_SHA256}  /opt/demucs/models/5c90dfd2-34c22ccb.th" | sha256sum -c - \
    && test "$(stat -c %s /opt/demucs/models/5c90dfd2-34c22ccb.th)" = "${MODEL_BYTES}" \
    && printf "models: ['5c90dfd2']\n" > /opt/demucs/models/htdemucs_6s.yaml \
    && python -m pip freeze --all | LC_ALL=C sort > /opt/rc20/python-resolved.txt

RUN python - <<'PY'
from pathlib import Path
from demucs.pretrained import get_model
model = get_model("htdemucs_6s", Path("/opt/demucs/models"))
assert tuple(model.sources) == ("drums", "bass", "other", "vocals", "guitar", "piano")
PY


FROM ${PYTHON_BASE} AS runtime

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONHASHSEED=0 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    VIRTUAL_ENV=/opt/venv \
    PATH=/opt/venv/bin:$PATH

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates ffmpeg libgomp1 libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /opt/venv /opt/venv
COPY --from=builder /opt/demucs /opt/demucs
COPY --from=builder /opt/rc20 /opt/rc20

WORKDIR /app
COPY audio_quality.py demucs_engine.py gbw_worker.py benchmark.py model_probe.py /app/
RUN useradd --uid 65532 --create-home --shell /usr/sbin/nologin worker \
    && chown -R worker:worker /app \
    && chmod -R a-w /opt/venv /opt/demucs/models /opt/rc20

USER 65532:65532
ENV HOME=/home/worker \
    GBW_MODEL_REPO=/opt/demucs/models \
    GBW_DEMUCS_DEVICE=cpu \
    GBW_DEMUCS_SHIFTS=1 \
    GBW_DEMUCS_OVERLAP=0.5 \
    GBW_DEMUCS_CPU_THREADS=8 \
    OMP_NUM_THREADS=8 \
    MKL_NUM_THREADS=8 \
    OPENBLAS_NUM_THREADS=8 \
    NUMEXPR_NUM_THREADS=8
ENTRYPOINT ["/opt/venv/bin/python3", "/app/gbw_worker.py"]
