package main.models;

/**
 * Represents a HDB Officer in the BTO system.
 * An officer can handle one approved project and also apply like an applicant.
 */
public class HDBOfficer extends Applicant {

	/**
	 * The registration status of a BTO application.
	 */
	public enum RegistrationStatus {
	    /** No application has been made. */
	    NONE,
	    /** Application has been submitted and is pending approval. */
	    PENDING,
	    /** Application has been reviewed and approved. */
	    APPROVED
	}

    private String handlingProjectId;
    private RegistrationStatus regStatus;

    /**
     * Constructs a new HDB Officer.
     *
     * @param nric          NRIC of the officer
     * @param name          Name of the officer
     * @param age           Age of the officer
     * @param maritalStatus Marital status of the officer
     * @param password      Password for login
     */
    public HDBOfficer(String nric, String name,
                      int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
        this.regStatus = RegistrationStatus.NONE;
        this.handlingProjectId = null;
    }

    /**
     * Checks if the officer's registration is pending.
     *
     * @return true if status is PENDING
     */
    public boolean isRegistrationPending() {
        return regStatus == RegistrationStatus.PENDING;
    }

    /**
     * Checks if the officer is approved and handling a project.
     *
     * @return true if status is APPROVED
     */
    public boolean isHandlingProject() {
        return regStatus == RegistrationStatus.APPROVED;
    }

    /**
     * Returns the ID of the project the officer is handling.
     *
     * @return project ID, or null if none
     */
    public String getHandlingProjectId() {
        return handlingProjectId;
    }

    /**
     * Returns the registration status of this officer.
     *
     * @return registration status
     */
    public RegistrationStatus getRegStatus() {
        return regStatus;
    }

    /**
     * Updates the registration status of this officer.
     *
     * @param status new registration status
     */
    public void setRegStatus(RegistrationStatus status) {
        this.regStatus = status;
    }

    /**
     * Sets the ID of the project this officer is handling.
     *
     * @param id project ID
     */
    public void setHandlingProjectId(String id) {
        this.handlingProjectId = id;
    }

    /**
     * Returns the role of this user.
     *
     * @return always returns "HDBOfficer"
     */
    @Override
    public String getRole() {
        return "HDBOfficer";
    }

    /**
     * Returns a string representation of the officer,
     * including registration status and project handling information.
     *
     * @return officer description
     */
    @Override
    public String toString() {
        String s =
                regStatus == RegistrationStatus.NONE ? "None"
                        : regStatus == RegistrationStatus.PENDING ? "PENDING‑" + handlingProjectId
                        : handlingProjectId;
        return super.toString() + " | Handling: " + s;
    }

    /**
     * Converts this officer's data to a CSV-style tab-separated row.
     *
     * @return CSV-formatted string
     */
    public String toCSVRow() {
        return String.join("\t", getName(), getNRIC(), String.valueOf(getAge()),
                getMaritalStatus(), getPassword(), regStatus.name(),
                handlingProjectId == null ? "null" : handlingProjectId);
    }
}
