import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

public class FaceDetection {

    static {
        OpenCVLoader.load();
    }

    public static void main(String[] args) {

        // Load database first
        PersonDatabase.loadDatabase();

        // Load dataset for recognition
        FaceRecognizer.loadDataset();

        VideoCapture camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.out.println("Camera not found!");
            return;
        }

        CascadeClassifier detector = new CascadeClassifier(ProjectPaths.resolve(
                "model/haarcascade_frontalface_default.xml").getAbsolutePath());

        if (detector.empty()) {
            System.out.println("Cannot load face detector.");
            return;
        }

        Mat frame = new Mat();
        Mat gray = new Mat();

        String lastDetected = "unknown";
        int confirmCount = 0;
        int emptyFrameCount = 0;

        final int REQUIRED_CONFIRMATIONS = 10;
        final int MAX_EMPTY_FRAMES = 60;

        System.out.println("Scanning for face...");

        while (true) {

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

            MatOfRect faces = new MatOfRect();

            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            detector.detectMultiScale(
                    gray,
                    faces,
                    1.15,
                    6,
                    0,
                    new Size(100, 100),
                    new Size());

            Rect[] faceArray = faces.toArray();
            Rect bestFace = FaceRecognizer.largestFace(faceArray);
            boolean faceFound = bestFace != null;

            if (faceArray.length > 1) {
                Imgproc.putText(
                        frame,
                        "Multiple faces detected - using largest face",
                        new Point(20, 40),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        new Scalar(0, 255, 255),
                        2);
            }

            if (faceFound) {
                Rect rect = FaceRecognizer.clampRect(
                        FaceRecognizer.addPadding(bestFace, 0.18),
                        frame.cols(), frame.rows());

                Mat faceROI = new Mat(frame, rect);

                // Use the new confidence-based recognition
                RecognitionResult result = FaceRecognizer.recognizeFaceWithConfidence(faceROI);

                Scalar color;
                String displayName;

                if (result.isConfident) {

                    displayName = PersonDatabase.getPersonName(result.imageFile);

                    color = new Scalar(0, 255, 0);

                    if (result.imageFile.equals(lastDetected)) {
                        confirmCount++;
                    } else {
                        lastDetected = result.imageFile;
                        confirmCount = 1;
                    }

                    // Scanning progress with confidence
                    String scanText = String.format("Scanning... %d/%d (confidence: %.0f%%)",
                            confirmCount, REQUIRED_CONFIRMATIONS, result.confidence * 100);
                    Imgproc.putText(
                            frame,
                            scanText,
                            new Point(20, 40),
                            Imgproc.FONT_HERSHEY_SIMPLEX,
                            0.8,
                            color,
                            2);

                    // If fully confirmed
                    if (confirmCount >= REQUIRED_CONFIRMATIONS) {

                        String identifiedText = String.format("IDENTIFIED: %s (%.1f%%)",
                                displayName, result.confidence * 100);
                        Imgproc.putText(
                                frame,
                                identifiedText,
                                new Point(20, 80),
                                Imgproc.FONT_HERSHEY_SIMPLEX,
                                0.8,
                                color,
                                2);

                        HighGui.imshow("Face Recognition", frame);

                        HighGui.waitKey(2000);

                        camera.release();
                        HighGui.destroyAllWindows();

                        PersonDatabase.showPersonDetails(result.imageFile);

                        return;
                    }

                } else {

                    displayName = "Unknown";
                    color = new Scalar(0, 0, 255);

                    lastDetected = "unknown";
                    confirmCount = 0;
                }

                // Draw face box
                Imgproc.rectangle(
                        frame,
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width,
                                rect.y + rect.height),
                        color,
                        2);

                // Draw name
                Imgproc.putText(
                        frame,
                        displayName,
                        new Point(rect.x, rect.y - 10),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        color,
                        2);
            }

            if (!faceFound) {
                confirmCount = 0;
                lastDetected = "unknown";
            }

            HighGui.imshow("Face Recognition", frame);

            // ESC to exit
            if (HighGui.waitKey(30) == 27) {
                break;
            }
        }

        camera.release();
        HighGui.destroyAllWindows();
    }
}
