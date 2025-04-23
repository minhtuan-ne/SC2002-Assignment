package main.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import main.models.*;
import main.repositories.ProjectRepository;


/**
 * Concrete implementation that relies ONLY on:
 *   • ProjectRepository      (for project list & flat counts)
 *   • ApplicantService       (static apps list & passwords)
 *   • EnquiryService         (existing list + reply)
 *
 * No additional repos are introduced.
 */
public class HDBOfficerService {

    private final ProjectRepository projectRepo;
    private final EnquiryService    enquirySvc;   // concrete type gives us getAllEnquiries()
    private final ApplicantService applicantSvc;

    // constructor — add param & assign
    public HDBOfficerService(ProjectRepository projectRepo,
                             EnquiryService    enquirySvc,
                             ApplicantService  applicantSvc) {   // ← new param
        this.projectRepo   = projectRepo;
        this.enquirySvc    = enquirySvc;
        this.applicantSvc  = applicantSvc;                       // ← store
    }

    /* -------------------------------------------------------- */
    /*  1 – Registration workflow                               */
    /* -------------------------------------------------------- */

    public boolean registerToHandleProject(HDBOfficer officer, String projectId) {
        // Find the target project
        BTOProject project = findProject(projectId);
        if (project == null) {
            System.out.println("Project not found.");
            return false;
        }

        // If officer is handling a project, check if it's expired
        if (officer.isHandlingProject()) {
            BTOProject current = findProject(officer.getHandlingProjectId());

            if (current != null && new Date().after(current.getEndDate())) {
                // Auto-clear expired project
                officer.cancelRegistration();
                current.removeAssignedOfficer(officer.getNRIC());
                System.out.println("Previous project has ended. Status reset – you may now register.");
            } else {
                System.out.println("You are still handling an ongoing project. Cannot register.");
                return false;
            }
        }

        // Check if officer has applied for this project
        Application app = applicantSvc.getApplication(officer.getNRIC());
        if (app != null && app.getProjectName().equalsIgnoreCase(projectId)) {
            System.out.println("You have already applied for this project as an applicant – cannot register as officer.");
            return false;
        }

        // All checks passed – submit request
        officer.submitRegistration(projectId);
        project.addPendingRegistration(officer);
        System.out.println("Request submitted – awaiting manager approval.");
        return true;
    }

    public void cancelRegistration(HDBOfficer officer) {
        if (officer.getRegStatus() == HDBOfficer.RegistrationStatus.NONE) {
            System.out.println("No active request / assignment to cancel.");
        } else {
            officer.cancelRegistration();
            System.out.println("Registration removed.");
        }
    }

    /* Read‑only helpers */
    public String getRegistrationStatus(HDBOfficer o){ return o.getRegStatus().name(); }
    public BTOProject viewHandledProject(HDBOfficer o){
        return o.isHandlingProject() ? findProject(o.getHandlingProjectId()) : null;
    }

    /* -------------------------------------------------------- */
    /*  2 – Enquiries                                           */
    /* -------------------------------------------------------- */

    public List<Enquiry> viewProjectEnquiries(HDBOfficer officer) {
        List<Enquiry> all = enquirySvc.getAllEnquiries();
        List<Enquiry> res = new ArrayList<>();
        if (!officer.isHandlingProject()) return res;

        String pid = officer.getHandlingProjectId();
        for (Enquiry e : all)
            if (e.getProjectName().equalsIgnoreCase(pid))
                res.add(e);
        return res;
    }

    public void replyToEnquiry(String enquiryId, String message) {
        enquirySvc.replyToEnquiry(enquiryId, message);
    }

    /* -------------------------------------------------------- */
    /*  3 – Flat booking duties                                 */
    /* -------------------------------------------------------- */

    public boolean bookFlat(String applicantNric, String flatType) {

        Application app = applicantSvc.getApplication(applicantNric);
        if (app == null || !"Successful".equalsIgnoreCase(app.getStatus())) {
            System.out.println("Booking rejected – application not in Successful state.");
            return false;
        }

        BTOProject proj = findProject(app.getProjectName());
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
        BTOProject p = findProject(app.getProjectName());

        System.out.println("\n======= BOOKING RECEIPT =======");
        System.out.printf("Name / NRIC : %s / %s%n", a.getName(), a.getNRIC());
        System.out.printf("Age / Status: %d / %s%n", a.getAge(), a.getMaritalStatus());
        System.out.printf("Project     : %s (%s)%n", p.getProjectName(), p.getNeighborhood());
        System.out.printf("Flat Type   : %s%n", a.getFlatType());
        System.out.println("================================\n");
    }

    /* -------------------------------------------------------- */
    /*  Helper                                                  */
    /* -------------------------------------------------------- */

    private BTOProject findProject(String name) {
        for (BTOProject p : projectRepo.getAllProjects())
            if (p.getProjectName().equalsIgnoreCase(name))
                return p;
        return null;
    }
}



