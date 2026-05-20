# Face Recognition System

Java/OpenCV webcam face recognition using OpenCV SFace embeddings.

## Project Structure

- `src/` - Java source files
- `lib/opencv-4120.jar` - OpenCV Java dependency
- `native/opencv_java4120.dll` - OpenCV native library
- `model/face_recognition_sface_2021dec.onnx` - SFace recognition model
- `model/face_detection_yunet_2023mar.onnx` - YuNet alignment model
- `model/haarcascade_frontalface_default.xml` - webcam face box detector
- `dataset/` - captured face images
- `database/persons.csv` - person details

## Commands

```bat
run-webcam-test.bat
run-capture.bat
run-trainer.bat
run-app.bat
```

Recommended flow:

1. Run `run-webcam-test.bat`.
2. Run `run-capture.bat` and capture 10-20 clear photos per person.
3. Update `database/persons.csv`.
4. Run `run-trainer.bat` to check dataset quality.
5. Run `run-app.bat` to start recognition.

## Notes

- The project intentionally uses one recognition backend: OpenCV SFace ONNX.
- TensorFlow Lite, FaceNet, OpenFace fallback, and handmade local-feature matching were removed to keep the code simple.
- Live recognition waits for repeated matching frames before accepting a person.
