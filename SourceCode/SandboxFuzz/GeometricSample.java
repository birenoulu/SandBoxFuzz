import java.security.SecureRandom;

public class GeometricSample {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static int sampleGeometricTimes(double mean, SecureRandom randomSource) {
        if (mean <= 0) {
            throw new IllegalArgumentException("mean must be positive");
        }

        double p = 1.0 / mean;
        double uniform = randomSource.nextDouble();
        double logArg = 1.0 - uniform;

        if (logArg <= 0) {
            throw new IllegalArgumentException("Invalid log argument: " + logArg);
        }

        return (int) Math.ceil(Math.log(logArg) / Math.log(1.0 - p));
    }

    public static int sampleGeometricTimes(double mean) {
        if (mean <= 0) {
            throw new IllegalArgumentException("mean must be positive");
        } else if (mean == 1) {
            return secureRandom.nextInt(2);
        }

        double p = 1.0 / mean;
        double uniform = secureRandom.nextDouble();
        double logArg = 1.0 - uniform;

        if (logArg <= 0) {
            throw new IllegalArgumentException("Invalid log argument: " + logArg);
        }

        return (int) Math.ceil(Math.log(logArg) / Math.log(1.0 - p));
    }
}
