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

    public Applicant getApplicant() { return applicant; }

    public String getProjectName() { return projectName; }

    public String getFlatType() { return flatType; }

    public String getStatus() { return status; }

    public String getPrevStatus() { return prevStatus; }

    public void setStatus(String status) { this.status = status; }

    public void setPrevStatus(String status) { this.prevStatus = status; }
}
