package main.models;

/**
 * Represents a 3-room flat type in the BTO system.
 */
public class ThreeRoom extends Flat {
    /**
     * Constructs a 3-room flat with given unit count and price.
     *
     * @param units number of available units
     * @param price price per unit
     */
    public ThreeRoom(int units, int price) {
        super("3-room", units, price);
    }
}
