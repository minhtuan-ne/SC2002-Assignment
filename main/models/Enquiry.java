package main.models;

/**
 * Represents a user enquiry about a BTO project.
 * Tracks the enquiry ID, the applicant’s NRIC, the target project,
 * the enquiry message, and an optional reply.
 */
public class Enquiry {
    private String enquiryId;
    private String userNRIC;
    private String projectName;
    private String message;
    private String reply;

    /**
     * Constructs a new Enquiry.
     *
     * @param enquiryId   unique identifier for this enquiry
     * @param userNric    NRIC of the user who submitted the enquiry
     * @param projectName name of the BTO project being enquired about
     * @param message     the enquiry text
     */
    public Enquiry(String enquiryId, String userNric, String projectName, String message) {
        this.enquiryId = enquiryId;
        this.userNRIC = userNric;
        this.projectName = projectName;
        this.message = message;
        this.reply = null;
    }

    /**
     * Gets this enquiry’s unique ID.
     *
     * @return enquiryId
     */
    public String getEnquiryId() { return enquiryId; }

    /**
     * Gets the NRIC of the user who made the enquiry.
     *
     * @return userNRIC
     */
    public String getUserNric() { return userNRIC; }

    /**
     * Gets the name of the project this enquiry is about.
     *
     * @return projectName
     */
    public String getProjectName() { return projectName; }

    /**
     * Gets the enquiry message text.
     *
     * @return message
     */
    public String getMessage() { return message; }

    /**
     * Updates the enquiry message.
     *
     * @param message new enquiry text
     */
    public void setMessage(String message) { this.message = message; }

    /**
     * Gets the manager’s reply to this enquiry, if any.
     *
     * @return reply (or null if none)
     */
    public String getReply() { return reply; }

    /**
     * Sets or updates the manager’s reply.
     *
     * @param reply the reply text
     */
    public void setReply(String reply) { this.reply = reply; }
}