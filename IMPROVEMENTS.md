# Current Recognition Setup

The project now uses a simpler OpenCV-only pipeline:

- **SFace ONNX** for face embeddings: `model/face_recognition_sface_2021dec.onnx`
- **YuNet ONNX** for landmark-based alignment: `model/face_detection_yunet_2023mar.onnx`
- **Haar cascade** for the existing webcam face box detection

Removed complexity:

- TensorFlow Lite Java/AAR dependency
- Incompatible `facenet.tflite`
- `TFLiteEmbedder.java`
- OpenFace fallback model/code
- Old handmade local-feature embedding fallback
- Stale FaceNet setup/checklist/reference documents

## Backend Priority

1. OpenCV SFace ONNX
2. No recognition model available

## Useful Commands

```bat
build.bat
run-trainer.bat
run-app.bat
```

`run-trainer.bat` checks dataset image quality. `run-app.bat` starts live recognition.
