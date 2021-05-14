package de.energiequant.vatsim.compatibility.legacyproxy.utils;

import java.util.Collection;

import org.vatplanner.dataformats.vatsimpublic.utils.GeoPoint2D;

/**
 * Helper methods to perform geographic calculations.
 */
public class GeoMath {
    private static final double LATITUDE_MINIMUM = -90.0;
    private static final double LATITUDE_MAXIMUM = 90.0;

    private static final double DEGREES_TO_RADIANS_FACTOR = Math.PI / 180.0;
    private static final double RADIANS_TO_DEGREES_FACTOR = 180.0 / Math.PI;

    /**
     * Thrown if the given argument is not within a valid value range.
     */
    public static class OutOfRange extends IllegalArgumentException {
        private OutOfRange(double actual, double expectedMin, double expectedMax) {
            super(String.format("Out of range: %f must be within [%f, %f]", actual, expectedMin, expectedMax));
        }
    }

    private GeoMath() {
        // utility class, hide contructor
    }

    /**
     * Calculates the average center of given points. Implemented using the formula
     * described on <a href=
     * "https://carto.com/blog/center-of-points/">https://carto.com/blog/center-of-points/</a>.
     * 
     * @param points points to calculate average center for, must not be empty
     * @return center point calculated by average
     * @throws OutOfRange if latitude is not within bounds of &pm; 90&deg;
     */
    public static GeoPoint2D average(Collection<GeoPoint2D> points) {
        int numPoints = points.size();

        if (numPoints == 1) {
            return points.iterator().next();
        } else if (numPoints == 0) {
            throw new IllegalArgumentException("No points given to calculate center for.");
        }

        double sumLatitudes = 0.0;
        double sumZeta = 0.0;
        double sumXi = 0.0;

        for (GeoPoint2D point : points) {
            double latitude = point.getLatitude();
            checkLatitude(latitude);
            sumLatitudes += latitude;

            double longitudeRad = point.getLongitude() * DEGREES_TO_RADIANS_FACTOR;
            sumZeta += Math.sin(longitudeRad);
            sumXi += Math.cos(longitudeRad);
        }

        double centerLatitude = sumLatitudes / numPoints;
        double centerLongitude = Math.atan2(sumZeta / numPoints, sumXi / numPoints) * RADIANS_TO_DEGREES_FACTOR;

        return new GeoPoint2D(centerLatitude, centerLongitude);
    }

    private static void checkLatitude(double latitude) {
        if ((latitude < LATITUDE_MINIMUM) || (latitude > LATITUDE_MAXIMUM)) {
            throw new OutOfRange(latitude, LATITUDE_MINIMUM, LATITUDE_MAXIMUM);
        }
    }
}
