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
import hall.collin.christopher.worldgeneration.math.SpherePoint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javax.imageio.ImageIO;

/**
 *
 * @author CCHall
 */
public class StartScreenController implements Initializable {
	
	private App masterController = null;
	
	@FXML private Pane rootPane;
	@FXML private TextField nameField;
	@FXML private Pane optionsPane;
	@FXML private Label radiusDisplay;
	@FXML private Slider radiusSlider;
	@FXML private Label atmDisplay;
	@FXML private Slider atmSlider;
	@FXML private Label oceanDisplay;
	@FXML private Slider oceanSlider;
	@FXML private Label sunDisplay;
	@FXML private Slider sunSlider;
	@FXML private Label tiltDisplay;
	@FXML private Slider tiltSlider;
	private ToggleGroup geologyOptions;
	@FXML private RadioButton earthLikeOption;
	@FXML private RadioButton randomOption;
	@FXML private RadioButton customOption;
	@FXML private Button nextButton;
	@FXML private Button randomNameButton;
	
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		setLabelDisplay(radiusSlider,radiusDisplay," km");
		setLabelDisplay(atmSlider,atmDisplay," kPa");
		setLabelDisplay(oceanSlider,oceanDisplay,"");
		setLabelDisplay(sunSlider,sunDisplay," Watts/meter²");
		setLabelDisplay(tiltSlider,tiltDisplay,"°");
		
		geologyOptions = new ToggleGroup();
		earthLikeOption.setToggleGroup(geologyOptions);
		randomOption.setToggleGroup(geologyOptions);
		customOption.setToggleGroup(geologyOptions);
		earthLikeOption.onActionProperty().setValue((ActionEvent ae)->{
			setSlidersToEarth();
			enableSliders(false);
		});
		randomOption.onActionProperty().setValue((ActionEvent ae)->{
			setSlidersToRandom(getSeedString());
			enableSliders(false);
		});
		customOption.onActionProperty().setValue((ActionEvent ae)->{
			enableSliders(true);
		});
		earthLikeOption.setSelected(true);
		enableSliders(false);
		
		nextButton.onActionProperty().setValue((ActionEvent ae)->{nextScreen(ae);});
		
		randomNameButton.onActionProperty().setValue((ActionEvent ae)->{nameField.setText(generateRandomName());});
		
		nameField.textProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue observable, String oldValue, String String) {
				if(randomOption.isSelected()){
					setSlidersToRandom(getSeedString());
				}
			}
		});
		
		nf.setMaximumFractionDigits(2);
		
	}
	
	private final java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance();
	private void setLabelDisplay(Slider slider, Label label, String unit){
		slider.valueProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue observable, Number oldValue, Number newValue) {
				label.setText(nf.format(newValue.doubleValue())+unit);
			}
		});
		label.setText(slider.valueProperty().getValue().toString()+unit);
	}
	
	private String getSeedString(){
		return App.cleanSeedString(nameField.getText());
	}
	
	private final double EARTH_RADIUS = 6370;
	private final double EARTH_ATMOSPHERE = 101;
	private final double EARTH_OCEAN = 0.7;
	private final double EARTH_SUN = 1367;
	private final double EARTH_TILT = 23.4;
	
	private void setSlidersToEarth(){
		radiusSlider.valueProperty().setValue(EARTH_RADIUS);
		atmSlider.valueProperty().setValue(EARTH_ATMOSPHERE);
		oceanSlider.valueProperty().setValue(EARTH_OCEAN);
		sunSlider.valueProperty().setValue(EARTH_SUN);
		tiltSlider.valueProperty().setValue(EARTH_TILT);
	}
	
	private void setSlidersToRandom(String seed){
		long seedVal = AbstractPlanet.stringHashCode(seed);
		Random prng = new Random(seedVal);
		radiusSlider.valueProperty().setValue(Math.max(radiusSlider.getMin(),3*EARTH_RADIUS*randomSquared(prng)));
		atmSlider.valueProperty().setValue(Math.max(atmSlider.getMin(),3*EARTH_ATMOSPHERE*randomSquared(prng)));
		oceanSlider.valueProperty().setValue(Math.max(oceanSlider.getMin(),prng.nextDouble()));
		sunSlider.valueProperty().setValue(Math.max(sunSlider.getMin(),3*EARTH_SUN*randomSquared(prng)));
		tiltSlider.valueProperty().setValue(Math.max(tiltSlider.getMin(),90*randomSquared(prng)));
	}
	
	private double randomSquared(Random prng){
		double d = prng.nextDouble();
		return d*d;
	}
	
	/** Used for callbacks to switch screens */
	void setMaster(App master) {
		masterController = master;
	}
	
	@FXML private void nextScreen(ActionEvent ae){
		PlanetConfig pc = new PlanetConfig(nameField.getText(),getSeedString(),
				radiusSlider.getValue(),atmSlider.getValue(),oceanSlider.getValue(),
				sunSlider.getValue(),tiltSlider.getValue(),getPlanetType(),masterController.getVersionString());
		rootPane.setDisable(true);
		masterController.startScreenDone(pc);
		rootPane.setDisable(false);
	}
	
	private int getPlanetType(){
		if(earthLikeOption.isSelected()) return 0;
		if(randomOption.isSelected()) return 1;
		if(customOption.isSelected()) return 2;
		return 2;
	}

	private void enableSliders(boolean b) {
		optionsPane.setDisable(!b);
	}

	
	private Random nrand = null;
	String[] consonants = {"b","c","d","f","g","h","j","k","l","m","n","p","qu","r","s","t","v","w","x","z","sh","th"};
	String[] vowels = {"a","e","i","o","u","y","a","e","i","o","u"};
	String[] suffixes = {" I"," II"," III"," IV"," V"," prime"," minor"," major"," planet"," world"," land"};
	private String generateRandomName() {
		if(nrand == null)nrand = new Random(System.currentTimeMillis());
		int numSyl = 1 + nrand.nextInt(5);
		
		double consonantStartProbability = 0.7;
		double extraConsonantProbability = 0.25;
		double suffixProbability = 0.25;
		boolean consEnd = false;
		StringBuilder buffer = new StringBuilder();
		
		for(int i = 0; i < numSyl; i++){
			if(nrand.nextDouble() < consonantStartProbability){
				// consonant-vowel
				buffer.append(consonants[nrand.nextInt(consonants.length)]);
				buffer.append(vowels[nrand.nextInt(vowels.length)]);
				consEnd = true;
			} else {
				// vowel-consonant
				if(consEnd){
					buffer.append("'");
				}
				buffer.append(vowels[nrand.nextInt(vowels.length)]);
				buffer.append(consonants[nrand.nextInt(consonants.length)]);
				consEnd = false;
			}
		}
		if(consEnd && nrand.nextDouble() < extraConsonantProbability){
			buffer.append(consonants[nrand.nextInt(consonants.length)]);
		}
		
		if(nrand.nextDouble() < suffixProbability)buffer.append(suffixes[nrand.nextInt(suffixes.length)]);
		
		buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));
		String name = buffer.toString();
		return name;
	}
}
