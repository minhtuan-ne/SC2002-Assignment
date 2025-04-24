package main.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import main.models.*;
import main.util.FileManager;

public class HDBManagerService {
    private final FileManager fileManager;
    private final ProjectService projectSvc;

    public HDBManagerService(ProjectService projectSvc, FileManager fileManager) {
        this.projectSvc = projectSvc;
        this.fileManager = fileManager;
    }

    public boolean createProject(HDBManager manager, String name, String neighborhood, Date startDate, Date endDate, int twoRoomUnits, int threeRoomUnits) {    
        List<BTOProject> managerProjects = viewOwnProjects(manager);
        
        // Check if manager has any projects with visibility ON and application deadline not passed
        Date currentDate = new Date(); // Get current date
        
        for (BTOProject p : managerProjects) {
            System.out.println("Project: " + p.getProjectName() + ", Visible: " + p.isVisible() + 
                            ", End date: " + p.getEndDate() + ", Current date: " + currentDate);
            
            if (p.isVisible() && !currentDate.after(p.getEndDate())) {
                System.out.println("Project is visible and not expired");
                break;
            }
        }
    
        // Create and save the project as before
        List<Flat> flats = List.of(new TwoRoom(twoRoomUnits, 0), new ThreeRoom(threeRoomUnits, 0));
        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flats, 10, new ArrayList<>());       
        projectSvc.addProject(newProject);
        manager.addProject(newProject);
    
        // Save project to file
        // Set default prices - these would normally come from parameters
        int twoRoomPrice = 350000;  // Default 2-room price
        int threeRoomPrice = 450000; // Default 3-room price
        
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
            10 
        );
        
        return true;
    }

    public List<BTOProject> viewAllProjects() {
        return projectSvc.getAllProjects();
    }

    public List<BTOProject> viewOwnProjects(HDBManager manager) {
        List<BTOProject> ownProjects = manager.getProjects();
        
        if (ownProjects.isEmpty()) {
            ownProjects = projectSvc.getAllProjects().stream()
                .filter(p -> p.getManager().getNRIC().equals(manager.getNRIC()))
                .collect(Collectors.toList());
            
            // Update manager's personal list for future use
            for (BTOProject p : ownProjects) {
                manager.addProject(p);
            }
        }
        
        return ownProjects;
    }

    public void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility) {
        if (project.getManager().getNRIC().equals(manager.getNRIC())) {
            project.setVisibility(visibility);
        }
    }

    public void editBTOProject(HDBManager manager, BTOProject project, String newName, String newNeighborhood,
        Date newStartDate, Date newEndDate, int newTwoRoomUnits, int newThreeRoomUnits) {
        
        // Store original project name before updating (in case it changes)
        String originalName = project.getProjectName();
        
        // Update in-memory project
        project.setProjectName(newName);
        project.setNeighborhood(newNeighborhood);
        project.setStartDate(newStartDate);
        project.setEndDate(newEndDate);
        project.setUnits("2-room",newTwoRoomUnits);
        project.setUnits("3-room",newThreeRoomUnits);
        
        // Update the project file
        try {
            boolean success = fileManager.updateProject(
                originalName,    // Original name to find the entry
                newName,         // New project name
                newNeighborhood, 
                newStartDate, 
                newEndDate, 
                newTwoRoomUnits, 
                newThreeRoomUnits
            );
            
            if (!success) {
                System.out.println("Failed to update project in file. Memory updated but file not changed.");
            }
        } catch (IOException e) {
            System.out.println("Failed to update project file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteBTOProject(HDBManager manager, BTOProject project) {
        // Store project name before removing it
        String projectName = project.getProjectName();
        
        // Remove from memory
        manager.removeProject(project);
        projectSvc.removeProject(project);
        
        // Remove from file
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

    public boolean handleBTOApplication(HDBManager manager, Application application, boolean approve) {
        String projectName = application.getProjectName();
        
        // Find the correct project
        BTOProject matchingProject = null;
        for (BTOProject project : projectSvc.getAllProjects()) {
            if (project.getProjectName().equalsIgnoreCase(projectName)) {
                matchingProject = project;
                break;
            }
        }
        
        if (matchingProject == null) {
            return false;
        }
        
        // Check if manager has permission to manage this project
        if (!matchingProject.getManager().getNRIC().equals(manager.getNRIC())) {
            return false;
        }
        
        if (approve) {
            // Check available units before approving
            String flatType = application.getFlatType();
            int available = matchingProject.getUnits(flatType);
            
            if (available > 0) {
                // Update application status
                application.setStatus("Successful");
                
                // Reduce available units
                matchingProject.setUnits(flatType, available - 1);
                return true;
            } else {
                return false;  // No units available to approve
            }
        } else {
            // Reject the application
            application.setStatus("Unsuccessful");
            return true;  // Rejection is always successful
        }
    }
    
    public void handleWithdrawal(HDBManager manager, Application application) {
        String projectName = application.getProjectName();
        for (BTOProject project : projectSvc.getAllProjects()) {
            if (project.getManager().equals(manager)
                && project.getProjectName().equalsIgnoreCase(projectName)) {

                String status = application.getStatus();
                if ("Successful".equalsIgnoreCase(status) 
                    || "Booked".equalsIgnoreCase(status)) {
                    String flatType = application.getFlatType();
                    int current = project.getUnits(flatType);
                    project.setUnits(flatType, current + 1);
                }
                application.setStatus("Withdrawn");
            }
        }
    }

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

    public boolean assignOfficerToProject(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (project.getManager().getNRIC().equals(manager.getNRIC())) {
            project.addOfficer(officer);
            
            // Need to get officer name to match the format in the file
            String officerName = getOfficerNameByNRIC(officer.getNRIC());
            
            // Update the project file with the new officer assignment
            fileManager.updateProjectOfficer(project.getProjectName(), officer.getNRIC(), officerName, true);
            
            return true;
        }
        return false;
    }

    public boolean removeOfficerFromProject(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (project.getManager().equals(manager)) {
            project.removeOfficer(officer);
            
            // Need to get officer name to match the format in the file
            String officerName = getOfficerNameByNRIC(officer.getNRIC());
            
            // Update the project file to remove the officer
            fileManager.updateProjectOfficer(project.getProjectName(), officer.getNRIC(), officerName, false);
            
            return true;
        }
        return false;
    }
    
    // Helper method to get officer name by NRIC
    private String getOfficerNameByNRIC(String nric) {
        
        List<List<String>> officers = fileManager.readFile("OfficerList.txt");
        for (List<String> officer : officers) {
            if (officer.size() > 1 && officer.get(1).equals(nric)) {
                return officer.get(0); // Return officer name
            }
        }
        
        return "Unknown"; // Fallback if officer not found
    }
}
