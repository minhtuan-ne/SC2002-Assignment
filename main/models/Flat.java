package main.models;

/**
 * Abstract base for a flat type in a BTO project.
 * Captures common properties: flat type name, available units, and unit price.
 */
public abstract class Flat {
    protected String type;
    protected int units;
    protected int price;

    /**
     * Constructs a Flat with the given parameters.
     *
     * @param type  flat type identifier (e.g. "2-room", "3-room")
     * @param units initial number of units available
     * @param price price per unit
     */
    public Flat(String type, int units, int price) {
        this.type = type;
        this.units = units;
        this.price = price;
    }

    /**
     * Gets the flat type name.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the number of units currently available.
     *
     * @return units
     */
    public int getUnits() {
        return units;
    }

    /**
     * Updates the number of units available.
     *
     * @param units new available unit count
     */
    public void setUnits(int units) {
        this.units = units;
    }

    /**
     * Gets the price per unit of this flat type.
     *
     * @return price
     */
    public int getPrice() {
        return price;
    }

    /**
     * Updates the price per unit.
     *
     * @param price new price value
     */
    public void setPrice(int price) {
        this.price = price;
    }
}
