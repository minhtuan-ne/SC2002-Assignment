package main.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import main.models.Applicant;
import main.models.Enquiry;

public class EnquiryService {
    private final List<Enquiry> enquiries;

    public EnquiryService() {
        this.enquiries = new ArrayList<>();
    }

    public void submitEnquiry(Applicant applicant, String projectName, String message) {
        String fullId  = UUID.randomUUID().toString();
        String shortId = fullId.substring(0, 8);
        Enquiry e       = new Enquiry(shortId, applicant.getNRIC(), projectName, message);
        enquiries.add(e);
        System.out.println("Enquiry submitted successfully with ID: " + shortId);
    }

    public List<Enquiry> getApplicantEnquiries(Applicant applicant) {
        List<Enquiry> result = new ArrayList<>();
        for (Enquiry e : enquiries) {
            if (e.getUserNric().equalsIgnoreCase(applicant.getNRIC())) {
                result.add(e);
            }
        }
        return result;
    }

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

    public List<Enquiry> getAllEnquiries() {
        // return a copy so callers can’t mutate our internal list
        return new ArrayList<>(enquiries);
    }

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