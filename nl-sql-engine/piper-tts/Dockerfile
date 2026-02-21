FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    && pip install --no-cache-dir piper-tts flask \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Download French voice model on build
RUN mkdir -p models && \
    wget -q -O models/fr_FR-siwis-medium.onnx "https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium.onnx" && \
    wget -q -O models/fr_FR-siwis-medium.onnx.json "https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium.onnx.json"

COPY server.py .

EXPOSE 5000
CMD ["python", "server.py"]
