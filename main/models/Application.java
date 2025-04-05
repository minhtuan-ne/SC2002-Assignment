package main.models;

public class Application {
    private String applicantNric;
    private String projectName;
    private String flatType;
    private String status; // Pending, Successful, Unsuccessful, Booked

    public Application(String applicantNric, String projectName, String flatType) {
        this.applicantNric = applicantNric;
        this.projectName = projectName;
        this.flatType = flatType;
        this.status = "Pending";
    }

    public String getApplicantNric() { return applicantNric; }
    public String getProjectName() { return projectName; }
    public String getFlatType() { return flatType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
