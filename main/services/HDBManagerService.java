// main/services/HDBManagerService.java
package main.services;

import main.models.*;
import main.repositories.IProjectRepository;
import main.util.IFileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class HDBManagerService implements IHDBManagerService {
    private final IFileManager fileManager;
    private final IProjectRepository projectRepository;

    private List<String> assignedOfficer;

    public HDBManagerService(IProjectRepository projectRepository, IFileManager fileManager) {
        this.projectRepository = projectRepository;
        this.fileManager = fileManager;
    }

    @Override
    public boolean createProject(HDBManager manager,
                                 String name,
                                 String neighborhood,
                                 Date startDate,
                                 Date endDate,
                                 List<String> flatTypes,
                                 int twoRoomUnits,
                                 int threeRoomUnits) {
    
        // Only check overlapping projects that this manager is handling
        for (BTOProject p : projectRepository.getAllProjects()) {
            if (p.getManager().equals(manager)) {
                if (!endDate.before(p.getStartDate()) && !startDate.after(p.getEndDate())) {
                    // Overlapping project found for this manager
                    return false;
                }
            }
        }
        
        

        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flatTypes, twoRoomUnits, threeRoomUnits, 10, new ArrayList<>());       
        projectRepository.addProject(newProject);
        manager.addProject(newProject);
        return true;
    }
    
    @Override
    public List<BTOProject> viewAllProjects() {
        return projectRepository.getAllProjects();
    }

    @Override
    public List<BTOProject> viewOwnProjects(HDBManager manager) {
       
        List<BTOProject> ownProjects = manager.getProjects();
        
        if (ownProjects.isEmpty()) {
            ownProjects = projectRepository.getAllProjects().stream()
                .filter(p -> p.getManager().getNRIC().equals(manager.getNRIC()))
                .collect(Collectors.toList());
            
            // Update manager's personal list for future use
            for (BTOProject p : ownProjects) {
                manager.addProject(p);
            }
        }
        
        return ownProjects;
    }

    @Override
    public void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility) {
        if (project.getManager().equals(manager)) {
            project.setVisibility(visibility);
        }
    }

    @Override
    public void editBTOProject(HDBManager manager,
                               BTOProject project,
                               String newName,
                               String newNeighborhood,
                               Date newStartDate,
                               Date newEndDate,
                               List<String> flatTypes,
                               int newTwoRoomUnits,
                               int newThreeRoomUnits) {
        if (!project.getManager().equals(manager)) {
            return;
        }
        project.setProjectName(newName);
        project.setNeighborhood(newNeighborhood);
        project.setStartDate(newStartDate);
        project.setEndDate(newEndDate);
        project.setFlatTypes(new ArrayList<>(flatTypes));
        project.setTwoRoomUnitsAvailable(newTwoRoomUnits);
        project.setThreeRoomUnitsAvailable(newThreeRoomUnits);
    }

    @Override
    public void deleteBTOProject(HDBManager manager, BTOProject project) {
        if (project.getManager().equals(manager)) {
            manager.removeProject(project);
            projectRepository.removeProject(project);
        }
    }

    @Override
    public boolean handleOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (!project.getManager().equals(manager)) {
            return false;
        }
        if (project.getHDBOfficers().size() < project.getMaxOfficers()) {
            project.getHDBOfficers().add(officer);
            return true;
        }
        return false;
    }
    @Override
    public boolean handleBTOApplication(HDBManager manager, Application application, boolean approve) {
        String projectName = application.getProjectName();
        
        // Find the correct project
        BTOProject matchingProject = null;
        for (BTOProject project : projectRepository.getAllProjects()) {
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
    
    @Override
    public void handleWithdrawal(HDBManager manager, Application application) {
        String projectName = application.getProjectName();
        for (BTOProject project : projectRepository.getAllProjects()) {
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

    @Override
    public void bookingReport(HDBManager manager, String filter) {
        List<BTOProject> all = manager.getProjects();
        for (BTOProject project : all) {
            for (Application application : project.getApplications()) {
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
    public List<String> getAssignedOfficer() {
        if (assignedOfficer == null) {
            assignedOfficer = new ArrayList<>();
        }
        return assignedOfficer;
    }
  
    @Override
    public boolean assignOfficerToProject(HDBManager manager, BTOProject project, String officerNRIC) {
        if (project.getManager().equals(manager)) {
            project.addAssignedOfficer(officerNRIC);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeOfficerFromProject(HDBManager manager, BTOProject project, String officerNRIC) {
        if (project.getManager().equals(manager)) {
            project.removeAssignedOfficer(officerNRIC);
            return true;
        }
        return false;
    }
    @Override
    public boolean changePassword(HDBManager manager, String oldPassword, String newPassword) {
        try {
            // 1) update in‑memory
            manager.changePassword(oldPassword, newPassword);
            // 2) persist to disk
            fileManager.updatePassword("Manager", manager.getNRIC(), newPassword);
            System.out.println("Password changed successfully.");
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        } catch (IOException ioe) {
            System.out.println("Failed to save new password: " + ioe.getMessage());
            return false;
        }
    }
}
