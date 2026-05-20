import java.io.File;

public class ProjectPaths {

    private ProjectPaths() {
    }

    public static File resolve(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }

        File fromParent = new File("..", relativePath);
        if (fromParent.exists()) {
            return fromParent;
        }

        return direct;
    }
}
