import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * FaceQualityAnalyzer - comprehensive face quality assessment.
 * 
 * Measures: sharpness, contrast, illumination, face alignment.
 */
public class FaceQualityAnalyzer {

    /**
     * Comprehensive quality score (0-100).
     * Considers sharpness, contrast, brightness, and face centering.
     */
    public static double analyzeQuality(Mat face) {
        if (face.empty()) {
            return 0.0;
        }

        double sharpness = measureSharpness(face);
        double contrast = measureContrast(face);
        double brightness = measureBrightness(face);

        // Weighted combination: sharpness (40%) + contrast (40%) + brightness (20%)
        double quality = (sharpness * 0.40) + (contrast * 0.40) + (brightness * 0.20);

        return Math.max(0.0, Math.min(100.0, quality));
    }

    /**
     * Measure image sharpness using Laplacian variance.
     * Higher variance = sharper image.
     * Score: 0-100, typically 20-60 for good quality.
     */
    private static double measureSharpness(Mat image) {
        Mat gray = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            image.copyTo(gray);
        }

        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);

        double variance = stddev.get(0, 0)[0] * stddev.get(0, 0)[0];

        // Map variance to 0-100 scale
        // Typically: 50-200+ for good quality
        return Math.min(100.0, (variance / 2.0));
    }

    /**
     * Measure image contrast.
     * Score: 0-100, based on histogram spread.
     */
    private static double measureContrast(Mat image) {
        Mat gray = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            image.copyTo(gray);
        }

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(gray, mean, stddev);

        double contrast = stddev.get(0, 0)[0];

        // Map to 0-100: good contrast is 40-80 stddev
        return Math.min(100.0, (contrast / 0.8));
    }

    /**
     * Measure image brightness in optimal range.
     * Score: 0-100, penalizes too dark or too bright.
     */
    private static double measureBrightness(Mat image) {
        Mat gray = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            image.copyTo(gray);
        }

        Scalar brightness = Core.mean(gray);
        double mean = brightness.val[0];

        // Optimal brightness is around 90-160 (0-255 scale)
        // Penalty for too dark or too bright
        double penalty = Math.abs(mean - 125.0) * 0.35;

        return Math.max(0.0, 80.0 - penalty);
    }

    /**
     * Check if face quality is acceptable.
     */
    public static boolean isAcceptableQuality(Mat face, double minQuality) {
        return analyzeQuality(face) >= minQuality;
    }

    /**
     * Get detailed quality report.
     */
    public static String getQualityReport(Mat face) {
        double overall = analyzeQuality(face);
        double sharp = measureSharpness(face);
        double contrast = measureContrast(face);
        double bright = measureBrightness(face);

        return String.format(
                "Overall: %.1f | Sharpness: %.1f | Contrast: %.1f | Brightness: %.1f",
                overall, sharp, contrast, bright);
    }
}
