import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FaceRecognizer - embedding based face recognition.
 *
 * Uses OpenCV SFace ONNX embeddings.
 */
public class FaceRecognizer {

    private static final int TOP_MATCHES_PER_PERSON = 3;

    private static final double SFACE_THRESHOLD = 0.363;
    private static final double SINGLE_PERSON_THRESHOLD_BONUS = 0.03;
    private static final double MIN_SCORE_MARGIN = 0.10;
    private static final double MIN_SAMPLE_QUALITY = 40.0;
    private static final double MIN_EMBEDDING_CONFIDENCE = 0.60;

    private static Map<String, List<FaceSample>> datasetCache = null;
    private static CascadeClassifier faceDetector = null;
    private static boolean usingSFace = false;
    private static int datasetPersonCount = 0;

    /**
     * Load all dataset images into memory and convert each face to an embedding.
     */
    public static void loadDataset() {

        datasetCache = new HashMap<>();

        SFaceEmbedder.init();
        usingSFace = SFaceEmbedder.isAvailable();

        File folder = ProjectPaths.resolve("dataset");
        File[] files = folder.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".jpg") ||
                        name.toLowerCase().endsWith(".jpeg") ||
                        name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            System.err.println("WARNING: No images found in dataset/ folder.");
            return;
        }

        for (File file : files) {

            Mat img = Imgcodecs.imread(file.getAbsolutePath());

            if (img.empty()) {
                System.out.println("SKIP unreadable image: " + file.getName());
                continue;
            }

            Mat faceOnly = cropLargestFace(img);
            if (faceOnly.empty()) {
                faceOnly = img;
            }

            double quality = FaceQualityAnalyzer.analyzeQuality(faceOnly);
            if (quality < MIN_SAMPLE_QUALITY) {
                System.out.printf("SKIP low quality image: %s (quality %.1f)%n",
                        file.getName(), quality);
                continue;
            }

            double[] embedding = createEmbedding(faceOnly);
            if (embedding.length == 0) {
                System.out.println("SKIP could not build embedding: " + file.getName());
                continue;
            }

            String filename = file.getName();
            String personId = filename.contains("_")
                    ? filename.split("_")[0]
                    : filename.replaceAll("\\.[^.]+$", "");

            datasetCache.computeIfAbsent(personId, k -> new ArrayList<>())
                    .add(new FaceSample(filename, embedding));
            System.out.printf("Loaded: %s -> %s (quality %.1f)%n",
                    filename, personId, quality);
        }

        int imageCount = datasetCache.values().stream().mapToInt(List::size).sum();
        datasetPersonCount = datasetCache.size();
        System.out.println("Dataset ready: " + datasetCache.size()
                + " person(s), " + imageCount + " image(s) total.");

        System.out.println("Recognition mode: OpenCV SFace ONNX embeddings");

        if (!usingSFace) {
            System.out.println("No recognition model is available. Add the SFace ONNX models to model/.");
        }

        for (Map.Entry<String, List<FaceSample>> entry : datasetCache.entrySet()) {
            if (entry.getValue().size() < 5) {
                System.out.println("Tip: " + entry.getKey()
                        + " has only " + entry.getValue().size()
                        + " image(s). Capture 10-20 varied photos for better accuracy.");
            }
        }
    }

    /**
     * Recognize a face from a BGR Mat crop.
     * Returns a RecognitionResult with confidence score.
     */
    public static RecognitionResult recognizeFaceWithConfidence(Mat faceFrame) {

        if (faceFrame == null || faceFrame.empty()) {
            return RecognitionResult.unknown();
        }

        if (datasetCache == null) {
            loadDataset();
        }

        if (datasetCache.isEmpty()) {
            return RecognitionResult.unknown();
        }

        double[] liveEmbedding = createEmbedding(faceFrame);
        if (liveEmbedding.length == 0) {
            return RecognitionResult.unknown();
        }

        String bestPerson = "unknown";
        String bestImageFile = "unknown";
        double bestScore = -1.0;
        double secondBestScore = -1.0;

        for (Map.Entry<String, List<FaceSample>> entry : datasetCache.entrySet()) {

            String personId = entry.getKey();
            List<FaceSample> storedEmbeddings = entry.getValue();
            List<Double> imageScores = new ArrayList<>();
            String personBestFile = storedEmbeddings.get(0).imageFile;
            double personBestScore = -1.0;

            for (FaceSample stored : storedEmbeddings) {
                double score = cosineSimilarity(liveEmbedding, stored.embedding);
                imageScores.add(score);
                if (score > personBestScore) {
                    personBestScore = score;
                    personBestFile = stored.imageFile;
                }
            }

            double personScore = topAverage(imageScores, TOP_MATCHES_PER_PERSON);

            System.out.printf("  vs %-12s -> %.4f (%d reference image%s)%n",
                    personId,
                    personScore,
                    storedEmbeddings.size(),
                    storedEmbeddings.size() == 1 ? "" : "s");

            if (personScore > bestScore) {
                secondBestScore = bestScore;
                bestScore = personScore;
                bestPerson = personId;
                bestImageFile = personBestFile;
            } else if (personScore > secondBestScore) {
                secondBestScore = personScore;
            }
        }

        double threshold = getAdaptiveThreshold();
        double confidence = calculateConfidence(bestScore, secondBestScore, threshold);

        System.out.printf("Best: %s (%.4f) confidence: %.1f%% threshold: %.2f%n",
                bestPerson, bestScore, confidence * 100, threshold);

        if (bestScore < threshold) {
            System.out.println("Rejected: Score below threshold");
            return RecognitionResult.unknown();
        }

        if (confidence < MIN_EMBEDDING_CONFIDENCE) {
            System.out.printf("Rejected: Confidence below minimum (%.1f%% < %.1f%%)%n",
                    confidence * 100, MIN_EMBEDDING_CONFIDENCE * 100);
            return RecognitionResult.unknown();
        }

        if (datasetPersonCount > 1
                && secondBestScore >= 0
                && bestScore - secondBestScore < MIN_SCORE_MARGIN) {
            System.out.printf("Rejected: Match too close (best %.4f vs second %.4f)%n",
                    bestScore, secondBestScore);
            return RecognitionResult.unknown();
        }

        String details = String.format("Score: %.4f vs %.4f", bestScore, secondBestScore);
        return new RecognitionResult(bestPerson, bestImageFile, bestScore,
                confidence, true, details);
    }

    /**
     * Calculate confidence score (0.0-1.0) based on similarity and thresholds.
     */
    private static double calculateConfidence(double bestScore, double secondBestScore,
            double threshold) {
        if (bestScore < threshold) {
            return 0.0;
        }

        double scoreConfidence = MIN_EMBEDDING_CONFIDENCE
                + ((bestScore - threshold) / (1.0 - threshold))
                        * (1.0 - MIN_EMBEDDING_CONFIDENCE);
        scoreConfidence = Math.min(1.0, scoreConfidence);

        double marginPenalty = 1.0;
        if (secondBestScore >= 0) {
            double margin = bestScore - secondBestScore;
            if (margin < MIN_SCORE_MARGIN) {
                marginPenalty = 0.3;
            } else {
                marginPenalty = Math.min(1.0, margin / MIN_SCORE_MARGIN);
            }
        }

        return scoreConfidence * marginPenalty;
    }

    /**
     * Get adaptive threshold based on dataset characteristics.
     */
    private static double getAdaptiveThreshold() {
        if (usingSFace) {
            double adjusted = SFACE_THRESHOLD + (datasetPersonCount > 10 ? 0.02 : 0.0);
            return datasetPersonCount <= 1 ? adjusted - SINGLE_PERSON_THRESHOLD_BONUS : adjusted;
        }
        return 1.0;
    }

    /**
     * Recognize a face from a BGR Mat crop (legacy method).
     * Returns a database image key like "person1_1.jpg", or "unknown".
     */
    public static String recognizeFace(Mat faceFrame) {
        RecognitionResult result = recognizeFaceWithConfidence(faceFrame);
        return result.isConfident ? result.imageFile : "unknown";
    }

    private static double[] createEmbedding(Mat face) {
        if (usingSFace) {
            return SFaceEmbedder.createEmbedding(face);
        }
        return new double[0];
    }

    public static double sampleQuality(Mat image) {
        return FaceQualityAnalyzer.analyzeQuality(image);
    }

    private static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length || a.length == 0) {
            return -1.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static double topAverage(List<Double> scores, int maxCount) {
        if (scores.isEmpty()) {
            return 0;
        }

        scores.sort(Collections.reverseOrder());
        int count = Math.min(maxCount, scores.size());
        double total = 0;
        for (int i = 0; i < count; i++) {
            total += scores.get(i);
        }
        return total / count;
    }

    private static Mat cropLargestFace(Mat image) {
        CascadeClassifier detector = getFaceDetector();
        if (detector == null || detector.empty()) {
            return new Mat();
        }

        Mat gray = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            image.copyTo(gray);
        }

        MatOfRect faces = new MatOfRect();
        Imgproc.equalizeHist(gray, gray);

        detector.detectMultiScale(gray, faces, 1.15, 5, 0,
                new Size(60, 60), new Size());

        Rect largest = largestFace(faces.toArray());
        if (largest == null) {
            return new Mat();
        }

        Rect padded = clampRect(addPadding(largest, 0.22), image.cols(), image.rows());
        return new Mat(image, padded);
    }

    public static Rect largestFace(Rect[] faces) {
        Rect largest = null;
        for (Rect face : faces) {
            if (largest == null || face.area() > largest.area()) {
                largest = face;
            }
        }
        return largest;
    }

    private static CascadeClassifier getFaceDetector() {
        if (faceDetector == null) {
            faceDetector = new CascadeClassifier(ProjectPaths.resolve(
                    "model/haarcascade_frontalface_default.xml").getAbsolutePath());
        }
        return faceDetector;
    }

    public static Rect addPadding(Rect rect, double percent) {
        int paddingX = (int) Math.round(rect.width * percent);
        int paddingY = (int) Math.round(rect.height * percent);
        return new Rect(
                rect.x - paddingX,
                rect.y - paddingY,
                rect.width + paddingX * 2,
                rect.height + paddingY * 2);
    }

    public static Rect clampRect(Rect rect, int frameWidth, int frameHeight) {
        int x = Math.max(rect.x, 0);
        int y = Math.max(rect.y, 0);
        int w = Math.min(rect.width, frameWidth - x);
        int h = Math.min(rect.height, frameHeight - y);
        return new Rect(x, y, w, h);
    }

    private static class FaceSample {
        final String imageFile;
        final double[] embedding;

        FaceSample(String imageFile, double[] embedding) {
            this.imageFile = imageFile;
            this.embedding = embedding;
        }
    }
}
