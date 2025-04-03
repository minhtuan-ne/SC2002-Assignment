package models;

public class Applicant extends User {
    public Applicant(String nric, String password, int age, String maritalStatus) {
        super(nric, password, age, maritalStatus);
    }

    @Override
    public String getRole() { return "Applicant"; }
}
