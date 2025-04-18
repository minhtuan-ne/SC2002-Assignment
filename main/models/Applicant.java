package main.models;

public class Applicant extends User {
    public Applicant(String nric, String name, int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
    }

    @Override
    public String getRole() { return "Applicant"; }
    private String flatType = "-";

    public void setFlatType(String flatType) { this.flatType = flatType; }
    public String getFlatType()              { return flatType; }
}
