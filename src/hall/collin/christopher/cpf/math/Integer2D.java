
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
