public class OpenCVLoader {

    private static boolean loaded = false;

    public static void load() {

        if (loaded) {
            return;
        }

        try {

            String dllPath = ProjectPaths.resolve(
                    "native\\opencv_java4120.dll").getAbsolutePath();

            System.load(dllPath);

            loaded = true;

            System.out.println("OpenCV loaded successfully.");

        } catch (UnsatisfiedLinkError | SecurityException e) {

            System.out.println("Error loading OpenCV.");
            e.printStackTrace();
        }
    }
}
