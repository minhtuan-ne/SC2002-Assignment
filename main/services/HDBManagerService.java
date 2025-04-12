package main.services;

import java.util.ArrayList;
import java.util.Date;
import main.models.*;

public class HDBManagerService {

    // Create a new project
    public static boolean createProject(HDBManager manager, String name, String neighborhood, Date startDate, Date endDate, ArrayList<String> flatTypes, int twoRoomUnits, int threeRoomUnits) {
        ArrayList<BTOProject> existingProjects = manager.getProjects();

        for (BTOProject project : existingProjects) {
            // Fix: use getter methods assuming start/endDate are private
            if (startDate.before(project.getEndDate()) && endDate.after(project.getStartDate())) {
                return false; // Overlap found
            }
        }

        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flatTypes, twoRoomUnits, threeRoomUnits, 10);
        ProjectRepository.addProject(newProject);
        manager.addProject(newProject);
        return true;
    }

    // View all projects
    public static ArrayList<BTOProject> viewAllProjects() {
        return ProjectRepository.getAllProjects();
    }

    // View manager's own projects
    public static ArrayList<BTOProject> viewOwnProjects(HDBManager manager) {
        return manager.getProjects();
    }

    // Change project visibility
    public static void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility) {
        if (project.getManager().equals(manager)) {
            project.setVisibility(visibility);
        }
    }

    // Edit a BTO project
    public static void editBTOProject(HDBManager manager, BTOProject project, String newName, String newNeighborhood, Date newStartDate, Date newEndDate, ArrayList<String> flatTypes, int newTwoRoomUnits, int newThreeRoomUnits) {
        if (project.getManager().equals(manager)) {
            project.setProjectName(newName);
            project.setNeighborhood(newNeighborhood);
            project.setStartDate(newStartDate);
            project.setEndDate(newEndDate);
            project.setTwoRoomUnitsAvailable(newTwoRoomUnits);
            project.setThreeRoomUnitsAvailable(newThreeRoomUnits);
        }
    }

    // Delete a project
    public static void deleteBTOProject(HDBManager manager, BTOProject project) {
        if (project.getManager().equals(manager)) {
            manager.removeProject(project);
            ProjectRepository.removeProject(project);
        }
    }

    // Officer registration
    public static boolean handleOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (!project.getManager().equals(manager)) {
            return false;
        }
        if (project.getHDBOfficers().size() < project.getMaxOfficers()) {
            project.getHDBOfficers().add(officer);
            return true;
        }
        return false;
    }

    // Handle BTO application approval
    public static boolean handleBTOApplication(HDBManager manager, Application application) {
        String projectName = application.getProjectName();
        ArrayList<BTOProject> all = ProjectRepository.getAllProjects();

        for (BTOProject project : all) {
            if (project.getProjectName().equalsIgnoreCase(projectName)) {
                if (!project.getManager().equals(manager)) {
                    return false;
                }

                String flatType = application.getFlatType();
                int available = project.getUnits(flatType);
                if (available > 0) {
                    // Assume update status and reduce count happens elsewhere
                    return true;
                }
            }
        }
        return false;
    }

    // Handle withdrawal request
    public static void handleWithdrawal(HDBManager manager, Application application) {
        String projectName = application.getProjectName();
        ArrayList<BTOProject> all = ProjectRepository.getAllProjects();

        for (BTOProject project : all) {
            if (project.getManager().equals(manager) && project.getProjectName().equalsIgnoreCase(projectName)) {
                String status = application.getStatus();

                if ("Successful".equalsIgnoreCase(status) || "Booked".equalsIgnoreCase(status)) {
                    String flatType = application.getFlatType();
                    int current = project.getUnits(flatType);
                    project.setUnits(flatType, current + 1);
                }

                application.setStatus("Withdrawn");
            }
        }
    }

    // Generate booking report based on 2-room or 3-room
    public static void bookingReport(HDBManager manager, String filter) {
        ArrayList<BTOProject> all = manager.getProjects();

        for (BTOProject project : all) {
            for (Application application : project.getApplications()) {
                if ("Booked".equalsIgnoreCase(application.getStatus())
                        && application.getFlatType().equalsIgnoreCase(filter)) {

                    System.out.println("Applicant: " + application.getApplicant().getNRIC()
                            + ", Flat type: " + application.getFlatType()
                            + ", Project name: " + project.getProjectName()
                            + ", Age: " + application.getApplicant().getAge()
                            + ", Marital status: " + application.getApplicant().getMaritalStatus());
                }
            }
        }
    }
}
