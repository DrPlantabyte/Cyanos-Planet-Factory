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
import hall.collin.christopher.worldgeneration.AbstractPlanet;
import hall.collin.christopher.worldgeneration.TectonicHydrologyPlanet;
import hall.collin.christopher.worldgeneration.graphics.MercatorMapProjector;
import hall.collin.christopher.worldgeneration.graphics.PlanetPainter;
import hall.collin.christopher.worldgeneration.graphics.VegetationPainter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Entry point for the application and master controller of all the 
 * other FXML controllers.
 * @author CCHall
 */
public class App extends Application {
	
	public final int VERSION_MAJOR;
	public final int VERSION_MINOR;
	public final int VERSION_BUILD;
	
	private Path saveDir = Paths.get(System.getProperty("user.home"));
	
	
	public App(){
		int build = -1;
		int major = -1;
		int minor = -1;
		// get version info
		Properties versionProps =  new Properties();
		try {
			versionProps.load(getClass().getResourceAsStream("version.properties"));
			build = Integer.parseInt(versionProps.getProperty("BUILD"));
			major = Integer.parseInt(versionProps.getProperty("VERSION"));
			minor = Integer.parseInt(versionProps.getProperty("SUBVERSION"));
		} catch (IOException | NumberFormatException ex) {
			Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Failed to read version info", ex);
		}
		VERSION_MAJOR = major;
		VERSION_MINOR = minor;
		VERSION_BUILD = build;
	}
	
	public String getVersionString(){
		return VERSION_MAJOR+"."+VERSION_MINOR+"."+VERSION_BUILD;
	}
	
	private static Stage rootStage = null;
	private static App singleton = null;
	
	public static Stage getAppStage() {
		if(rootStage == null){
			throw new RuntimeException("Application stage is null because the application has no been initialized yet!");
		}
		return rootStage;
	}
	
	public static App getInstance() {
		if(singleton == null){
			throw new RuntimeException("Application is null because the application has no been initialized yet!");
		}
		return singleton;
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		// set static instances
		rootStage = stage;
		singleton = this;
		System.out.println("Starting Cyano's Planet Factory - V "+VERSION_MAJOR+"."+VERSION_MINOR+"."+VERSION_BUILD);
		
		if(Files.isDirectory(Paths.get(System.getProperty("user.home"), "Pictures"))){
			saveDir = Paths.get(System.getProperty("user.home"), "Pictures");
		}
		
		// Load first screen
		FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/StartScreen.fxml"));
		Parent root = loader.load();
		StartScreenController ssc = loader.getController();
		ssc.setMaster(this);
		switchToScreen(root);
		stage.show();
		
		
		
		
	}
	/**
	 * Switches the content of the main window to the given JavaFX node 
	 * tree. Example:<br><code>
	 * FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/screen.fxml"));<br>
	 * Parent root = (Parent)fxmlLoader.load();<br>
	 * switchToScreen(root);<br>
	 * </code><br>
	 * @param rootFXMLElement Root of the node tree
	 */
	private void switchToScreen(Parent rootFXMLElement){
		getAppStage().setScene(new Scene(rootFXMLElement));
		getAppStage().sizeToScene();
	}
	final static int GUImapSize = 270;
	private TectonicHydrologyPlanet planet = null;
	private BufferedImage vegTexture = null;
	private BufferedImage landShadeTexture = null;
	public void startScreenDone(final PlanetConfig planetOptions){
		final java.util.concurrent.atomic.DoubleAdder pt = new java.util.concurrent.atomic.DoubleAdder();
		
		boolean complete = ProgressScreenController.waitOnTask(()->{
			planet = TectonicHydrologyPlanet.createPlanet(planetOptions.getSeed(),
					planetOptions.getRadius(),planetOptions.getAtmosphere(),
					planetOptions.getOceanFraction(),planetOptions.getSolarFlux(),
					pt);
			PlanetPainter pp = new VegetationPainter();
			MercatorMapProjector mp = new MercatorMapProjector();
			mp.enableHillshading(false);
			vegTexture = mp.createMapProjection(planet, GUImapSize, pp, pt);
			mp.enableHillshading(true);
			mp.enableMap(false);
			landShadeTexture = mp.createMapProjection(planet, GUImapSize, pp, pt);
		}, "Generating Planet...", pt, 3, rootStage);
		if(complete){
			// planet generation not canceled, continue to next screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/WorldMapScreen.fxml"));
			try{
				Parent root = loader.load();
				WorldMapScreenController wmsc = loader.getController();
				wmsc.setMaster(this);
				wmsc.setAxisTilt(planetOptions.getAxisTilt());
				wmsc.setMapImage(vegTexture,landShadeTexture);
				
				switchToScreen(root);
				getAppStage().setWidth(800); // MapScreen does not correctly compute its size
				getAppStage().setHeight(700);
				javafx.application.Platform.runLater(()->{wmsc.setStats(planetOptions,planet);});
				
			} catch(IOException ex){
				// shouldn't happen!
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load FXML!",ex);
			}
		}
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}
	
	
	public static String cleanSeedString(String seed){
		return seed.trim().toUpperCase(Locale.US);
	}

	void backToStartScreen() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/StartScreen.fxml"));
		try{
			Parent root = loader.load();
			StartScreenController ssc = loader.getController();
			ssc.setMaster(this);
			switchToScreen(root);
		} catch(IOException ex){
			// shouldn't happen!
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to load FXML!",ex);
		}
	}

	AbstractPlanet getPlanet() {
		return planet;
	}

	Path getSaveDir() {
		return saveDir;
	}

	void setSaveDir(Path saveDir) {
		this.saveDir = saveDir;
	}
	
	@SuppressWarnings("ThrowableResultIgnored")
	public static void showErrorMessage(String message, Exception ex) {
		final AtomicBoolean isSure = new AtomicBoolean(false);
		final Stage dialog = new Stage(StageStyle.DECORATED); // check back if transparent was a good or bad idea
		
		StringBuilder detailedMessage = new StringBuilder();
		Throwable cause = ex;
		while(cause != null){
			detailedMessage.append("\nCaused by thrown exception of class ");
			detailedMessage.append(cause.getClass().getCanonicalName());
			detailedMessage.append("\n");
			detailedMessage.append(cause.getLocalizedMessage()).append("\n\t");
			for(StackTraceElement e : cause.getStackTrace()){
				detailedMessage.append(e.toString()).append("\n\t");
			}
			cause = cause.getCause();
		}
		
		VBox rootPane = new VBox();
		rootPane.setSpacing(4);
		rootPane.setPadding(new Insets(8));
		Label title = new Label("ERROR: " + ex.getClass().getSimpleName());
		title.setStyle("-fx-font-size: 125%; -fx-font-weight: bold;");
		Label msg = new Label(message);
		Label msg2 = new Label(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
		TextArea detailsBox = new TextArea();
		detailsBox.setPrefWidth(300);
		detailsBox.setPrefHeight(200);
		detailsBox.setText(detailedMessage.toString());
		detailsBox.setEditable(false);
		Button okButton = new Button("OK");
		okButton.onActionProperty().setValue((ActionEvent ae)->{dialog.close();});
		rootPane.getChildren().addAll(title,msg,msg2,detailsBox,okButton);
		
		
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(App.getAppStage());
		dialog.setTitle("Error!");
		dialog.setScene(new Scene(rootPane));
		dialog.toFront();
		dialog.showAndWait();
	}
}
