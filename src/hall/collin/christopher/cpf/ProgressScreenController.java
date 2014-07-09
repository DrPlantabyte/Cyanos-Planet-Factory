
package hall.collin.christopher.cpf;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author CCHall
 */
public class ProgressScreenController  implements Initializable {
	private  Thread task = null;
	private  Stage dialogWindow = null;
	private  Thread updateThread = null;
	private volatile boolean updating = true;
	private  DoubleAdder progressTracker = null;
	private  double maxProgress = Double.NaN;
	private  String name = null;
	
	private volatile boolean canceled = false;
	
	@FXML private Label taskNameLabel;
	@FXML private ProgressBar progressBar;
	@FXML private Button cancelButton;
	
	public ProgressScreenController(){
		//
	}
	
	private void postInit(Thread t, Stage window, DoubleAdder pTracker, double max, String taskName){
		name = taskName;
		maxProgress = max;
		progressTracker = pTracker;
		task = t;
		dialogWindow = window;
		updateThread = new Thread(()->{
			while(updating & task.isAlive()){
				javafx.application.Platform.runLater(()->{
					progressBar.setProgress(progressTracker.doubleValue() / maxProgress);
				});
				try{
					Thread.sleep(1000);
				}catch(InterruptedException ex){
					// terminate request
					break;
				}
			}
			javafx.application.Platform.runLater(()->{dialogWindow.close();});
		});
		updateThread.setDaemon(true);
		updateThread.setName(ProgressScreenController.class.getSimpleName()+" Update Thread");
		
		taskNameLabel.setText(name);
		updateThread.start();
	}
	
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		cancelButton.onActionProperty().setValue((ActionEvent ae)->{cancel();});
	}
	
	
	@FXML public void cancel(){
		canceled = true;
		task.interrupt();
		try {
			task.join(1000);
		} catch (InterruptedException ex) {
			Logger.getLogger(ProgressScreenController.class.getName()).log(Level.INFO, "Interruption while waiting for task to terminate", ex);
		}
		// DANGEROUS
		if(task.isAlive()){
			Logger.getLogger(ProgressScreenController.class.getName()).log(Level.SEVERE, 
					"Thread "+task.getName()+" failed to terminate after interruption within aloted time!"
							+ " Forcing thread to stop!");
			task.stop();
		}
		dialogWindow.close();
	}
	
	protected boolean wasSuccessful(){
		return (!canceled) && (!task.isAlive());
	}
	
	public static boolean waitOnTask(Runnable task, String taskName, DoubleAdder progressTracker, double progressMax,Stage mainStage){
		Thread taskThread = new Thread(task);
		taskThread.setDaemon(true);
		taskThread.setName("Task Thread");
		FXMLLoader fxmlLoader = new FXMLLoader(ProgressScreenController.class.getResource("fxml/ProcessingScreen.fxml"));
		Parent fxmlroot = null;
		try {
			fxmlroot = (Parent)fxmlLoader.load();
		} catch (IOException ex) {
			Logger.getLogger(ProgressScreenController.class.getName()).log(Level.SEVERE, "Failed to load and initialize FXML", ex);
			return false;
		}
		final Stage dialog = new Stage(StageStyle.UNDECORATED); // check back if transparent was a good or bad idea
		ProgressScreenController controller = fxmlLoader.getController();
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(mainStage);
		dialog.setTitle("Processing...");
		dialog.setScene(new Scene(fxmlroot));
		taskThread.start();
		controller.postInit(taskThread,dialog,progressTracker,progressMax,taskName);
		dialog.toFront();
		dialog.showAndWait();
		return controller.wasSuccessful();
	}
}
