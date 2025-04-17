package main.services;

import main.models.Applicant;
import main.models.Enquiry;
import java.util.List;

public interface IEnquiryService {
    void submitEnquiry(Applicant applicant, String projectName, String message);
    List<Enquiry> getApplicantEnquiries(Applicant applicant);
    boolean deleteEnquiry(Applicant applicant, String enquiryId);
    boolean editEnquiry(Applicant applicant, String enquiryId, String newMessage);
}
