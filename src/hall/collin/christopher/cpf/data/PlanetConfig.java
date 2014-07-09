
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
