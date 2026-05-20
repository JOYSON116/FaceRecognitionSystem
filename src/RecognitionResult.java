/**
 * RecognitionResult - encapsulates face recognition result with confidence.
 */
public class RecognitionResult {

    public final String personId;
    public final String imageFile;
    public final double confidence; // 0.0-1.0
    public final double similarityScore; // Cosine similarity
    public final boolean isConfident;
    public final String details;

    public RecognitionResult(String personId, String imageFile, double similarity,
            double confidence, boolean isConfident, String details) {
        this.personId = personId;
        this.imageFile = imageFile;
        this.similarityScore = similarity;
        this.confidence = confidence;
        this.isConfident = isConfident;
        this.details = details;
    }

    public static RecognitionResult unknown() {
        return new RecognitionResult("unknown", "unknown", 0.0, 0.0, false,
                "No confident match found");
    }

    @Override
    public String toString() {
        return String.format("%s (confidence: %.1f%%, similarity: %.4f) - %s",
                personId, confidence * 100, similarityScore, details);
    }
}
