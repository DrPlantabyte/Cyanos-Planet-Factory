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

package hall.collin.christopher.cpf.math;

/**
 * This class is a 2D coordinate using integers. It is used for passing 
 * screen/image coordinates to various methods.
 * @author CCHall
 */
public final class Integer2D {
	public final int x;
	public final int y;
	/**
	 * Constructs a 2D integer object.
	 * @param X first dimension value
	 * @param Y second dimension value
	 */
	public Integer2D(int X, int Y){
		x = X;
		y = Y;
	}
	/**
	 * Returns the first dimension (x-axis) coordinate value.
	 * @return  Returns the first dimension (x-axis) coordinate value.
	 */
	public final int getX(){
		return x;
	}
	/**
	 * Returns the second dimension (y-axis) coordinate value.
	 * @return  Returns the second dimension (y-axis) coordinate value.
	 */
	public final int getY(){
		return y;
	}
	/**
	 * Override of Object.equals().
	 * @param o object to test equivalency
	 * @return 
	 */
	@Override public boolean equals(Object o){
		if(o instanceof Integer2D){
			return this.x == ((Integer2D)o).x && this.y == ((Integer2D)o).y;
		} else {
			return false;
		}
	}
	/**
	 * Override of Object.hashCode().
	 * @return The hash code.
	 */
	@Override
	public int hashCode() {
		int hash = 1;
		hash = 373 * hash + this.x;
		hash = 317 * hash + this.y;
		return hash;
	}
}
