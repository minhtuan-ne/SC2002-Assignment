package main.services;

import main.models.*;

public class HDBOfficerService {
    private final ApplicantService applicantSvc;
    private final ProjectService projectSvc;

    public HDBOfficerService(ProjectService projectSvc, ApplicantService  applicantSvc) { 
        this.projectSvc = projectSvc;
        this.applicantSvc  = applicantSvc;              
    }

    public String getRegistrationStatus(HDBOfficer o){ return o.getRegStatus().name(); }
    public BTOProject viewHandledProject(HDBOfficer o){
        return o.isHandlingProject() ? projectSvc.getProjectByName(o.getHandlingProjectId()) : null;
    }

    public boolean bookFlat(String applicantNric, String flatType) {

        Application app = applicantSvc.getApplication(applicantNric);
        if (app == null || !"Successful".equalsIgnoreCase(app.getStatus())) {
            System.out.println("Booking rejected – application not in Successful state.");
            return false;
        }

        BTOProject proj = projectSvc.getProjectByName(app.getProjectName());
        if (proj == null) { System.out.println("Project not found."); return false; }

        if (!proj.decrementFlatCount(flatType)) {
            System.out.println("No remaining units of " + flatType);
            return false;
        }

        // Update status & applicant profile
        app.setStatus("Booked");
        Applicant a = app.getApplicant();
        a.setFlatType(flatType);

        System.out.println("Flat booked successfully.");
        generateReceipt(applicantNric);
        return true;
    }

    public void generateReceipt(String applicantNric) {
        Application app = applicantSvc.getApplication(applicantNric);
        if (app == null || !"Booked".equalsIgnoreCase(app.getStatus())) {
            System.out.println("No booked application found to generate receipt.");
            return;
        }

        Applicant  a = app.getApplicant();
        BTOProject p = projectSvc.getProjectByName(app.getProjectName());

        System.out.println("\n======= BOOKING RECEIPT =======");
        System.out.printf("Name / NRIC : %s / %s%n", a.getName(), a.getNRIC());
        System.out.printf("Age / Status: %d / %s%n", a.getAge(), a.getMaritalStatus());
        System.out.printf("Project     : %s (%s)%n", p.getProjectName(), p.getNeighborhood());
        System.out.printf("Flat Type   : %s%n", a.getFlatType());
        System.out.println("================================\n");
    }
}



