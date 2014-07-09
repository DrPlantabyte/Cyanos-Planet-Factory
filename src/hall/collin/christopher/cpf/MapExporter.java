
package hall.collin.christopher.cpf;

import hall.collin.christopher.worldgeneration.graphics.PlanetPainter;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.DoubleAdder;

/**
 *
 * @author CCHall
 */
public interface MapExporter {
	public abstract double maxProgress();
	public abstract BufferedImage generateMap(int size, PainterOption painterType, boolean landShading, boolean oceanShading, boolean drawLabels, DoubleAdder progressTracker);
}
