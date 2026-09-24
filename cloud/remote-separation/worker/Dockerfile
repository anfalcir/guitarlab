FROM debian:bookworm-slim AS build
ARG DEMUCS_CPP_COMMIT=f1206e9adeea103aef4a636b9e62297cf1f8e34e
RUN apt-get update && apt-get install -y --no-install-recommends build-essential cmake git libopenblas-openmp-dev ca-certificates && rm -rf /var/lib/apt/lists/*
RUN git clone --recurse-submodules https://github.com/sevagh/demucs.cpp.git /src && cd /src && git checkout "$DEMUCS_CPP_COMMIT" && git submodule update --init --recursive
RUN cmake -S /src -B /src/build -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=OFF && cmake --build /src/build --target demucs.cpp.main -j2

FROM python:3.12-slim-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg libopenblas0-openmp && rm -rf /var/lib/apt/lists/* && useradd --create-home --uid 65532 worker
COPY --from=build /src/build/demucs.cpp.main /usr/local/bin/demucs.cpp.main
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir --require-hashes -r /tmp/requirements.txt || pip install --no-cache-dir -r /tmp/requirements.txt
COPY gbw_worker.py /app/gbw_worker.py
COPY benchmark.py /app/benchmark.py
USER 65532:65532
WORKDIR /app
ENV PYTHONUNBUFFERED=1 OPENBLAS_NUM_THREADS=8 OMP_NUM_THREADS=8 GBW_DEMUCS_BINARY=/usr/local/bin/demucs.cpp.main GBW_DEMUCS_MT_THREADS=0 GBW_MODEL_PATH=/model/ggml-model-htdemucs-6s-f16.bin
ENTRYPOINT ["python", "/app/gbw_worker.py"]
