import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class WebcamTest {

    static {
        OpenCVLoader.load();
    }

    public static void main(String[] args) {

        System.out.println("Testing webcam...");

        VideoCapture camera = new VideoCapture(0);

        if (!camera.isOpened()) {
            System.err.println("FAILED: Camera not detected at index 0.");
            System.err.println("Try changing VideoCapture(0) to VideoCapture(1).");
            return;
        }

        double width  = camera.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double height = camera.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double fps    = camera.get(Videoio.CAP_PROP_FPS);
        System.out.printf("Camera opened: %.0fx%.0f @ %.1f FPS%n", width, height, fps);

        Mat frame = new Mat();
        camera.read(frame);

        if (!frame.empty()) {
            System.out.println("SUCCESS: Frame captured ("
                    + frame.cols() + "x" + frame.rows() + " px).");
        } else {
            System.err.println("WARNING: Camera opened but no frame captured.");
        }

        camera.release();
        System.out.println("Webcam test complete.");
    }
}