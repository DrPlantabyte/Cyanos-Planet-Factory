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

package hall.collin.christopher.cpf;

import hall.collin.christopher.cpf.data.PlanetConfig;
import hall.collin.christopher.cpf.graphics.GlobeAnimator;
import hall.collin.christopher.cpf.math.SphereLUT;
import hall.collin.christopher.worldgeneration.AbstractPlanet;
import hall.collin.christopher.worldgeneration.TectonicHydrologyPlanet;
import hall.collin.christopher.worldgeneration.biomes.StandardBiomeFactory;
import hall.collin.christopher.worldgeneration.graphics.MercatorMapProjector;
import hall.collin.christopher.worldgeneration.graphics.PlanetPainter;
import hall.collin.christopher.worldgeneration.graphics.VegetationPainter;
import hall.collin.christopher.worldgeneration.math.SpherePoint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.DoubleAdder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

/**
 * FXML Controller class
 *
 * @author CCHall
 */
public class WorldMapScreenController implements Initializable {

	
	private App app = null;
	
	
	@FXML private ImageView globeSpinner;
	@FXML private ImageView mapView;
	@FXML private CheckBox biomeCheckbox;
	@FXML private CheckBox sectorCheckbox;
	@FXML private CheckBox landShaderCheckbox;
	@FXML private CheckBox oceanShaderCheckbox;
	
	@FXML private Label planetName;
	@FXML private Label planetStat_radius;
	@FXML private Label planetStat_gravity;
	@FXML private Label planetStat_tilt;
	@FXML private Label planetStat_solarPower;
	@FXML private Label planetStat_atmosphere;
	@FXML private Label planetStat_oceanCover;
	@FXML private Label planetStat_aveTemp;
	@FXML private Label planetStat_Tscore;
	
	@FXML private Button backButton;
	@FXML private Button exportMercatorButton;
	@FXML private Button exportGlobeButton;
	
	@FXML private Pane legendPane;
	@FXML private Pane mapOptionsPane;
	@FXML private Pane planetPropertiesPane;
	
	private NumberFormat nf = NumberFormat.getNumberInstance();
	
	private WritableImage currentMapView = null;
	private double tilt = 0; // tilt of spinning globe
	private BufferedImage globeTexture = null;
	
	
	// Layer caches, calculated as needed
	private BufferedImage vegetationLayer = null;
	private BufferedImage biomeLayer = null;
	private BufferedImage landShadeLayer = null;
	private BufferedImage oceanShadeLayer = null;
	private BufferedImage labelLayer = null;
	
	GlobeAnimator spinner = null;
	
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(0);
		// back button
		backButton.onActionProperty().setValue((ActionEvent ae)->{goBack(ae);});
		// checkboxes
		biomeCheckbox.onActionProperty().setValue((ActionEvent ae)->{update(ae);});
		sectorCheckbox.onActionProperty().setValue((ActionEvent ae)->{update(ae);});
		landShaderCheckbox.onActionProperty().setValue((ActionEvent ae)->{update(ae);});
		oceanShaderCheckbox.onActionProperty().setValue((ActionEvent ae)->{update(ae);});
		// legend
		makeLegend(false);
		// Export buttons
		exportMercatorButton.onActionProperty().setValue((ActionEvent ae)->{exportMercator(ae);});
		exportGlobeButton.onActionProperty().setValue((ActionEvent ae)->{exportGlobes(ae);});
		// TODO: fix windowing issues (add modality and enforce correct window ordering and export window close when done)
		// TODO: foreward to sector map
	}	
	
	public void setMapImage(BufferedImage vegetationLayer, BufferedImage landShadeLayer){
		this.vegetationLayer = vegetationLayer;
		this.landShadeLayer = landShadeLayer;
		globeTexture = vegetationLayer;//layerImages(vegetationLayer,landShadeLayer);
		currentMapView = layerImagesToJFX(vegetationLayer);
		mapView.setImage(currentMapView);
		spinner = new GlobeAnimator(globeSpinner,App.GUImapSize,globeTexture,tilt);
	}
	/**
	 * Combines multiple images, from bottom to top.
	 * @param layers Images to layer, with bottom image first, top image last. 
	 * Any null values in the list will be skipped without causing an exception.
	 * @return 
	 */
	private static BufferedImage layerImages(BufferedImage... layers){
		int width = layers[0].getWidth();
		int height = layers[0].getHeight();
		BufferedImage output = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = output.createGraphics();
		for(BufferedImage l : layers){
			if(l == null) continue;
			g.drawImage(l, 0, 0, null);
		}
		return output;
	}
	
	@FXML private void goBack(ActionEvent ae){
		// clear the cache to make GC of data not dependant on GC of GUI
		vegetationLayer = null;
		biomeLayer = null;
		landShadeLayer = null;
		oceanShadeLayer = null;
		labelLayer = null;
		app.backToStartScreen();
		spinner.terminate();
	}
	/**
	 * Combines multiple images, from bottom to top.
	 * @param layers Images to layer, with bottom image first, top image last. 
	 * Any null values in the list will be skipped without causing an exception.
	 * @return 
	 */
	private static WritableImage layerImagesToJFX(BufferedImage... layers){
		BufferedImage temp = layerImages(layers);
		WritableImage output = new WritableImage(temp.getWidth(),temp.getHeight());
		drawImageOnImage(temp,output);
		return output;
	}
	
	private static void drawImageOnImage(BufferedImage src, WritableImage dest){
		int width = min(src.getWidth(), (int)dest.getWidth());
		int height = min(src.getHeight(), (int)dest.getHeight());
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				int opx = dest.getPixelReader().getArgb(x, y);
				int oa = (opx >> 24) & 0xFF;
				int or = (opx >> 16) & 0xFF;
				int og = (opx >>  8) & 0xFF;
				int ob = (opx      ) & 0xFF;
				int npx = src.getRGB(x, y);
				int na = (npx >> 24) & 0xFF;
				int nr = (npx >> 16) & 0xFF;
				int ng = (npx >>  8) & 0xFF;
				int nb = (npx      ) & 0xFF;
				int a = min(oa+na,0xFF);
				int r = min((nr * na + or * oa * (0xFF - or)) / 0xFF,0xFF);
				int g = min((ng * na + og * oa * (0xFF - og)) / 0xFF,0xFF);
				int b = min((nb * na + ob * oa * (0xFF - ob)) / 0xFF,0xFF);
				int px = (a << 24) | (r << 16) | (g << 8) | (b);
				dest.getPixelWriter().setArgb(x, y, px);
			}
		}
	}
	
	private static int min(int... v){
		int min = v[0];
		for(int i =0; i < v.length; i++){
			if(v[i] < min) min = v[i];
		}
		return min;
	}
	
	
	
	/** Used for callbacks to switch screens */
	void setMaster(App master) {
		app = master;
	}

	void setAxisTilt(double axisTilt) {
		tilt = axisTilt;
		if(globeTexture != null){
			spinner = new GlobeAnimator(globeSpinner,App.GUImapSize,globeTexture,tilt);
		}
	}

	private void update(ActionEvent ae) {
		ArrayList<BufferedImage> mapLayers = new ArrayList<>();
		if(biomeCheckbox.isSelected() && biomeLayer == null){
			final DoubleAdder ptracker = new DoubleAdder();
			final BufferedImage[] handle = new BufferedImage[1]; // funky workaround to needing final variables in lambdas
			handle[0] = null;
			boolean successful = ProgressScreenController.waitOnTask(()->{
				MercatorMapProjector mmp = new MercatorMapProjector();
				mmp.enableHillshading(false);
				mmp.enableOceanshading(false);
				mmp.enableMap(true);
				PlanetPainter pp = StandardBiomeFactory.createPlanetPainter();
				handle[0] = mmp.createMapProjection(app.getPlanet(), App.GUImapSize, pp, ptracker);
			}, "Computing biome map...", ptracker, 1, App.getAppStage());
			if(successful) {biomeLayer = handle[0];} else {
				biomeCheckbox.setSelected(false);
			}
		}
		if(oceanShaderCheckbox.isSelected() && oceanShadeLayer == null){
			final DoubleAdder ptracker = new DoubleAdder();
			final BufferedImage[] handle = new BufferedImage[1]; // funky workaround to needing final variables in lambdas
			handle[0] = null;
			boolean successful = ProgressScreenController.waitOnTask(()->{
				MercatorMapProjector mmp = new MercatorMapProjector();
				mmp.enableHillshading(false);
				mmp.enableOceanshading(true);
				mmp.enableMap(false);
				PlanetPainter pp = StandardBiomeFactory.createPlanetPainter();
				handle[0] = mmp.createMapProjection(app.getPlanet(), App.GUImapSize, pp, ptracker);
			}, "Computing underwater terrain...", ptracker, 1, App.getAppStage());
			if(successful) {oceanShadeLayer = handle[0];} else {
				oceanShaderCheckbox.setSelected(false);
			}
		}
		if(landShaderCheckbox.isSelected() && landShadeLayer == null){
			final DoubleAdder ptracker = new DoubleAdder();
			final BufferedImage[] handle = new BufferedImage[1]; // funky workaround to needing final variables in lambdas
			handle[0] = null;
			boolean successful = ProgressScreenController.waitOnTask(()->{
				MercatorMapProjector mmp = new MercatorMapProjector();
				mmp.enableHillshading(true);
				mmp.enableOceanshading(false);
				mmp.enableMap(false);
				PlanetPainter pp = StandardBiomeFactory.createPlanetPainter();
				handle[0] = mmp.createMapProjection(app.getPlanet(), App.GUImapSize, pp, ptracker);
			}, "Computing underwater terrain...", ptracker, 1, App.getAppStage());
			if(successful) {landShadeLayer = handle[0];} else {
				landShaderCheckbox.setSelected(false);
			}
		}
		if(sectorCheckbox.isSelected() && labelLayer == null){
			throw new UnsupportedOperationException("Not supported yet.");
			// TODO: sectors
		}
		if(vegetationLayer != null)	mapLayers.add(vegetationLayer);
		if(biomeLayer != null && biomeCheckbox.isSelected() )	{
			mapLayers.add(biomeLayer);
		}
		if(oceanShadeLayer != null && oceanShaderCheckbox.isSelected() )	mapLayers.add(oceanShadeLayer);
		if(landShadeLayer != null && landShaderCheckbox.isSelected() )	mapLayers.add(landShadeLayer);
		
		// update GUI
		spinner.terminate();
		makeLegend(biomeCheckbox.isSelected());
		globeTexture = layerImages(mapLayers.toArray(new BufferedImage[0]));
		currentMapView = layerImagesToJFX(mapLayers.toArray(new BufferedImage[0]));
		mapView.setImage(currentMapView);
		spinner = new GlobeAnimator(globeSpinner,App.GUImapSize,globeTexture,tilt);
	}

	private void makeLegend(boolean isBiome) {
		legendPane.getChildren().clear();
		Label title = new Label("Legend");
		title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
		legendPane.getChildren().add(title);
		
		if(isBiome){
			StandardBiomeFactory.StandardBiome[] biomes = StandardBiomeFactory.allBiomes;
			for(StandardBiomeFactory.StandardBiome b : biomes){
				int color = b.getColorARGB() & 0x00FFFFFF;
				addTextLabelWithColor(legendPane,b.getName(),color);
			}
		} else {
			VegetationPainter vp = new VegetationPainter();
			final AbstractPlanet fakeOcean,fakeIceSheet,fakeBorialForest,fakeTemperateForest,
					fakeJungle,fakePlains,fakeDesert,fakeMoon;
			// <editor-fold defaultstate="collapsed" desc="AbstractPlanet implementations">
			fakeOcean = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return -1000;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 12;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakeIceSheet = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return -1000;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return -20;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakeBorialForest = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 80;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 7;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakeTemperateForest = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 80;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 15;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakeJungle = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 80;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 25;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakePlains = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 40;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 15;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakeDesert = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return 10;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 22;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			fakeMoon = new AbstractPlanet(){

				@Override
				public double getRoughness(double longitude, double latitude, double precision) {
					return 0;
				}

				@Override
				public double getAltitude(double longitude, double latitude, double precision) {
					return 100;
				}

				@Override
				public double getMoisture(double longitude, double latitude, double precision) {
					return -100;
				}

				@Override
				public double getTemperature(double longitude, double latitude, double precision) {
					return 15;
				}

				@Override
				public double getRadius() {
					return 6e6;
				}
			};
			// </editor-fold>
			int color_moon = vp.getColor(fakeMoon, 0, 0, 0, 0, 0);
			int color_ocean = vp.getColor(fakeOcean, 0, 0, 0, 0, 0);
			int color_ice = vp.getColor(fakeIceSheet, 0, 0, 0, 0, 0);
			int color_desert = vp.getColor(fakeDesert, 0, 0, 0, 0, 0);
			int color_plains = vp.getColor(fakePlains, 0, 0, 0, 0, 0);
			int color_forest = vp.getColor(fakeTemperateForest, 0, 0, 0, 0, 0);
			int color_jungle = vp.getColor(fakeJungle, 0, 0, 0, 0, 0);
			int color_borial = vp.getColor(fakeBorialForest, 0, 0, 0, 0, 0);
			addTextLabelWithColor(legendPane,"Moonscape",color_moon);
			addTextLabelWithColor(legendPane,"Ice",color_ice);
			addTextLabelWithColor(legendPane,"Ocean",color_ocean);
			addTextLabelWithColor(legendPane,"Desert",color_desert);
			addTextLabelWithColor(legendPane,"Plains",color_plains);
			addTextLabelWithColor(legendPane,"Forest",color_forest);
			addTextLabelWithColor(legendPane,"Jungle",color_jungle);
			addTextLabelWithColor(legendPane,"Borial Forest",color_borial);
		}
	}

	private void addTextLabelWithColor(Pane legendPane, String text, int color) {
		Label l = new Label(text);
		String hex = Integer.toHexString(color & 0x00FFFFFF);
		StringBuilder zeroPadding = new StringBuilder(6);
		for(int i = 6; i > hex.length(); i--){
			zeroPadding.append('0');
		}
		l.setStyle("-fx-text-fill: #"+zeroPadding.toString() + hex + ";");
		legendPane.getChildren().add(l);
	}

private PlanetConfig planetStats = null;	
	void setStats(PlanetConfig planetOptions, TectonicHydrologyPlanet planet) {
		planetStats = planetOptions;
		planetName.setText(planetOptions.getName());
		planetStat_radius.setText("Radius: "+nf.format(planetOptions.getRadius())+" km ("+(int)(planetOptions.getRadius() * 100 / 6370)+"% rₑₐᵣₜₕ)");
		double gravity = planetOptions.getRadius() * (9.80665 / 6370);
		planetStat_gravity.setText("Gravity: "+nf.format(gravity) + " m/s² ("+nf.format(gravity / 9.80665)+" ₓg)");
		planetStat_tilt.setText("Axis Tilt: " + nf.format(planetOptions.getAxisTilt())+"°");
		planetStat_solarPower.setText("Solar Intensity: "+nf.format(planetOptions.getSolarFlux())+" W/m²");
		planetStat_atmosphere.setText("Atmospheric Pressure: " + nf.format(planetOptions.getAtmosphere())+" kPa ("+nf.format(planetOptions.getAtmosphere() / 101.0)+" bar)");
		planetStat_oceanCover.setText("Ocean Coverage: "+nf.format(planet.percentOcean())+"% ocean");
		double meanTemp = calculateMeanTemperature(planet);
		planetStat_aveTemp.setText("Planetary Mean Temperature: "+nf.format(meanTemp)+"°C (\""+temperatureComment(meanTemp)+"\")");
		int tScore = calculateTScore(planet);
		planetStat_Tscore.setText("T-score: T" + tScore + " (\""+TScoreComment(tScore)+"\")");
	}

	private double calculateMeanTemperature(AbstractPlanet planet) {
		double delta = Math.PI / 10;
		int i = 0;
		double sumOfAve = 0;
		for(double lon = 0; lon < Math.PI*2; lon += delta){
			double sum = 0;
			double j = 0;
			for(double lat = (-0.5 * Math.PI); lat <= (0.5*Math.PI); lat += delta){
				double bias = Math.cos(lat); // correction factor for oversampling of the poles
				sum += bias * planet.getTemperature(lon, lat, delta*planet.getRadius());
				j += bias;
			}
			sumOfAve += (sum / j);
			i++;
		}
		return (sumOfAve / i);
	}

	private String TScoreComment(int tScore) {
		switch(tScore){
			case 0:
				return "barren rock, survival not possible";
			case 1:
				return "extremely hostile environment, may have microbial life";
			case 2:
				return "unstable environment, home to highly specialized life-forms";
			case 3:
				return "hospitable, full of life";
			default:
				return "invalid T-Score";
		}
	}

	private int calculateTScore(AbstractPlanet planet) {
		double n = 0;
		double sum = 0;
		double delta = Math.PI / 10;
		double p = delta * planet.getRadius();
		
		for(double lon = 0; lon < Math.PI*2; lon += delta){
			for(double lat = (-0.5 * Math.PI); lat <= (0.5*Math.PI); lat += delta){
				StandardBiomeFactory.StandardBiome b  = StandardBiomeFactory.biomeFromTempRainfall(planet.getTemperature(lon, lat, p), planet.getMoisture(lon, lat, p));
				double bias = Math.cos(lat); // correction factor for oversampling of the poles
				sum += bias * b.getTScore();
				n += bias;
			}
		}
		return (int)Math.round(sum / n);
	}

	private String temperatureComment(double meanTemp) {
		if(meanTemp <= 10 && meanTemp > 0) return "cold";
		if(meanTemp <= 20 && meanTemp > 10) return "comfortable";
		if(meanTemp <= 30 && meanTemp > 20) return "hot";
		if(meanTemp <= 0 && meanTemp > -20) return "extremely cold";
		if(meanTemp <= 50 && meanTemp > 30) return "dangerously hot";
		
		return "lethal";
	}

	private void exportMercator(ActionEvent ae) {
		PainterOption po = PainterOption.VEGETATION;
		if(biomeCheckbox.isSelected())po = PainterOption.BIOME;
		String worldName = planetStats.getName();
		Path saveDir = app.getSaveDir();
		ExportScreenController.exportMap(worldName, App.GUImapSize, po, landShaderCheckbox.isSelected(), oceanShaderCheckbox.isSelected(), sectorCheckbox.isSelected(), saveDir, 
				new MercatorMapExporter());
		app.setSaveDir(saveDir);
	}

	private void exportGlobes(ActionEvent ae) {
		PainterOption po = PainterOption.VEGETATION;
		if(biomeCheckbox.isSelected())po = PainterOption.BIOME;
		String worldName = planetStats.getName();
		Path saveDir = app.getSaveDir();
		ExportScreenController.exportMap(worldName, App.GUImapSize, po, landShaderCheckbox.isSelected(), oceanShaderCheckbox.isSelected(), sectorCheckbox.isSelected(), saveDir, 
				new GlobeMapExporter());
		app.setSaveDir(saveDir);
	}
	
	private class MercatorMapExporter implements MapExporter{

		@Override
		public double maxProgress() {
			return 1.0;
		}

		@Override
		public BufferedImage generateMap(int size, PainterOption painterType, boolean landShading, boolean oceanShading, boolean drawLabels, DoubleAdder progressTracker) {
			PlanetPainter pp;
			switch (painterType) {
				case BIOME:
					pp = StandardBiomeFactory.createPlanetPainter();
					break;
				case VEGETATION:
				default:
					pp = new VegetationPainter();
			}
			MercatorMapProjector mp = new MercatorMapProjector();
			mp.enableOceanshading(oceanShading);
			mp.enableHillshading(landShading);
			// TODO: sector labels
			return mp.createMapProjection(app.getPlanet(), size, pp, progressTracker);
		}
	}
	
	private class GlobeMapExporter implements MapExporter{

		@Override
		public double maxProgress() {
			return 2.0;
		}

		@Override
		public BufferedImage generateMap(int size, PainterOption painterType, boolean landShading, boolean oceanShading, boolean drawLabels, DoubleAdder progressTracker) {
			PlanetPainter pp;
			switch (painterType) {
				case BIOME:
					pp = StandardBiomeFactory.createPlanetPainter();
					break;
				case VEGETATION:
				default:
					pp = new VegetationPainter();
			}
			MercatorMapProjector mp = new MercatorMapProjector();
			mp.enableOceanshading(oceanShading);
			mp.enableHillshading(landShading);
			// TODO: sector labels
			final BufferedImage texture = mp.createMapProjection(app.getPlanet(), size, pp, progressTracker);
			// globe rotation snapshots
			final BufferedImage globes = new BufferedImage(size*4,size,BufferedImage.TYPE_INT_ARGB);
			ArrayList<Callable<Boolean>> taskList = new ArrayList<>(size);
			final SphereLUT lut = new SphereLUT(size,size,0);
			for(int i = 0; i < 4; i++){
				// draw globe from 4 different angles
				double rotation = i * 0.5 * Math.PI;
				int xOffset = i * size;
				for(int nfy = 0; nfy < size; nfy++){
					final int y = nfy;
					taskList.add(()->{
					for(int x = 0; x < size; x++){
						if(Thread.interrupted() ){
							return false;
						}
						SpherePoint p = lut.getCoordinateAt(x, y);
						if(p == null) continue;
						int u = (int)((p.getLongitude() + rotation) / (2 * Math.PI) * texture.getWidth()) % texture.getWidth();
						int v = (int)((p.getLatitude() + (0.5 * Math.PI)) / Math.PI * texture.getHeight()) % texture.getHeight();
						if(u < 0 ) u += texture.getWidth();
						globes.setRGB(x+xOffset, y, texture.getRGB(u, v));
					}
					progressTracker.add((0.25 / size));
					return true;
					});
				}
			}
			ForkJoinPool.commonPool().invokeAll(taskList);
			return globes;
		}
	}
}
