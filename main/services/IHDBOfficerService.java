package main.services;

import main.models.Enquiry;
import main.models.HDBOfficer;
import main.models.BTOProject;
import java.util.List;

/**
 * All officer‑specific operations.
 * (Standard applicant operations are accessed through ApplicantService.)
 */
public interface IHDBOfficerService {

    /* ----- Registration requests ----- */
    boolean registerToHandleProject(HDBOfficer officer, String projectId);  // returns success / failure
    void    cancelRegistration      (HDBOfficer officer);                  // officer retracts

    /* ----- Read‑only helpers ----- */
    String         getRegistrationStatus(HDBOfficer officer);              // PENDING / APPROVED / NONE
    BTOProject     viewHandledProject (HDBOfficer officer);
    List<Enquiry>  viewProjectEnquiries(HDBOfficer officer);

    /* ----- Project enquiries ----- */
    void replyToEnquiry(String enquiryId, String message);

    /* ----- Flat booking duties ----- */
    boolean bookFlat   (String applicantNric, String flatType);            // updates stock + status
    void    generateReceipt(String applicantNric);                         // prints receipt
}
