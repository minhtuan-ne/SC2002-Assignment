package main.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import main.models.*;
import main.util.FileManager;

/**
 * Service class that handles operations related to HDB Managers such as
 * creating, editing, deleting BTO projects, toggling visibility, assigning officers,
 * handling applications and withdrawals, and generating booking reports.
 * 
 */
public class HDBManagerService {
    private final FileManager fileManager;
    private final ProjectService projectSvc;

    /**
     * Constructs an HDBManagerService with the specified project and file managers.
     *
     * @param projectSvc service to handle project-related logic
     * @param fileManager utility for file persistence
     */
    public HDBManagerService(ProjectService projectSvc, FileManager fileManager) {
        this.projectSvc = projectSvc;
        this.fileManager = fileManager;
    }

    /**
     * Creates a new BTO project managed by the specified HDB manager.
     *
     * @param manager the manager creating the project
     * @param name project name
     * @param neighborhood neighborhood location
     * @param startDate application start date
     * @param endDate application end date
     * @param twoRoomUnits number of 2-room units
     * @param threeRoomUnits number of 3-room units
     * @return true if project is successfully created
     */
    public boolean createProject(HDBManager manager, String name, String neighborhood, Date startDate, Date endDate, int twoRoomUnits, int threeRoomUnits) {
        List<BTOProject> managerProjects = viewOwnProjects(manager);
        Date currentDate = new Date();

        for (BTOProject p : managerProjects) {
            System.out.println("Project: " + p.getProjectName() + ", Visible: " + p.isVisible() + 
                            ", End date: " + p.getEndDate() + ", Current date: " + currentDate);
            if (p.isVisible() && !currentDate.after(p.getEndDate())) {
                System.out.println("Project is visible and not expired");
                break;
            }
        }

        List<Flat> flats = List.of(new TwoRoom(twoRoomUnits, 0), new ThreeRoom(threeRoomUnits, 0));
        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flats, 10, new ArrayList<>());
        projectSvc.addProject(newProject);
        manager.addProject(newProject);

        int twoRoomPrice = 350000;
        int threeRoomPrice = 450000;

        fileManager.saveProject(
            manager.getNRIC(),
            manager.getName(),
            name,
            neighborhood, 
            startDate, 
            endDate, 
            twoRoomUnits, 
            threeRoomUnits,
            twoRoomPrice,
            threeRoomPrice,
            10,
            true
        );

        return true;
    }

    /**
     * Returns all BTO projects in the system.
     *
     * @return list of all projects
     */
    public List<BTOProject> viewAllProjects() {
        return projectSvc.getAllProjects();
    }

    /**
     * Returns a list of projects created by the specified manager.
     *
     * @param manager the manager whose projects are retrieved
     * @return list of projects owned by the manager
     */
    public List<BTOProject> viewOwnProjects(HDBManager manager) {
        List<BTOProject> ownProjects = manager.getProjects();

        if (ownProjects.isEmpty()) {
            ownProjects = projectSvc.getAllProjects().stream()
                .filter(p -> p.getManager().getNRIC().equals(manager.getNRIC()))
                .collect(Collectors.toList());
            for (BTOProject p : ownProjects) {
                manager.addProject(p);
            }
        }

        return ownProjects;
    }

    /**
     * Sets visibility for a project managed by the specified manager.
     *
     * @param manager the project manager
     * @param project the project to update
     * @param visibility new visibility state
     */
    public void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility) {
        if (project.getManager().getNRIC().equals(manager.getNRIC())) {
            project.setVisibility(visibility);
        }
    }

    /**
     * Edits a project and updates its values both in memory and file.
     *
     * @param manager the manager modifying the project
     * @param project the project to edit
     * @param newName new project name
     * @param newNeighborhood new neighborhood
     * @param newStartDate new start date
     * @param newEndDate new end date
     * @param newTwoRoomUnits updated 2-room unit count
     * @param newThreeRoomUnits updated 3-room unit count
     */
    public void editBTOProject(HDBManager manager, BTOProject project, String newName, String newNeighborhood,
        Date newStartDate, Date newEndDate, int newTwoRoomUnits, int newThreeRoomUnits) {

        String originalName = project.getProjectName();

        project.setProjectName(newName);
        project.setNeighborhood(newNeighborhood);
        project.setStartDate(newStartDate);
        project.setEndDate(newEndDate);
        project.setUnits("2-room", newTwoRoomUnits);
        project.setUnits("3-room", newThreeRoomUnits);

        try {
            boolean success = fileManager.updateProject(originalName, newName, newNeighborhood, newStartDate, newEndDate, newTwoRoomUnits, newThreeRoomUnits);
            if (!success) {
                System.out.println("Failed to update project in file. Memory updated but file not changed.");
            }
        } catch (IOException e) {
            System.out.println("Failed to update project file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes a BTO project from memory and file.
     *
     * @param manager the manager deleting the project
     * @param project the project to delete
     */
    public void deleteBTOProject(HDBManager manager, BTOProject project) {
        String projectName = project.getProjectName();
        manager.removeProject(project);
        projectSvc.removeProject(project);

        try {
            boolean success = fileManager.deleteProjectFromFile(projectName);
            if (!success) {
                System.out.println("Failed to delete project from file. Memory updated but file not changed.");
            }
        } catch (IOException e) {
            System.out.println("Failed to delete project from file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Approves or rejects an application under a project managed by the given manager.
     *
     * @param manager the approving manager
     * @param application the application to process
     * @param approve true to approve, false to reject
     * @return true if operation was successful
     */
    public boolean handleBTOApplication(HDBManager manager, Application application, boolean approve) {
        String projectName = application.getProjectName();
        BTOProject matchingProject = null;

        for (BTOProject project : projectSvc.getAllProjects()) {
            if (project.getProjectName().equalsIgnoreCase(projectName)) {
                matchingProject = project;
                break;
            }
        }

        if (approve) {
            String flatType = application.getFlatType();
            int available = matchingProject.getUnits(flatType);
            if (available > 0) {
                application.setStatus("Successful");
                fileManager.updateApplication(application.getApplicant().getNRIC(), application);                
                return true;
            } else {
                return false;
            }
        } else {
            application.setStatus("Unsuccessful");
            fileManager.updateApplication(application.getApplicant().getNRIC(), application);                
            return true;
        }
    }

    /**
     * Handles a withdrawal request by updating unit availability and application status.
     *
     * @param manager the manager processing the withdrawal
     * @param application the application to withdraw
     * @return true if withdrawal is processed
     */
    public boolean handleWithdrawal(HDBManager manager, Application application) {
        String projectName = application.getProjectName();
        for (BTOProject project : projectSvc.getAllProjects()) {
            if (project.getProjectName().equalsIgnoreCase(projectName)) {
                String status = application.getPrevStatus();
                if (status.equalsIgnoreCase("Booked")) {
                    String flatType = application.getFlatType();
                    int current = project.getUnits(flatType);
                    project.setUnits(flatType, current + 1);
                }
                application.setStatus("Withdrawn");
                application.setPrevStatus("null");
                fileManager.updateApplication(application.getApplicant().getNRIC(), application);
                try {
                    fileManager.updateProject(project.getProjectName(), project.getProjectName(), project.getNeighborhood(), project.getStartDate(), project.getEndDate(), project.getUnits("2-room"), project.getUnits("3-room"));
                } catch (Exception e) {
                    System.out.println("Update project failed");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a report of all booked flats filtered by flat type.
     *
     * @param manager the manager generating the report
     * @param filter flat type filter (e.g., "2-room")
     */
    public void bookingReport(HDBManager manager, String filter) {
        List<BTOProject> all = manager.getProjects();
        for (BTOProject project : all) {
            for (Application application : projectSvc.getApplicationByProject(project)) {
                if ("Booked".equalsIgnoreCase(application.getStatus())
                    && filter.equalsIgnoreCase(application.getFlatType())) {
                    System.out.println("Applicant: " + application.getApplicant().getNRIC()
                        + ", Flat type: " + application.getFlatType()
                        + ", Project name: " + project.getProjectName()
                        + ", Age: " + application.getApplicant().getAge()
                        + ", Marital status: " + application.getApplicant().getMaritalStatus());
                }
            }
        }
    }

    /**
     * Assigns an officer to a project if the manager has control over it.
     *
     * @param manager the manager assigning the officer
     * @param project the target project
     * @param officer the officer to assign
     * @return true if successful
     */
    public boolean assignOfficerToProject(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (project.getManager().getNRIC().equals(manager.getNRIC())) {
            project.addOfficer(officer);
            String officerName = getOfficerNameByNRIC(officer.getNRIC());
            fileManager.updateProjectOfficer(project.getProjectName(), officer.getNRIC(), officerName, true);
            return true;
        }
        return false;
    }

    /**
     * Removes an officer from a project.
     *
     * @param manager the manager controlling the project
     * @param project the project to update
     * @param officer the officer to remove
     * @return true if successful
     */
    public boolean removeOfficerFromProject(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (project.getManager().equals(manager)) {
            project.removeOfficer(officer);
            String officerName = getOfficerNameByNRIC(officer.getNRIC());
            fileManager.updateProjectOfficer(project.getProjectName(), officer.getNRIC(), officerName, false);
            return true;
        }
        return false;
    }

    /**
     * Retrieves officer's name by their NRIC from the OfficerList file.
     *
     * @param nric NRIC of the officer
     * @return name of the officer if found, else "Unknown"
     */
    private String getOfficerNameByNRIC(String nric) {
        List<List<String>> officers = fileManager.readFile("OfficerList.txt");
        for (List<String> officer : officers) {
            if (officer.size() > 1 && officer.get(1).equals(nric)) {
                return officer.get(0);
            }
        }
        return "Unknown";
    }
}

