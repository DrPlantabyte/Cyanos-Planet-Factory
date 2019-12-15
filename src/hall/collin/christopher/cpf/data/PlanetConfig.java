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

package hall.collin.christopher.cpf.data;

import com.grack.nanojson.*;

/**
 * A simple data-holding class for transferring (and saving) 
 * configurations.
 * @author CCHall
 */
public class PlanetConfig {
	private final JsonObject data = new JsonObject();
	
	public PlanetConfig(String name, String seed, double radius_km, 
			double atmosphere_kPa, double ocean_fraction, double solarFlux_wattsPerSqrMeter,
			double tilt, int type, String appVersion){
		data.put("version", appVersion);
		data.put("type", type); // 0 for earth-like, 1 for random, 2 for custom
		data.put("name", name);
		data.put("seed", seed);
		data.put("radius", radius_km);
		data.put("atmosphere", atmosphere_kPa);
		data.put("ocean-fraction", ocean_fraction);
		data.put("solar-flux", solarFlux_wattsPerSqrMeter);
		data.put("tilt", tilt);
	}
	
	public String getAppVersion(){
		return data.getString("version");
	}
	public String getSeed(){
		return data.getString("seed");
	}
	public String getName(){
		return data.getString("name");
	}
	public int getType(){
		return data.getInt("type");
	}
	public double getRadius(){
		return data.getDouble("radius");
	}
	public double getAtmosphere(){
		return data.getDouble("atmosphere");
	}
	public double getOceanFraction(){
		return data.getDouble("ocean-fraction");
	}
	public double getSolarFlux(){
		return data.getDouble("solar-flux");
	}
	public double getAxisTilt(){
		return data.getDouble("tilt");
	}
}
