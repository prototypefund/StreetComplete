package de.westnordost.osmagent.util;

import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.LatLon;
import de.westnordost.osmapi.map.data.OsmLatLon;

/** Calculate stuff assuming a spherical Earth. The Earth is not spherical, but it is a good
 *  approximation and totally sufficient for our use here. */
public class SphericalEarthMath
{
	/**
	 * In meters. See https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
	 */
	public static final double EARTH_RADIUS = 6371000;

	/**
	 * Calculate a bounding box that contains the given circle. In other words, it is a square
	 * centered at the given position and with a side length of radius*2
	 * @param center of the circle
	 * @param radius in meters
	 * @return The bounding box that contains the area
	 */
	public static BoundingBox enclosingBoundingBox(LatLon center, double radius)
	{
		double distance = sqrt(2) * radius;
		LatLon min = translate(center, distance, 225);
		LatLon max = translate(center, distance, 45);

		return new BoundingBox(min, max);
	}

	/** @return a new position in the given distance and angle from the original position */
	public static LatLon translate(LatLon pos, double distance, double angle)
	{
		double φ1 = Math.toRadians(pos.getLatitude());
		double λ1 = Math.toRadians(pos.getLongitude());
		double α1 = Math.toRadians(angle);
		double σ12 = distance / EARTH_RADIUS;

		double y = sin(φ1) * cos(σ12) + cos(φ1) * sin(σ12) * cos(α1);

		double a = cos(φ1) * cos(σ12) - sin(φ1) * sin(σ12) * cos(α1);
		double b = sin(σ12) * sin(α1);
		double x = sqrt(sqr(a) + sqr(b));

		double φ2 = atan2(y, x);
		double λ2 = λ1 + atan2(b, a);

		return createTranslated(Math.toDegrees(φ2), Math.toDegrees(λ2));
	}

	private static LatLon createTranslated(double lat, double lon)
	{
		if(lon > 180) lon -= 360;
		else if(lon < -180) lon += 360;

		boolean crossedPole = false;
		// north pole
		if(lat > 90)
		{
			lat = 180-lat;
			crossedPole = true;
		}
		// south pole
		else if(lat < -90)
		{
			lat = -180-lat;
			crossedPole = true;
		}

		if(crossedPole)
		{
			lon += 180;
			if(lon > 180) lon -= 360;
		}

		return new OsmLatLon(lat, lon);
	}

	/**
	 * @return distance between two points in meters
	 */
	public static double distance(LatLon pos1, LatLon pos2)
	{
		return EARTH_RADIUS * distance(
				Math.toRadians(pos1.getLatitude()),
				Math.toRadians(pos1.getLongitude()),
				Math.toRadians(pos2.getLatitude()),
				Math.toRadians(pos2.getLongitude()
				));
	}

	/** @return initial bearing from one point to the other.<br/>
	 *          If you take a globe and draw a line straight up to the north pole from pos1 and a
	 *          second one that connects pos1 and pos2, this is the angle between those two
	 *          lines */
	public static double bearing(LatLon pos1, LatLon pos2)
	{
		double bearing =  Math.toDegrees(bearing(
				Math.toRadians(pos1.getLatitude()),
				Math.toRadians(pos1.getLongitude()),
				Math.toRadians(pos2.getLatitude()),
				Math.toRadians(pos2.getLongitude())
		));

		if(bearing < 0) bearing += 360;
		if(bearing >= 360) bearing -= 360;
		return bearing;
	}

	/** @return final initial bearing from one point to the other.<br/>
	 *          If you take a globe and draw a line straight up to the north pole from <em>pos2</em>
	 *          and a second one that connects pos1 and pos2 (and goes on straight after this), this
	 *          is the angle between those two lines */
	public static double finalBearing(LatLon pos1, LatLon pos2)
	{
		double bearing =  Math.toDegrees(finalBearing(
				Math.toRadians(pos1.getLatitude()),
				Math.toRadians(pos1.getLongitude()),
				Math.toRadians(pos2.getLatitude()),
				Math.toRadians(pos2.getLongitude())
		));

		if(bearing < 0) bearing += 360;
		return bearing;
	}

	// https://en.wikipedia.org/wiki/Great-circle_navigation#cite_note-2
	private static double distance(double φ1, double λ1, double φ2, double λ2)
	{
		double Δλ = λ2 - λ1;

		double y = sqrt(sqr(cos(φ2)*sin(Δλ)) + sqr(cos(φ1)*sin(φ2) - sin(φ1)*cos(φ2)*cos(Δλ)));
		double x = sin(φ1)*sin(φ2) + cos(φ1)*cos(φ2)*cos(Δλ);
		return atan2(y, x);
	}

	//See https://en.wikipedia.org/wiki/Great-circle_navigation#Course_and_distance
	private static double bearing(double φ1, double λ1, double φ2, double λ2)
	{
		double Δλ = λ2 - λ1;
		return Math.atan2(sin(Δλ), cos(φ1) * tan(φ2) - sin(φ1) * cos(Δλ));
	}

	private static double finalBearing(double φ1, double λ1, double φ2, double λ2)
	{
		double Δλ = λ2 - λ1;
		return Math.atan2(sin(Δλ), -cos(φ2)*tan(φ1) + sin(φ1)*cos(Δλ));
	}

	// Just for better readability of the formulas. Hoping JVM does proper inline expansion
	private static double cos(double x) { return Math.cos(x); }
	private static double sin(double x) { return Math.sin(x); }
	private static double tan(double x) { return Math.tan(x); }
	private static double sqr(double x) { return Math.pow(x, 2); }
	private static double sqrt(double x) { return Math.sqrt(x); }
	private static double atan2(double y, double x) { return Math.atan2(y,x); }

}
