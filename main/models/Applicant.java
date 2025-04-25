// src/main/models/Applicant.java
package main.models;

/**
 * Represents an applicant in the BTO system.
 * Extends User to track the selected flat type.
 */
public class Applicant extends User {
    private String flatType = "-"; // Selected flat type or none

    /**
     * Constructs an Applicant with given personal details.
     * @param nric          the applicant's NRIC
     * @param name          the applicant's name
     * @param age           the applicant's age
     * @param maritalStatus the applicant's marital status
     * @param password      initial password
     */
    public Applicant(String nric, String name, int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
    }

    /**
     * Returns the role of this user.
     * @return always "Applicant"
     */
    @Override
    public String getRole() {
        return "Applicant";
    }

    /**
     * Sets the applicant's chosen flat type.
     * @param flatType type of flat selected (e.g., "2-room", "3-room")
     */
    public void setFlatType(String flatType) {
        this.flatType = flatType;
    }

    /**
     * Returns the flat type selected by the applicant.
     * @return selected flat type
     */
    public String getFlatType() {
        return flatType;
    }
}
