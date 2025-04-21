package main.services;

import main.models.Applicant;
import main.models.Application;
import main.models.BTOProject;
import java.util.List;

public interface IApplicantService {
    boolean apply(Applicant applicant, BTOProject project, String flatType);
    boolean hasApplied(Applicant applicant);
    Application getApplication(String nric);
    void viewAppliedProject(Applicant applicant, List<BTOProject> allProjects);
    boolean requestWithdrawal(Applicant applicant, List<BTOProject> allProjects);
    List<BTOProject> viewAvailableProjects(Applicant applicant, List<BTOProject> allProjects);
    boolean changePassword(Applicant applicant, String oldPassword, String newPassword);
}
