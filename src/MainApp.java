public class MainApp {

    static {
        OpenCVLoader.load();
    }

    public static void main(String[] args) {
        System.out.println("=== Face Recognition System ===");

        // Start the detection loop
        FaceDetection.main(new String[]{});
    }
}
