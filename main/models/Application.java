package main.models;

public class Application {
    private final Applicant applicant;
    private final String projectName;
    private final String flatType;
    private String status; // Pending, Withdrawing, Withdrawn, Successful, Unsuccessful, Booked
    private String prevStatus; // Only use when requesting withdrawal

    public Application(Applicant applicant, String projectName, String flatType, String status, String prevStatus) {
        this.applicant = applicant;
        this.projectName = projectName;
        this.flatType = flatType;
        this.status = status;
        this.prevStatus = prevStatus;
    }

    public Application(Applicant applicant, String projectName, String flatType) {
        this(applicant, projectName, flatType, "Pending", "null");
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

    public String getPrevStatus(){
        return prevStatus;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPrevStatus(String status){
        this.prevStatus = status;
    }
}
