package main.models;

/**
 * Represents an application for a flat by an applicant.
 * Tracks the flat type, associated project, and application status.
 */
public class Application {
    private final Applicant applicant;
    private final String projectName;
    private final String flatType;
    private String status;       // Pending, Withdrawing, Withdrawn, Successful, etc.
    private String prevStatus;   // Used only during withdrawal

    /**
     * Constructs a full Application instance.
     *
     * @param applicant    the applicant
     * @param projectName  the project applied for
     * @param flatType     flat type applied (2-room, 3-room)
     * @param status       current application status
     * @param prevStatus   previous status (if withdrawing)
     */
    public Application(Applicant applicant, String projectName, String flatType, String status, String prevStatus) {
        this.applicant = applicant;
        this.projectName = projectName;
        this.flatType = flatType;
        this.status = status;
        this.prevStatus = prevStatus;
    }

    /**
     * Constructs a new Application with default \"Pending\" status.
     *
     * @param applicant   the applicant
     * @param projectName project name
     * @param flatType    flat type
     */
    public Application(Applicant applicant, String projectName, String flatType) {
        this(applicant, projectName, flatType, "Pending", "null");
    }

    /**
     * Returns the Applicant who submitted this application.
     *
     * @return the Applicant object
     */
    public Applicant getApplicant() {
        return applicant;
    }

    /**
     * Returns the name of the project applied for.
     *
     * @return project name as a String
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the flat type chosen.
     *
     * @return flat type (e.g., "2-Room")
     */
    public String getFlatType() {
        return flatType;
    }

    /**
     * Returns the current status of this application.
     *
     * @return current status (e.g., "PENDING", "APPROVED" or "REJECTED")
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the previous status before the current update.
     *
     * @return previous status, or null if none
     */
    public String getPrevStatus() {
        return prevStatus;
    }

    /**
     * Updates the current status of the application, preserving the old status.
     *
     * @param status the new status to set
     */
    public void setStatus(String status) {
        this.prevStatus = this.status;
    }

    /**
     * Sets the previous status explicitly (use with caution).
     *
     * @param status the status to store as previous
     */
    public void setPrevStatus(String status) {
    }
}
