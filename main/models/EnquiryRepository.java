package main.models;

import java.util.ArrayList;
import java.util.List;

public class EnquiryRepository {
    private static final ArrayList<Enquiry> ENQUIRIES = new ArrayList<>();
    
    public static List<Enquiry> getAllEnquiries() {
        return ENQUIRIES;
    }
    
    public static void addEnquiry(Enquiry enquiry) {
        ENQUIRIES.add(enquiry);
    }
    
    public static void removeEnquiry(Enquiry enquiry) {
        ENQUIRIES.remove(enquiry);
    }
    
    public static Enquiry getEnquiryById(String enquiryId) {
        for (Enquiry enquiry : ENQUIRIES) {
            if (enquiry.getEnquiryId().equals(enquiryId)) {
                return enquiry;
            }
        }
        return null;
    }
}