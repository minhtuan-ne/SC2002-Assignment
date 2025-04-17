package main.services;

import main.models.HDBOfficer;

public interface IHDBOfficerService {
    void assignToProject(HDBOfficer officer, String projectId);
    void removeFromProject(HDBOfficer officer);
    void bookFlat(String applicantNric, String flatType);
    void replyToEnquiry(String enquiryId, String message);
}