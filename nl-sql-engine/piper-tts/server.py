import io
import wave
from flask import Flask, request, Response
from piper import PiperVoice

app = Flask(__name__)
voice = PiperVoice.load("models/fr_FR-siwis-medium.onnx")


@app.route("/synthesize", methods=["POST"])
def synthesize():
    data = request.get_json()
    text = data.get("text", "")
    if not text:
        return {"error": "missing text"}, 400

    buf = io.BytesIO()
    with wave.open(buf, "wb") as wav:
        voice.synthesize_wav(text, wav, set_wav_format=True)
    buf.seek(0)
    return Response(buf.read(), mimetype="audio/wav")


@app.route("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
