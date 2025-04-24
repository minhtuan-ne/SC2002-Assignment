package main.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import main.models.Applicant;
import main.models.Enquiry;

/**
 * Service class to manage all operations related to Enquiries,
 * such as submitting, editing, deleting, replying, and retrieving enquiries.
 * 
 * @author 
 * @version 1.0
 * @since 2025-04-25
 */
public class EnquiryService {

    /** Internal list to store all enquiries */
    private final List<Enquiry> enquiries;

    /**
     * Constructs a new EnquiryService instance with an empty enquiry list.
     */
    public EnquiryService() {
        this.enquiries = new ArrayList<>();
    }

    /**
     * Submits a new enquiry for the given applicant and project.
     *
     * @param applicant the applicant submitting the enquiry
     * @param projectName the name of the BTO project
     * @param message the enquiry message
     */
    public void submitEnquiry(Applicant applicant, String projectName, String message) {
        String fullId  = UUID.randomUUID().toString();
        String shortId = fullId.substring(0, 8);
        Enquiry e       = new Enquiry(shortId, applicant.getNRIC(), projectName, message);
        enquiries.add(e);
        System.out.println("Enquiry submitted successfully with ID: " + shortId);
    }

    /**
     * Retrieves all enquiries submitted by a specific applicant.
     *
     * @param applicant the applicant whose enquiries are to be retrieved
     * @return list of enquiries submitted by the applicant
     */
    public List<Enquiry> getApplicantEnquiries(Applicant applicant) {
        List<Enquiry> result = new ArrayList<>();
        for (Enquiry e : enquiries) {
            if (e.getUserNric().equalsIgnoreCase(applicant.getNRIC())) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Deletes a specific enquiry made by the applicant.
     *
     * @param applicant the applicant requesting the deletion
     * @param enquiryId the ID of the enquiry to delete
     * @return true if the enquiry is deleted, false otherwise
     */
    public boolean deleteEnquiry(Applicant applicant, String enquiryId) {
        Iterator<Enquiry> iter = enquiries.iterator();
        while (iter.hasNext()) {
            Enquiry e = iter.next();
            if (e.getEnquiryId().equals(enquiryId) 
                && e.getUserNric().equalsIgnoreCase(applicant.getNRIC())) {
                iter.remove();
                System.out.println("Enquiry deleted successfully.");
                return true;
            }
        }
        System.out.println("Enquiry not found or no permission to delete.");
        return false;
    }

    /**
     * Edits the message of an existing enquiry submitted by the applicant.
     *
     * @param applicant the applicant editing the enquiry
     * @param enquiryId the ID of the enquiry to edit
     * @param newMessage the new message content
     * @return true if the enquiry is updated, false otherwise
     */
    public boolean editEnquiry(Applicant applicant, String enquiryId, String newMessage) {
        for (Enquiry e : enquiries) {
            if (e.getEnquiryId().equals(enquiryId) 
                && e.getUserNric().equalsIgnoreCase(applicant.getNRIC())) {
                e.setMessage(newMessage);
                System.out.println("Enquiry updated successfully.");
                return true;
            }
        }
        System.out.println("Enquiry not found or no permission to edit.");
        return false;
    }

    /**
     * Retrieves all enquiries in the system.
     *
     * @return a copy of the list of all enquiries
     */
    public List<Enquiry> getAllEnquiries() {
        // return a copy so callers can’t mutate our internal list
        return new ArrayList<>(enquiries);
    }

    /**
     * Adds a reply to a specific enquiry.
     *
     * @param enquiryId the ID of the enquiry to reply to
     * @param message the reply message
     */
    public void replyToEnquiry(String enquiryId, String message) {
        for (Enquiry e : enquiries) {
            if (e.getEnquiryId().equals(enquiryId)) {
                // in a real system you’d persist this reply somewhere;
                // for now we just print confirmation:
                e.setReply(message);
                System.out.println("Replied to enquiry " + enquiryId + ": " + message);
                return;
            }
        }
        System.out.println("Enquiry not found: " + enquiryId);
    }
}

