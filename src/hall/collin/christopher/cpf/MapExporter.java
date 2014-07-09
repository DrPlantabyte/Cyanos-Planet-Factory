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
