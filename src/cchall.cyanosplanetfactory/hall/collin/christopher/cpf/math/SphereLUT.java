/*
Cyano's Planet Factory

Copyright (C) 2014 Christopher Collin Hall
email: explosivegnome@yahoo.com

Cyano's Planet Factory is distributed under the GNU General Public 
License (GPL) version 3.

Cyano's Planet Factory is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License 
as published by the Free Software Foundation, either version 3 of 
the License, or (at your option) any later version.

Cyano's Planet Factory is distributed in the hope that it will be 
useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Cyano's Planet Factory.  If not, see 
<http://www.gnu.org/licenses/>.

*/

package hall.collin.christopher.cpf.math;

import hall.collin.christopher.worldgeneration.math.SpherePoint;
import java.util.Map;

/**
 * This class creates a look-up table of pixel coordinates on a screen 
 * looking at a sphere and the longitude, latitude location on the 
 * sphere. <b>All the methods in this class are thread safe and designed for 
 * use in a multi-thread application.</b>
 * @author CCHall
 */
public class SphereLUT {
	final private Map<Integer2D,SpherePoint> LUT;
	private final double tiltAngle;
	private final int width;
	private final int height;
	private final double radius;
	private final double radiusSquared;
	
	
	public SphereLUT(int width, int height, double tilt){
		LUT = new java.util.concurrent.ConcurrentHashMap<>(width * height);
		tiltAngle = tilt;
		this.width = width;
		this.height = height;
		int smaller = ((width < height) ? width : height);
		radius = 0.5 * (double)smaller;
		radiusSquared = radius * radius;
	}
	/**
	 * Looks-up the (longitude, latitude) sphere coordinate of a pixel 
	 * on the screen.
	 * @param pixelCoord The pixel coordinate
	 * @return The (longitude, latitude) coordinate on the sphere at that 
	 * pixel, or null if the pixel does not lie on the sphere.
	 */
	public SpherePoint getCoordinateAt(Integer2D pixelCoord){
		final int x = pixelCoord.x - (width / 2);
		final int y = pixelCoord.y - (height / 2);
		
		if((x * x + y * y) >= radiusSquared){
			return null;
		}
		return LUT.computeIfAbsent(pixelCoord, (Integer2D pt) -> computeCoordinate(x,y));
	}
	/**
	 * Looks-up the (longitude, latitude) sphere coordinate of a pixel 
	 * on the screen.
	 * @param pixelX The pixel coordinate
	 * @param pixelY The pixel coordinate
	 * @return The (longitude, latitude) coordinate on the sphere at that 
	 * pixel, or null if the pixel does not lie on the sphere.
	 */
	public SpherePoint getCoordinateAt(int pixelX, int pixelY){
		return getCoordinateAt(new Integer2D(pixelX,pixelY));
	}
	/**
	 * Computation for Map reference.
	 * @param x coordinate from center of image
	 * @param y coordinate from center of image
	 * @return Lon,Lat coordinate
	 */
	private SpherePoint computeCoordinate(int x, int y){
		// apply axis tilt
		double dist = distance(x,y);
		double angle = atan2(y,x) + tiltAngle;
		 x = (int)(dist * cos(angle));
		 y = (int)(dist * sin(angle));
		double lat = asin(y/radius);
		double lon = asin((x/radius)/cos(lat));
		return new SpherePoint(lon,lat);
	}
	/** Method provided a an optimization target */
	private double sin(double a){
		return Math.sin(a);
	}
	/** Method provided a an optimization target */
	private double cos(double a){
		return Math.cos(a);
	}
	/** Method provided a an optimization target */
	private double asin(double a){
		return Math.asin(a);
	}
	/** Method provided a an optimization target */
	private double acos(double a){
		return Math.acos(a);
	}

	/** Method provided a an optimization target */
	private double atan2(double y, double x) {
		if(x == 0 && y == 0){
			return 0;
		}
		return Math.atan2(y, x);
	}

	private double distance(int x0, int y0) {
		return Math.sqrt(x0 * x0 + y0 * y0);
	}
	
}
