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

import hall.collin.christopher.worldgeneration.biomes.StandardBiomeFactory;
import hall.collin.christopher.worldgeneration.graphics.PlanetPainter;
import hall.collin.christopher.worldgeneration.graphics.VegetationPainter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.imageio.ImageIO;

/**
 *
 * @author CCHall
 */
public class ExportScreenController  implements Initializable{

	@FXML private Label nameLabel;
	@FXML private Button browseButton;
	@FXML private Button saveButton;
	@FXML private TextField filepathField;
	@FXML private TextField sizeField;
	@FXML private RadioButton radioButton_vegetation;
	@FXML private RadioButton radioButton_biome;
	@FXML private CheckBox landShadingCheckbox;
	@FXML private CheckBox oceanShadingCheckbox;
	@FXML private CheckBox labelsCheckbox;
	
	private final ToggleGroup painterGroup = new ToggleGroup();
	private MapExporter mapMaker = null;
	private Path saveDir  = null;
	
	
	private FileChooser fc = new FileChooser();
	
	private Stage stage = null;
	
	protected void configure(String worldName, int size, PainterOption painter, 
			boolean landShader, boolean oceanShader, boolean drawLabels, 
			Path saveDir, MapExporter mapMaker, Stage thisStage){
		stage = thisStage;
		nameLabel.setText(worldName);
		switch(painter){
			case BIOME:
				radioButton_biome.setSelected(true);
				break;
			case VEGETATION:
			default:
				radioButton_vegetation.setSelected(true);
				break;
		}
		sizeField.setText(Integer.toString(size));
		landShadingCheckbox.setSelected(landShader);
		oceanShadingCheckbox.setSelected(oceanShader);
		labelsCheckbox.setSelected(drawLabels);
		filepathField.setText(saveDir.toString()+File.separator+worldName+".png");
		this.mapMaker = mapMaker;
		this.saveDir = saveDir;
		fc.setInitialDirectory(saveDir.toFile());
	}
	
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		radioButton_vegetation.setToggleGroup(painterGroup);
		radioButton_biome.setToggleGroup(painterGroup);
		saveButton.onActionProperty().setValue((ActionEvent ae)->{export();});
		
		browseButton.onActionProperty().setValue((ActionEvent ae)->{
			File save = fc.showSaveDialog(App.getAppStage().getOwner());
			if(save != null){
				filepathField.setText(save.toPath().toString());
			}
		});
	}
	
	private void export(){
		Path filepath = (new File(filepathField.getText())).toPath();
		if(Files.exists(filepath)){
			// ask user if sure
			if(!sure("File '"+filepath+"' already exists. Overwrite it?"))return;
		}
		saveDir = filepath.getParent();
		PainterOption pp;
		if(radioButton_biome.isSelected()){
			pp = PainterOption.BIOME;
		} else {
			pp = PainterOption.VEGETATION;
		}
		final double progressGoal = mapMaker.maxProgress();
		final DoubleAdder pTracker = new DoubleAdder();
		final Exception[] thrownException = new Exception[1];
		stage.getScene().getRoot().setDisable(true);
		boolean notCancelled = ProgressScreenController.waitOnTask(
				()->{
				try {
					if(Files.exists(filepath) && Files.isWritable(filepath) == false){
						throw new IOException("Cannot write to file '"+filepath.toString()+"'. User may not have writable access permission");
					}
					ImageIO.write(mapMaker.generateMap(Integer.parseInt(sizeField.getText()), pp, landShadingCheckbox.isSelected(), oceanShadingCheckbox.isSelected(), labelsCheckbox.isSelected(), pTracker),"png",filepath.toFile());
				} catch (IOException  ex) {
					Logger.getLogger(ExportScreenController.class.getName()).log(Level.SEVERE, "Failed to write image to file.", ex);
					thrownException[0] = ex;
				}
			}
				, "Generating detailed map...", pTracker, progressGoal, App.getAppStage());
		if(thrownException[0] != null){
			App.showErrorMessage("Failed to write image to file '"+filepath.toString()+"'", thrownException[0]);
		}
		stage.getScene().getRoot().setDisable(false);
		// done
		if(notCancelled){
			stage.close();
		}
	}
	
	public static Path exportMap(String worldName, int size, PainterOption painter, 
			boolean landShader, boolean oceanShader, boolean drawLabels, 
			Path saveDir, MapExporter mapMaker){
		FXMLLoader fxmlLoader = new FXMLLoader(ProgressScreenController.class.getResource("fxml/ExportScreen.fxml"));
		Parent fxmlroot = null;
		try {
			fxmlroot = (Parent)fxmlLoader.load();
		} catch (IOException ex) {
			Logger.getLogger(ProgressScreenController.class.getName()).log(Level.SEVERE, "Failed to load and initialize FXML", ex);
			return saveDir;
		}
		final Stage dialog = new Stage(StageStyle.DECORATED); // check back if transparent was a good or bad idea
		ExportScreenController controller = fxmlLoader.getController();
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.initOwner(App.getAppStage());
		dialog.setTitle("Export Map");
		dialog.setScene(new Scene(fxmlroot));
		controller.configure( worldName,  size,  painter, 
			 landShader,  oceanShader,  drawLabels, 
			 saveDir,  mapMaker, dialog);
		dialog.showAndWait();
		return controller.saveDir;
	}

	private boolean sure(String message) {
		final AtomicBoolean isSure = new AtomicBoolean(false);
		final Stage dialog = new Stage(StageStyle.DECORATED); // check back if transparent was a good or bad idea
		
		VBox l1 = new VBox();
		Label text = new Label(message);
		l1.getChildren().add(text);
		HBox l2 = new HBox();
		l1.getChildren().add(l2);
		Button yesButton = new Button("Yes");
		yesButton.onActionProperty().setValue((ActionEvent ae)->{isSure.set(true);dialog.close();});
		Button noButton = new Button("No");
		noButton.onActionProperty().setValue((ActionEvent ae)->{isSure.set(false);dialog.close();});
		l2.getChildren().add(yesButton);
		l2.getChildren().add(noButton);
		
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.initOwner(App.getAppStage());
		dialog.setTitle("Are you sure?");
		dialog.setScene(new Scene(l1));
		dialog.showAndWait();
		return isSure.get();
	}
	
}

