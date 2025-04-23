package main.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import main.models.*;

public class ApplicantService{
    private final List<Application> applications = new ArrayList<>();

    public ApplicantService(){}

    public boolean apply(Applicant applicant, BTOProject project, String flatType) {
        //if officer apply, check if managing the project
        if (applicant instanceof HDBOfficer officer) {
            if (officer.isHandlingProject() && project.getProjectName().equalsIgnoreCase(officer.getHandlingProjectId())) {
                System.out.println("As an HDB Officer, you cannot apply for the project you are handling.");
                return false;
            }
        }
        if (project == null) {
            System.out.println("Project not found.");
            return false;
        }
        // ... user already applied?
        if (hasApplied(applicant)) {
            System.out.println("You already have an active application.");
            return false;
        }

        String maritalStatus = applicant.getMaritalStatus();
        boolean isSingle  = maritalStatus.equalsIgnoreCase("Single");
        boolean isMarried = maritalStatus.equalsIgnoreCase("Married");
        int     age       = applicant.getAge();

        // enforce flat‑type rules:
        if (flatType.equalsIgnoreCase("2-room")) {
            if (!(isSingle && age >= 35) && !(isMarried && age >= 21)) {
                System.out.println("Only singles 35+ or married 21+ can apply for 2-room flats.");
                return false;
            }
        }
        else if (flatType.equalsIgnoreCase("3-room")) {
            if (!(isMarried && age >= 21)) {
                System.out.println("Only married applicants 21+ can apply for 3-room flats.");
                return false;
            }
        }
        else {
            System.out.println("Invalid flat type.");
            return false;
        }

        // passed all checks:
        Application application = new Application(applicant, project.getProjectName(), flatType);
        applications.add(application);
        System.out.println("Application submitted successfully.");
        return true;
    }

    public boolean hasApplied(Applicant applicant) {
        for (Application a : applications) {
            if (a.getApplicant().getNRIC().equals(applicant.getNRIC()) && !a.getStatus().equalsIgnoreCase("Unsuccessful")) {
                return true;
            }
        }
        return false;
    }

    public List<Application> getAllApplication(){
        return applications;
    }

    public Application getApplication(String nric) {
        // scan from the end so we pick up the most‐recent application first
        for (int i = applications.size() - 1; i >= 0; i--) {
            Application a = applications.get(i);
            if (a.getApplicant().getNRIC().equals(nric)
                // ignore any withdrawn/unsuccessful apps
                && !a.getStatus().equalsIgnoreCase("Withdrawn")) {
            return a;
            }
        }
        return null;
    }

    public void viewAppliedProject(Applicant applicant, List<BTOProject> allProjects) {
        Application app = getApplication(applicant.getNRIC());
        if (app == null) {
            System.out.println("No application found.");
            return;
        }

        System.out.println("Status: " + app.getStatus());
        System.out.println("Flat Type: " + app.getFlatType());

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");

        for (BTOProject p : allProjects) {
            if (p.getProjectName().equals(app.getProjectName())) {
                System.out.println("Project Name: " + p.getProjectName());
                System.out.println("Neighborhood: " + p.getNeighborhood());
                System.out.println("Application Period: " + df.format(p.getStartDate()) + " to " + df.format(p.getEndDate()));
                System.out.println("Manager: " + p.getManager().getName());
                return;
            }
        }
        System.out.println("Applied project details are not found.");
    }

    public boolean requestWithdrawal(Applicant applicant) {
        Application app = getApplication(applicant.getNRIC());
        if (app == null) {
            System.out.println("No application to withdraw.");
            return false;
        }

        if (app.getStatus().equalsIgnoreCase("Pending") || app.getStatus().equalsIgnoreCase("Successful")) {
            app.setStatus("Unsuccessful");
            System.out.println("Application withdrawn.");
            return true;
        }

        System.out.println("Cannot withdraw application in current state: " + app.getStatus());
        return false;
    }

    public List<BTOProject> viewAvailableProjects(Applicant applicant, List<BTOProject> allProjects) {
        List<BTOProject> result = new ArrayList<>();
        Date today = new Date();

        for (BTOProject project : allProjects) {
            if (!project.isVisible()) continue;
            if (today.before(project.getStartDate()) || today.after(project.getEndDate())) continue;

            // HDBOfficer logic — allow viewing if not handling this project
            if (applicant instanceof HDBOfficer officer) {
                if (officer.isHandlingProject()
                        && project.getProjectName().equalsIgnoreCase(officer.getHandlingProjectId())) {
                    continue;  //  Don't show the project the officer is handling
                }

                // Officer can view any visible, open project
                if (project.getUnits("2-room") > 0 || project.getUnits("3-room") > 0) {
                    result.add(project);
                    continue;
                }
            }

            // Applicant logic
            boolean isSingle = applicant.getMaritalStatus().equalsIgnoreCase("Single") && applicant.getAge() >= 35;
            boolean isMarried = applicant.getMaritalStatus().equalsIgnoreCase("Married") && applicant.getAge() >= 21;

            if ((isSingle && project.getUnits("2-room") > 0)
                    || (isMarried && project.getUnits("2-room") > 0)
                    || (isMarried && project.getUnits("3-room") > 0)) {
                result.add(project);
            }
        }

        return result;
    }
}
