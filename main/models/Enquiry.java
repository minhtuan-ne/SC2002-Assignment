package models;

public class Enquiry {
    private String enquiryId;
    private String userNric;
    private String projectName;
    private String message;

    public Enquiry(String enquiryId, String userNric, String projectName, String message) {
        this.enquiryId = enquiryId;
        this.userNric = userNric;
        this.projectName = projectName;
        this.message = message;
    }

    public String getEnquiryId() { return enquiryId; }
    public String getUserNric() { return userNric; }
    public String getProjectName() { return projectName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
