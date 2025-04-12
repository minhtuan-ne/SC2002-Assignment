package main.models;

public class HDBOfficer extends Applicant {
    private String handlingProjectId; // Can only handle 1 project at a time

    public HDBOfficer(String nric, String name, int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
        this.handlingProjectId = null;
    }

    public void assignToProject(String projectId) {
        this.handlingProjectId = projectId;
    }

    public void removeFromProject() {
        this.handlingProjectId = null;
    }

    public boolean isHandlingProject() {
        return handlingProjectId != null;
    }

    public String getHandlingProjectId() {
        return handlingProjectId;
    }

    @Override
    public String getRole() {
        return "HDBOfficer";
    }

    @Override
    public String toString() {
        return super.toString() + (isHandlingProject() ? " | Handling Project: " + handlingProjectId : " | Not assigned to a project");
    }
}
