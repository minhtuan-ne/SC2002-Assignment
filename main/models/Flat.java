package main.models;

public abstract class Flat {
    protected String type;
    protected int units;
    protected int price;

    public Flat(String type, int units, int price) {
        this.type = type;
        this.units = units;
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }
    
    public int getPrice(){
        return price;
    }

    public void setPrice(int price){
        this.price = price;
    }
}