# Real-Time Student and Faculty Face Recognition System Using OpenCV SFace

## Abstract

Face recognition systems are increasingly used in educational institutions for identity verification, attendance support, and controlled access. This paper presents a real-time face recognition system developed in Java using OpenCV. The system captures facial images through a webcam, evaluates image quality, generates deep facial embeddings using the OpenCV SFace ONNX model, and identifies registered users by comparing live embeddings with stored reference embeddings. The proposed system supports two institutional user categories: students and faculty. For students, the system displays name, USN, department, year, and college; for faculty, it displays name, department, and college. The implementation uses a lightweight OpenCV-based pipeline with Haar cascade face detection, SFace embedding extraction, cosine similarity matching, quality filtering, and multi-frame confirmation to reduce false identification. The system is designed for practical deployment in an institutional environment such as MITE College.

**Keywords:** Face recognition, OpenCV, SFace, Java, biometric identification, student verification, faculty verification, real-time recognition.

## 1. Introduction

Biometric recognition provides a convenient method for identifying individuals using unique physical characteristics. Among biometric techniques, face recognition is widely adopted because it is contactless, user-friendly, and suitable for real-time camera-based applications. Educational institutions can use face recognition to support student verification, faculty identification, attendance workflows, and laboratory access systems.

Traditional identity verification methods such as ID cards, manual registers, and passwords can be misplaced, shared, or forged. A camera-based face recognition system can reduce manual effort and improve verification speed. However, reliable recognition requires careful handling of image quality, lighting variation, face alignment, and similarity thresholds.

This project implements a real-time institutional face recognition system using Java and OpenCV. The system captures face images from a webcam, stores them as a local dataset, validates sample quality, and performs recognition using OpenCV SFace embeddings. The output format is customized for two user roles: student and faculty.

## 2. Objectives

The main objectives of the proposed system are:

1. To develop a real-time face recognition system using Java and OpenCV.
2. To recognize registered students and faculty members from live webcam input.
3. To maintain separate output formats for student and faculty users.
4. To improve recognition reliability using image quality analysis and repeated frame confirmation.
5. To build a simple local database for storing user details and dataset image mapping.

## 3. Existing System

Many manual identification systems depend on physical ID cards or human verification. These methods require additional time and can be affected by human error. Some simple face recognition systems use only classical image-processing methods, which may not perform reliably when lighting, pose, or facial expression changes.

Deep-learning-based face recognition models provide stronger feature representations by mapping faces into embedding vectors. Instead of comparing raw pixels, these systems compare compact numerical representations of faces. This improves robustness and enables practical real-time recognition.

## 4. Proposed System

The proposed system uses a webcam to capture live video frames and performs face recognition in real time. The system has five major modules:

1. Face image capture
2. Face quality analysis
3. Dataset loading and embedding generation
4. Live recognition and confidence checking
5. Student/faculty information display

The system stores registered face images in the `dataset/` folder. User details are stored in `database/persons.csv`. During recognition, each dataset image is converted into a face embedding. A live face crop is also converted into an embedding, and cosine similarity is used to find the best match.

## 5. System Architecture

The system follows this processing flow:

```text
Webcam Input
    |
Face Detection
    |
Face Crop and Quality Analysis
    |
SFace Embedding Extraction
    |
Cosine Similarity Matching
    |
Confidence and Margin Validation
    |
Multi-frame Confirmation
    |
Student/Faculty Details Output
```

### 5.1 Face Capture Module

The face capture module collects multiple face images for each registered user. The user enters a person ID, and the system stores images in the format:

```text
person1_1.jpg
person1_2.jpg
person1_3.jpg
```

Capturing 10 to 20 clear images per person is recommended. Multiple samples improve recognition because the system can compare the live face against several reference embeddings.

### 5.2 Face Quality Analysis Module

The system evaluates face quality before accepting dataset images. The quality score is calculated using:

1. Sharpness
2. Contrast
3. Brightness

The weighted formula used by the system is:

```text
Quality = 0.40(Sharpness) + 0.40(Contrast) + 0.20(Brightness)
```

Low-quality images are skipped during dataset loading. This helps reduce inaccurate recognition caused by blurry, dark, or overexposed images.

### 5.3 Feature Extraction Using SFace

The project uses OpenCV SFace ONNX embeddings for face recognition. SFace converts a face image into a numerical embedding vector. These embeddings are compared using cosine similarity. Two images of the same person should produce embeddings with higher similarity, while images of different people should produce lower similarity.

### 5.4 Recognition and Matching

For each registered person, the system compares the live embedding with stored embeddings. It calculates similarity scores and averages the top matches for each person. The person with the highest valid score is selected as the candidate identity.

The system applies the following validation checks:

1. Minimum similarity threshold
2. Minimum confidence value
3. Minimum score margin between best and second-best match
4. Repeated confirmation across multiple frames

The current implementation requires repeated matching frames before accepting a person as identified. This reduces false recognition from a single unstable frame.

### 5.5 Student and Faculty Output Module

The database stores the user type as either `student` or `faculty`.

For a student, the output format is:

```text
Name       : <Student Name>
USN        : <USN>
Department : Student of ISE
Year       : <Year>
College    : MITE College
```

For a faculty member, the output format is:

```text
Name       : <Faculty Name>
Department : Faculty of ISE
College    : MITE College
```

This makes the recognition output suitable for an institutional environment.

## 6. Implementation Details

The system is implemented using:

| Component | Technology |
|---|---|
| Programming language | Java |
| Computer vision library | OpenCV |
| Face recognition model | SFace ONNX |
| Face alignment model | YuNet ONNX |
| Webcam interface | OpenCV VideoCapture |
| Local database | CSV file |
| Dataset storage | Local image folder |

Important project files include:

| File | Purpose |
|---|---|
| `FaceCapture.java` | Captures face images through webcam |
| `FaceRecognizer.java` | Loads dataset and performs embedding matching |
| `FaceDetection.java` | Performs live webcam recognition |
| `FaceQualityAnalyzer.java` | Evaluates image sharpness, contrast, and brightness |
| `PersonDatabase.java` | Loads and displays student/faculty details |
| `persons.csv` | Stores user details and image mapping |

## 7. Algorithm

```text
Algorithm: Real-Time Face Recognition

Input: Live webcam frame
Output: Recognized user details or Unknown

1. Load registered users from persons.csv.
2. Load dataset images from dataset/.
3. For each dataset image:
   a. Detect and crop the largest face.
   b. Analyze face quality.
   c. Skip image if quality is below threshold.
   d. Generate SFace embedding.
   e. Store embedding under the person ID.
4. Start webcam video stream.
5. For each frame:
   a. Detect face region.
   b. Crop detected face.
   c. Generate live face embedding.
   d. Compare live embedding with stored embeddings.
   e. Calculate cosine similarity scores.
   f. Select best matching person.
   g. Validate threshold, confidence, and score margin.
   h. Confirm the same identity across repeated frames.
6. Display student or faculty details after successful confirmation.
```

## 8. Experimental Setup

The system can be evaluated using a webcam and a local dataset of registered users. Each user should have multiple reference images captured under normal lighting conditions.

Suggested evaluation conditions:

| Test Case | Description |
|---|---|
| Normal lighting | User faces camera with sufficient light |
| Low lighting | User appears in dim light |
| Slight pose variation | User turns face slightly left or right |
| Multiple users | Dataset contains more than one registered person |
| Unknown user | Unregistered person appears before camera |

## 9. Results and Discussion

The current system successfully performs real-time face recognition for registered users when clear face images are available. The use of quality filtering improves dataset reliability by rejecting weak samples. Multi-frame confirmation improves stability because the system does not immediately accept a match from a single frame.

Fill the following table after testing:

| No. of Users | Images per User | Correct Recognitions | False Recognitions | Unknown Rejections | Accuracy |
|---:|---:|---:|---:|---:|---:|
| 2 | 10 | TBD | TBD | TBD | TBD |
| 5 | 10 | TBD | TBD | TBD | TBD |
| 10 | 15 | TBD | TBD | TBD | TBD |

Accuracy can be calculated as:

```text
Accuracy = (Correct Recognitions / Total Test Attempts) * 100
```

## 10. Advantages

1. Contactless identification
2. Real-time webcam-based recognition
3. Separate student and faculty output
4. Local dataset and database storage
5. Quality filtering for better dataset reliability
6. Multi-frame confirmation to reduce unstable results
7. Simple Java and OpenCV implementation

## 11. Limitations

1. Recognition accuracy depends on dataset image quality.
2. Poor lighting may reduce recognition performance.
3. The system does not currently include liveness detection.
4. The database is stored as a CSV file, which is suitable for small deployments but not ideal for large systems.
5. Recognition may be affected by major changes in face angle, occlusion, or appearance.

## 12. Future Enhancements

1. Add liveness detection to prevent photo-based spoofing.
2. Integrate a relational database such as MySQL.
3. Add an attendance management module.
4. Add an admin dashboard for adding students and faculty.
5. Improve face detection using a deep-learning detector for all webcam frames.
6. Add automatic report generation for recognized users.
7. Encrypt stored user data for stronger privacy protection.

## 13. Conclusion

This paper presented a real-time student and faculty face recognition system using Java and OpenCV. The system uses SFace embeddings for recognition, cosine similarity for matching, face quality analysis for dataset filtering, and repeated frame confirmation for stable identification. The final output is customized for institutional use by displaying student and faculty details in separate formats and associating recognized users with MITE College. The system is lightweight, practical for small-scale deployment, and can be extended into a complete attendance or access control system.

## References

[1] OpenCV, "OpenCV Zoo: Face Recognition SFace Model." Available: https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface

[2] Y. Zhong, W. Deng, J. Hu, D. Zhao, X. Li, and D. Wen, "SFace: Sigmoid-Constrained Hypersphere Loss for Robust Face Recognition." Available: https://arxiv.org/abs/2205.12010

[3] OpenCV, "OpenCV Zoo: Face Detection YuNet Model." Available: https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet

[4] W. Wu, H. Peng, and S. Yu, "YuNet: A Tiny Millisecond-level Face Detector." Available: https://doi.org/10.1007/s11633-023-1423-y

[5] P. Viola and M. Jones, "Rapid Object Detection using a Boosted Cascade of Simple Features," Proceedings of the IEEE Conference on Computer Vision and Pattern Recognition, 2001.
