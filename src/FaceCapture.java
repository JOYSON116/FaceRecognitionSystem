import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.Scanner;

/**
 * FaceCapture - captures face photos from webcam and saves them to dataset/.
 *
 * Usage:
 * 1. Run this class
 * 2. Enter the person ID (e.g. 1 for person1)
 * 3. Enter how many photos to capture
 * 4. Look at the camera - press SPACE to capture each photo
 * 5. Press ESC to quit early
 *
 * Output files saved as:
 * dataset/person1_1.jpg
 * dataset/person1_2.jpg
 * ...
 */
public class FaceCapture {

    static {
        OpenCVLoader.load();
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Face Capture Tool ===");
        System.out.print("Enter person ID (e.g. 1 for person1): ");
        String personId = "person" + scanner.nextLine().trim();

        System.out.print("How many photos to capture? (recommended: 15): ");
        int totalPhotos;
        try {
            totalPhotos = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            totalPhotos = 15;
        }

        System.out.println("\nInstructions:");
        System.out.println("  - Look straight at the camera");
        System.out.println("  - Make sure your face is well lit");
        System.out.println("  - Press SPACE to capture a photo");
        System.out.println("  - Press ESC to quit");
        System.out.println("\nStarting camera...\n");

        // Make sure dataset folder exists
        File datasetFolder = ProjectPaths.resolve("dataset");
        datasetFolder.mkdirs();

        VideoCapture camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.err.println("ERROR: Camera not detected.");
            return;
        }

        CascadeClassifier faceDetector = new CascadeClassifier(ProjectPaths.resolve(
                "model/haarcascade_frontalface_default.xml").getAbsolutePath());

        if (faceDetector.empty()) {
            System.err.println("ERROR: Could not load haarcascade_frontalface_default.xml");
            camera.release();
            return;
        }

        Mat frame = new Mat();
        Mat display = new Mat();
        Mat gray = new Mat();
        int captured = 0;
        int emptyFrameCount = 0;
        final int MAX_EMPTY_FRAMES = 60;
        final double MIN_CAPTURE_QUALITY = 45.0;

        while (captured < totalPhotos) {

            camera.read(frame);
            if (frame.empty()) {
                emptyFrameCount++;
                if (emptyFrameCount >= MAX_EMPTY_FRAMES) {
                    System.out.println("Camera stopped sending frames. Close other camera apps and try again.");
                    break;
                }
                HighGui.waitKey(30);
                continue;
            }

            emptyFrameCount = 0;

            frame.copyTo(display);

            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(
                    gray, faces, 1.15, 6, 0,
                    new Size(100, 100), new Size());

            Rect[] faceArray = faces.toArray();
            Rect bestFace = FaceRecognizer.largestFace(faceArray);
            boolean faceFound = bestFace != null;
            double quality = 0.0;

            if (faceFound) {
                Rect qualityRect = FaceRecognizer.clampRect(
                        FaceRecognizer.addPadding(bestFace, 0.18),
                        frame.cols(), frame.rows());
                quality = FaceQualityAnalyzer.analyzeQuality(new Mat(frame, qualityRect));
            }

            for (Rect rect : faceArray) {

                // Green box when face detected
                Imgproc.rectangle(
                        display,
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0), 2);

                // Draw center crosshair guide
                int cx = rect.x + rect.width / 2;
                int cy = rect.y + rect.height / 2;
                Imgproc.line(display,
                        new Point(cx - 15, cy), new Point(cx + 15, cy),
                        new Scalar(0, 255, 255), 1);
                Imgproc.line(display,
                        new Point(cx, cy - 15), new Point(cx, cy + 15),
                        new Scalar(0, 255, 255), 1);
            }

            // Status text at top
            boolean qualityOk = faceFound && quality >= MIN_CAPTURE_QUALITY;

            String statusText = qualityOk
                    ? "Face ready - press SPACE (" + captured + "/" + totalPhotos + ")"
                    : faceFound
                            ? "Improve quality (" + String.format("%.0f", quality) + "/45) - better lighting"
                            : "No face detected - position your face in frame";

            Scalar statusColor = qualityOk
                    ? new Scalar(0, 255, 0)
                    : faceFound
                            ? new Scalar(0, 165, 255) // Orange for marginal
                            : new Scalar(0, 0, 255);

            // Dark background behind text for readability
            Imgproc.rectangle(display,
                    new Point(0, 0),
                    new Point(display.cols(), 50),
                    new Scalar(0, 0, 0), -1);

            Imgproc.putText(display, statusText,
                    new Point(10, 28),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.65,
                    statusColor, 2);

            // Quality bar
            int barWidth = Math.min(300, display.cols() - 20);
            int barHeight = 8;
            int barX = 10;
            int barY = 38;
            int fillWidth = (int) (barWidth * Math.min(1.0, quality / 100.0));

            Imgproc.rectangle(display,
                    new Point(barX, barY),
                    new Point(barX + barWidth, barY + barHeight),
                    new Scalar(100, 100, 100), 1);
            Imgproc.rectangle(display,
                    new Point(barX, barY),
                    new Point(barX + fillWidth, barY + barHeight),
                    statusColor, -1);

            // Person ID label at bottom
            Imgproc.rectangle(display,
                    new Point(0, display.rows() - 35),
                    new Point(display.cols(), display.rows()),
                    new Scalar(0, 0, 0), -1);

            Imgproc.putText(display,
                    "Capturing for: " + personId,
                    new Point(10, display.rows() - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.65,
                    new Scalar(255, 255, 255), 2);

            HighGui.imshow("Face Capture - " + personId, display);

            int key = HighGui.waitKey(30);

            // ESC = quit
            if (key == 27) {
                System.out.println("Cancelled. Captured " + captured + " photo(s).");
                break;
            }

            // SPACE = capture
            if (key == 32 && qualityOk) {

                // Crop just the face region with small padding
                bestFace = clampRect(
                        FaceRecognizer.addPadding(bestFace, 0.22),
                        frame.cols(), frame.rows());

                Mat faceCrop = new Mat(frame, bestFace);

                // Resize to standard size before saving
                Imgproc.resize(faceCrop, faceCrop, new Size(200, 200));

                captured++;
                File outputFile = new File(datasetFolder, personId + "_" + captured + ".jpg");
                String filename = outputFile.getPath();
                Imgcodecs.imwrite(filename, faceCrop);

                System.out.println("Saved: " + filename);

                // Flash effect - brief white overlay to confirm capture
                Mat flash = new Mat(display.size(), display.type(), new Scalar(255, 255, 255));
                HighGui.imshow("Face Capture - " + personId, flash);
                HighGui.waitKey(120);

                if (captured >= totalPhotos) {
                    System.out.println("\nAll " + totalPhotos + " photos captured!");
                    System.out.println("Now update database/persons.csv with this person's details.");
                    System.out.println("Then recompile and run MainApp.");
                }
            }

            // SPACE pressed but no face
            if (key == 32 && !faceFound) {
                System.out.println("No face in frame - could not capture.");
            } else if (key == 32 && faceFound && !qualityOk) {
                System.out.printf("Face is too blurry or poorly lit (quality %.1f). Try again.%n", quality);
            }
        }

        camera.release();
        HighGui.destroyAllWindows();
        System.out.println("\nCapture session ended.");
    }

    // Clamp rect to stay within frame
    private static Rect clampRect(Rect rect, int frameWidth, int frameHeight) {
        int x = Math.max(rect.x, 0);
        int y = Math.max(rect.y, 0);
        int w = Math.min(rect.width, frameWidth - x);
        int h = Math.min(rect.height, frameHeight - y);
        return new Rect(x, y, w, h);
    }
}
