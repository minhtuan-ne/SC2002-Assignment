package main.models;

public class Application {
    private Applicant applicant;
    private String projectName;
    private String flatType;
    private String status; // Pending, Successful, Unsuccessful, Booked

    public Application(Applicant applicant, String projectName, String flatType) {
        this.applicant = applicant;
        this.projectName = projectName;
        this.flatType = flatType;
        this.status = "Pending";
    }

    public Applicant getApplicant() {
        return applicant;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getFlatType() {
        return flatType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
