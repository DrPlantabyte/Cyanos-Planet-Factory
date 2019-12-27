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

package hall.collin.christopher.cpf.graphics;

import hall.collin.christopher.cpf.math.SphereLUT;
import hall.collin.christopher.worldgeneration.math.SpherePoint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author CCHall
 */
public class GlobeAnimator {
	/** if false, animation is paused */
	public final BooleanProperty run = new SimpleBooleanProperty(true);
	/** if false, animation thread terminates */
	private final AtomicBoolean live = new AtomicBoolean(true);
	/** current frame */
	public final IntegerProperty frameIndex = new SimpleIntegerProperty(0);
	/** desired rotation speed */
	public final FloatProperty secondsPerRotation = new SimpleFloatProperty(10);
	
	private BufferedImage texture = null;
	
	private final ImageView target;
	private final int size;
	private final int frameBufferSize = 144;
	private final double tilt; // in radians
	
	private Thread animationThread;
	
	
	final List<WritableImage> frameBuffer = new ArrayList<>(frameBufferSize);
	
	public GlobeAnimator(ImageView target, int size){
		this.target = target;
		this.size = size;
		try{texture = ImageIO.read(getClass().getResource("DefaultTexture.jpg"));}
		catch(IOException ex){Logger.getLogger(getClass().getName()).log(Level.SEVERE,"Failed to load default image texture", ex);}
		tilt = 23.4/180*Math.PI;
		animationThread = new Thread(new Animator());
		animationThread.setName(getClass()+".Animator");
		animationThread.setDaemon(true);
		animationThread.start();
	}
	/**
	 * 
	 * @param target
	 * @param size
	 * @param map
	 * @param tilt globe axis tilt, in degrees
	 */
	public GlobeAnimator(ImageView target, int size, BufferedImage map, double tilt){
		this.target = target;
		this.size = size;
		this.tilt = tilt/180*Math.PI; // degrees to radians
		texture = map;
		
		animationThread = new Thread(new Animator());
		animationThread.setName(getClass()+".Animator");
		animationThread.setDaemon(true);
		animationThread.start();
	}

	public void terminate() {
		live.set(false);
		animationThread.interrupt();
		try {
			animationThread.join();
		} catch (InterruptedException ex) {
			// do nothing, the animation thread will die and is Daemon anyway
			Logger.getLogger(GlobeAnimator.class.getName()).log(Level.INFO, "Thread interrpution while waiting for animation thread to terminate.", ex);
		}
		frameBuffer.clear();
	}
	
	private class Animator implements Runnable{

		@Override
		public void run() {
			int index = 0;
			final SphereLUT lut = new SphereLUT(size,size,tilt);
			final int frameBufferSize = 144; // 2.5 degrees rotation per frame
			while(live.get()){
				index = index % frameBufferSize;
				long computationTime = 0;
				long msPerFrame = (long)(1000.0 * secondsPerRotation.doubleValue() / frameBufferSize);
				if(index >= frameBuffer.size()){
					long t0 = System.currentTimeMillis();
					double rotation = (-360.0 / frameBufferSize) * index * (Math.PI/180.0);
					if(rotation >= (Math.PI * 2)){
						rotation -= (Math.PI * 2);
					}
					WritableImage globe = new WritableImage(size,size);
					PixelWriter pw = globe.getPixelWriter();
					for(int y = 0; y < size; y++){
						for(int x = 0; x < size; x++){
							SpherePoint p = lut.getCoordinateAt(x, y);
							if(p == null) continue;
							int u = (int)((p.getLongitude() + rotation) / (2 * Math.PI) * texture.getWidth()) % texture.getWidth();
							int v = (int)((p.getLatitude() + (0.5 * Math.PI)) / Math.PI * texture.getHeight()) % texture.getHeight();
							if(u < 0 ) u += texture.getWidth();
							pw.setArgb(x, y, texture.getRGB(u, v));
						}
					}
					frameBuffer.add(globe);
					computationTime = System.currentTimeMillis() - t0;
					if(computationTime > msPerFrame) computationTime = msPerFrame;
				}
				final int findex = index;
				javafx.application.Platform.runLater(()->{if(live.get())target.setImage(frameBuffer.get(findex));});
				try {
					Thread.sleep(msPerFrame - computationTime);
				} catch (InterruptedException ex) {
					return;
				}
				index++;
			}
			Logger.getLogger(getClass().getName()).log(Level.FINE,"Terminating animation thread");
		}
		
	}
}
