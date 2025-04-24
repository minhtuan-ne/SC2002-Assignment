package main.models;

/**
 * Represents a 2-room flat type in the BTO system.
 */
public class TwoRoom extends Flat {
    /**
     * Constructs a 2-room flat with given unit count and price.
     *
     * @param units number of available units
     * @param price price per unit
     */
    public TwoRoom(int units, int price) {
        super("2-room", units, price);
    }
}
