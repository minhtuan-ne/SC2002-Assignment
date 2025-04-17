package main.services;

import main.models.Applicant;
import main.models.Enquiry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class EnquiryService implements IEnquiryService {
    private final List<Enquiry> enquiries;

    public EnquiryService() {
        this.enquiries = new ArrayList<>();
    }

    @Override
    public void submitEnquiry(Applicant applicant, String projectName, String message) {
        String enquiryId = UUID.randomUUID().toString();
        Enquiry e = new Enquiry(enquiryId, applicant.getNRIC(), projectName, message);
        enquiries.add(e);
        System.out.println("Enquiry submitted successfully with ID: " + enquiryId);
    }

    @Override
    public List<Enquiry> getApplicantEnquiries(Applicant applicant) {
        List<Enquiry> result = new ArrayList<>();
        for (Enquiry e : enquiries) {
            if (e.getUserNric().equalsIgnoreCase(applicant.getNRIC())) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
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

    @Override
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
}