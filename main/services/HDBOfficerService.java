package main.services;

import main.models.HDBOfficer;

public class HDBOfficerService {

    public void assignToProject(HDBOfficer officer, String projectId) {
        if (officer.isHandlingProject()) {
            System.out.println("Officer already handling a project.");
        } else {
            officer.assignToProject(projectId);
            System.out.println("Officer assigned to project: " + projectId);
        }
    }

    public void removeFromProject(HDBOfficer officer) {
        if (!officer.isHandlingProject()) {
            System.out.println("Officer is not currently handling a project.");
        } else {
            String oldProject = officer.getHandlingProjectId();
            officer.removeFromProject();
            System.out.println("Officer removed from project: " + oldProject);
        }
    }

    // Placeholder methods for expansion
    public void bookFlat(String applicantNric, String flatType) {
        System.out.println("Flat of type " + flatType + " booked for applicant " + applicantNric);
        // Eventually link to Project/Applicant/Flat service
    }

    public void replyToEnquiry(String enquiryId, String message) {
        System.out.println("Replied to enquiry " + enquiryId + ": " + message);
        // Eventually update enquiry records
    }
}
