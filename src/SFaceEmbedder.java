import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;

/**
 * SFaceEmbedder - OpenCV Zoo SFace ONNX embeddings.
 *
 * Uses OpenCV's FaceDetectorYN for landmark alignment when possible, then
 * FaceRecognizerSF for feature extraction.
 */
public class SFaceEmbedder {

    private static final String DETECTOR_MODEL = "model/face_detection_yunet_2023mar.onnx";
    private static final String RECOGNIZER_MODEL = "model/face_recognition_sface_2021dec.onnx";
    private static final int FACE_SIZE = 112;
    private static final int EMBEDDING_SIZE = 128;

    private static FaceDetectorYN detector = null;
    private static FaceRecognizerSF recognizer = null;
    private static boolean initialized = false;
    private static boolean available = false;

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        File detectorFile = ProjectPaths.resolve(DETECTOR_MODEL);
        File recognizerFile = ProjectPaths.resolve(RECOGNIZER_MODEL);

        if (!detectorFile.exists() || !recognizerFile.exists()) {
            System.out.println("SFace/YuNet ONNX models not found. Skipping SFace backend.");
            return;
        }

        try {
            detector = FaceDetectorYN.create(detectorFile.getAbsolutePath(), "",
                    new Size(320, 320), 0.85f, 0.3f, 5000);
            recognizer = FaceRecognizerSF.create(recognizerFile.getAbsolutePath(), "");

            available = validate();
            if (available) {
                System.out.println("SFace ONNX model loaded and validated.");
            } else {
                System.out.println("SFace ONNX model could not be validated. Falling back to other backends.");
                detector = null;
                recognizer = null;
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Error initializing SFace ONNX backend: " + e.getMessage());
            detector = null;
            recognizer = null;
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static double[] createEmbedding(Mat face) {
        if (!available || recognizer == null || face == null || face.empty()) {
            return new double[0];
        }

        try {
            Mat aligned = alignFace(face);
            Mat feature = new Mat();
            recognizer.feature(aligned, feature);
            return matToEmbedding(feature);
        } catch (Exception e) {
            System.out.println("Error creating SFace embedding: " + e.getMessage());
            return new double[0];
        }
    }

    private static boolean validate() {
        try {
            Mat sample = Mat.zeros(FACE_SIZE, FACE_SIZE, CvType.CV_8UC3);
            Mat feature = new Mat();
            recognizer.feature(sample, feature);
            return feature != null && !feature.empty() && feature.total() >= EMBEDDING_SIZE;
        } catch (Exception e) {
            return false;
        }
    }

    private static Mat alignFace(Mat face) {
        Mat normalized = ensureColor(face);

        if (detector != null) {
            try {
                detector.setInputSize(new Size(normalized.cols(), normalized.rows()));
                Mat faces = new Mat();
                detector.detect(normalized, faces);

                if (faces.rows() > 0) {
                    Mat bestFace = faces.row(bestFaceRow(faces));
                    Mat aligned = new Mat();
                    recognizer.alignCrop(normalized, bestFace, aligned);
                    if (!aligned.empty()) {
                        return aligned;
                    }
                }
            } catch (Exception e) {
                // A Haar crop may be too tight for YuNet landmarks; use direct crop below.
            }
        }

        Mat resized = new Mat();
        Imgproc.resize(normalized, resized, new Size(FACE_SIZE, FACE_SIZE));
        return resized;
    }

    private static int bestFaceRow(Mat faces) {
        int best = 0;
        double bestScore = -1.0;
        for (int i = 0; i < faces.rows(); i++) {
            double score = faces.cols() > 14 ? faces.get(i, 14)[0] : 0.0;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private static Mat ensureColor(Mat image) {
        if (image.channels() == 3) {
            return image;
        }

        Mat color = new Mat();
        Imgproc.cvtColor(image, color, Imgproc.COLOR_GRAY2BGR);
        return color;
    }

    private static double[] matToEmbedding(Mat feature) {
        Mat flat = feature.reshape(1, 1);
        int length = (int) flat.total();
        double[] embedding = new double[length];

        for (int i = 0; i < length; i++) {
            embedding[i] = flat.get(0, i)[0];
        }

        return l2Normalize(embedding);
    }

    private static double[] l2Normalize(double[] values) {
        double sumSquares = 0.0;
        for (double value : values) {
            sumSquares += value * value;
        }

        if (sumSquares == 0.0) {
            return values;
        }

        double length = Math.sqrt(sumSquares);
        for (int i = 0; i < values.length; i++) {
            values[i] /= length;
        }
        return values;
    }
}
