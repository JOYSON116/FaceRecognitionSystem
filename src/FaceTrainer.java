import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;

public class FaceTrainer {

    static {
        OpenCVLoader.load();
    }

    public static void main(String[] args) {

        File folder = ProjectPaths.resolve("dataset");

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("ERROR: 'dataset' folder not found.");
            System.err.println("Create it and add images named: person1_1.jpg, person2_1.jpg ...");
            return;
        }

        File[] files = folder.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".jpg") ||
                        name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            System.err.println("ERROR: No images found in dataset/ folder.");
            return;
        }

        System.out.println("Scanning dataset...");
        System.out.println("-------------------------------------");

        int valid = 0;
        int invalid = 0;
        CascadeClassifier faceDetector = new CascadeClassifier(ProjectPaths.resolve(
                "model/haarcascade_frontalface_default.xml").getAbsolutePath());

        for (File file : files) {

            Mat img = Imgcodecs.imread(file.getAbsolutePath());

            if (img.empty()) {
                System.out.println("SKIP (unreadable) : " + file.getName());
                invalid++;
                continue;
            }

            Mat gray = new Mat();
            Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faces = new MatOfRect();
            if (!faceDetector.empty()) {
                faceDetector.detectMultiScale(gray, faces, 1.15, 5, 0,
                        new Size(60, 60), new Size());
            }

            Rect[] faceArray = faces.toArray();
            Rect bestFace = FaceRecognizer.largestFace(faceArray);
            Mat face = bestFace == null
                    ? img
                    : new Mat(img, FaceRecognizer.clampRect(
                            FaceRecognizer.addPadding(bestFace, 0.22),
                            img.cols(), img.rows()));

            double quality = FaceQualityAnalyzer.analyzeQuality(face);
            if (bestFace == null || quality < 40.0) {
                System.out.printf("WARN %-25s  face=%s quality=%.1f%n",
                        file.getName(),
                        bestFace == null ? "not found" : "found",
                        quality);
                invalid++;
                continue;
            }

            System.out.printf("OK   %-25s  faces=%d quality=%.1f%n",
                    file.getName(), faceArray.length, quality);
            valid++;
        }

        System.out.println("-------------------------------------");
        System.out.println("Valid   : " + valid);
        System.out.println("Skipped : " + invalid);

        if (valid > 0) {
            System.out.println("\nDataset looks good! Now run MainApp.");
        } else {
            System.err.println("\nNo valid images found. Check dataset folder.");
        }
    }
}
