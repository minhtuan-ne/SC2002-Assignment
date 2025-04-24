package main.models;

public class Application {
    private final Applicant applicant;
    private final String projectName;
    private final String flatType;
    private String status; // Pending, Successful, Unsuccessful, Booked

    public Application(Applicant applicant, String projectName, String flatType, String status) {
        this.applicant = applicant;
        this.projectName = projectName;
        this.flatType = flatType;
        this.status = status;
    }

    public Application(Applicant applicant, String projectName, String flatType) {
        this(applicant, projectName, flatType, "Pending");
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
