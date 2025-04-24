package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import main.models.*;
import main.util.FileManager;

/**
 * Service class to handle applicant-related operations such as applying for a project,
 * viewing applications, withdrawing, and listing available projects.
 * 
 * 
 */
public class ApplicantService {

    /** List of all applications in the system */
    private final List<Application> applications = new ArrayList<>();

    /** Utility for file persistence operations */
    private final FileManager fileManager;

    /**
     * Constructs an ApplicantService instance and loads applications from file.
     * 
     * @param fileManager utility class for file operations
     */
    public ApplicantService(FileManager fileManager) {
        this.fileManager = fileManager;
        try {
            Path path = Paths.get("data", "ApplicationList.txt");
            List<String> lines = Files.readAllLines(path).stream()
                .filter(l -> !l.startsWith("Applicant") && !l.isBlank())
                .collect(Collectors.toList());

            List<User> users = fileManager.loadAllUser();
            Map<String, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getNRIC, u -> u));

            for (String line : lines) {
                String[] cols = line.split("\\t");
                String applicantNRIC = cols[0];
                String projectName = cols[1];
                String type = cols[2];
                String status = cols[3];
                String prevStatus = cols[4];

                Applicant applicant = (Applicant) userMap.get(applicantNRIC);
                if (applicant == null) {
                    System.err.println("No user found for NRIC: " + applicantNRIC);
                    continue;
                }

                Application app = new Application(applicant, projectName, type, status, prevStatus);
                applications.add(app);
            }
        } catch (IOException ex) {
            System.err.println("ERROR loading ApplicationList.txt: " + ex.getMessage());
        }
    }

    /**
     * Applies for a BTO project if applicant meets all requirements.
     *
     * @param applicant the user applying
     * @param project the BTO project to apply for
     * @param flatType the type of flat being applied for
     * @param fileManager utility for saving application
     * @return true if application is submitted, false otherwise
     */
    public boolean apply(Applicant applicant, BTOProject project, String flatType, FileManager fileManager) {
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

        if (hasApplied(applicant)) {
            System.out.println("You already have an active application.");
            return false;
        }

        String maritalStatus = applicant.getMaritalStatus();
        boolean isSingle = maritalStatus.equalsIgnoreCase("Single");
        boolean isMarried = maritalStatus.equalsIgnoreCase("Married");
        int age = applicant.getAge();

        if (flatType.equalsIgnoreCase("2-room")) {
            if (!(isSingle && age >= 35) && !(isMarried && age >= 21)) {
                System.out.println("Only singles 35+ or married 21+ can apply for 2-room flats.");
                return false;
            }
        } else if (flatType.equalsIgnoreCase("3-room")) {
            if (!(isMarried && age >= 21)) {
                System.out.println("Only married applicants 21+ can apply for 3-room flats.");
                return false;
            }
        } else {
            System.out.println("Invalid flat type.");
            return false;
        }

        Application application = new Application(applicant, project.getProjectName(), flatType);
        applications.add(application);
        fileManager.saveApplication(application);
        System.out.println("Application submitted successfully.");
        return true;
    }

    /**
     * Checks whether the applicant has an active application.
     * 
     * @param applicant the applicant to check
     * @return true if an active application exists, false otherwise
     */
    public boolean hasApplied(Applicant applicant) {
        for (Application a : applications) {
            if (a.getApplicant().getNRIC().equals(applicant.getNRIC()) &&
                !a.getStatus().equalsIgnoreCase("Unsuccessful") &&
                !a.getStatus().equalsIgnoreCase("Withdrawn")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the list of all applications in the system.
     * 
     * @return list of applications
     */
    public List<Application> getAllApplication() {
        return applications;
    }

    /**
     * Retrieves the most recent application for a given NRIC.
     * 
     * @param nric the NRIC of the applicant
     * @return the latest Application object, or null if not found
     */
    public Application getApplication(String nric) {
        for (int i = applications.size() - 1; i >= 0; i--) {
            Application a = applications.get(i);
            if (a.getApplicant().getNRIC().equals(nric) && !a.getStatus().equalsIgnoreCase("Withdrawn")) {
                return a;
            }
        }
        return null;
    }

    /**
     * Displays project details and status for the current applicant’s application.
     * 
     * @param applicant the user whose application to view
     * @param allProjects list of all available projects
     */
    public void viewAppliedProject(Applicant applicant, List<BTOProject> allProjects) {
        Application app = getApplication(applicant.getNRIC());
        if (app == null) {
            System.out.println("No application found.");
            return;
        }

        System.out.println("Status: " + app.getStatus());
        if (app.getStatus().equals("Withdrawing")) {
            System.out.println("Previous Status: " + app.getPrevStatus());
        }
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

    /**
     * Requests a withdrawal for an applicant's application.
     * 
     * @param applicant the user requesting to withdraw
     * @return true if the withdrawal request is successful
     */
    public boolean requestWithdrawal(Applicant applicant) {
        Application app = getApplication(applicant.getNRIC());
        if (app == null) {
            System.out.println("No application to withdraw.");
            return false;
        }

        if (app.getStatus().equalsIgnoreCase("Pending") || app.getStatus().equalsIgnoreCase("Successful")) {
            app.setPrevStatus(app.getStatus());
            app.setStatus("Withdrawing");
            fileManager.updateApplication(applicant.getNRIC(), app);
            System.out.println("Withdrawal request submitted!");
            return true;
        }

        System.out.println("Cannot withdraw application in current state: " + app.getStatus());
        return false;
    }

    /**
     * Lists projects that are currently visible and open to the applicant.
     * 
     * @param applicant the applicant viewing available projects
     * @param allProjects all BTO projects in the system
     * @return list of projects the applicant is eligible to apply for
     */
    public List<BTOProject> viewAvailableProjects(Applicant applicant, List<BTOProject> allProjects) {
        List<BTOProject> result = new ArrayList<>();
        Date today = new Date();

        for (BTOProject project : allProjects) {
            if (!project.isVisible()) continue;
            if (today.before(project.getStartDate()) || today.after(project.getEndDate())) continue;

            if (applicant instanceof HDBOfficer officer) {
                if (officer.isHandlingProject() &&
                    project.getProjectName().equalsIgnoreCase(officer.getHandlingProjectId())) {
                    continue;
                }
                if (project.getUnits("2-room") > 0 || project.getUnits("3-room") > 0) {
                    result.add(project);
                    continue;
                }
            }

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

