package main.services;

import main.models.*;

/**
 * Service class that handles HDB Officer operations such as viewing their assigned project,
 * booking flats for applicants, generating receipts, and checking registration status.
 * 
 */
public class HDBOfficerService {
    private final ApplicantService applicantSvc;
    private final ProjectService projectSvc;

    /**
     * Constructs a new HDBOfficerService with dependencies injected.
     *
     * @param projectSvc the project service used for retrieving project details
     * @param applicantSvc the applicant service used to retrieve and update application data
     */
    public HDBOfficerService(ProjectService projectSvc, ApplicantService applicantSvc) {
        this.projectSvc = projectSvc;
        this.applicantSvc = applicantSvc;
    }

    /**
     * Retrieves the registration status of the given HDB Officer.
     *
     * @param o the HDB Officer
     * @return registration status as a string
     */
    public String getRegistrationStatus(HDBOfficer o) {
        return o.getRegStatus().name();
    }

    /**
     * Returns the BTO project currently being handled by the officer.
     *
     * @param o the HDB Officer
     * @return the BTOProject if handling one; otherwise null
     */
    public BTOProject viewHandledProject(HDBOfficer o) {
        return o.isHandlingProject() ? projectSvc.getProjectByName(o.getHandlingProjectId()) : null;
    }

    /**
     * Books a flat of the specified type for an applicant if the application is in "Successful" state.
     *
     * @param applicantNric NRIC of the applicant
     * @param flatType the type of flat to book (e.g., "2-room" or "3-room")
     * @return true if the booking is successful, false otherwise
     */
    public boolean bookFlat(String applicantNric, String flatType) {
        Application app = applicantSvc.getApplication(applicantNric);
        if (app == null || !"Successful".equalsIgnoreCase(app.getStatus())) {
            System.out.println("Booking rejected – application not in Successful state.");
            return false;
        }

        BTOProject proj = projectSvc.getProjectByName(app.getProjectName());
        if (proj == null) {
            System.out.println("Project not found.");
            return false;
        }

        if (!decrementFlatCount(proj, flatType)) {
            System.out.println("No remaining units of " + flatType);
            return false;
        }

        app.setStatus("Booked");
        Applicant a = app.getApplicant();
        a.setFlatType(flatType);

        System.out.println("Flat booked successfully.");
        generateReceipt(applicantNric);
        return true;
    }

    /**
     * Generates a booking receipt for the specified applicant.
     *
     * @param applicantNric NRIC of the applicant
     */
    public void generateReceipt(String applicantNric) {
        Application app = applicantSvc.getApplication(applicantNric);
        if (app == null || !"Booked".equalsIgnoreCase(app.getStatus())) {
            System.out.println("No booked application found to generate receipt.");
            return;
        }

        Applicant a = app.getApplicant();
        BTOProject p = projectSvc.getProjectByName(app.getProjectName());

        System.out.println("\n======= BOOKING RECEIPT =======");
        System.out.printf("Name / NRIC : %s / %s%n", a.getName(), a.getNRIC());
        System.out.printf("Age / Status: %d / %s%n", a.getAge(), a.getMaritalStatus());
        System.out.printf("Project     : %s (%s)%n", p.getProjectName(), p.getNeighborhood());
        System.out.printf("Flat Type   : %s%n", a.getFlatType());
        System.out.println("================================\n");
    }

    /**
     * Decrements the flat unit count for a specific flat type in a project.
     *
     * @param project the project containing the units
     * @param type the type of flat to decrement
     * @return true if successful; false if no units are available
     */
    private boolean decrementFlatCount(BTOProject project, String type) {
        int remain = project.getUnits(type);
        if (remain <= 0) return false;
        project.setUnits(type, remain - 1);
        return true;
    }
}




