// main/services/HDBManagerService.java
package main.services;

import main.models.*;
import main.repositories.IProjectRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HDBManagerService implements IHDBManagerService {
    private final IProjectRepository projectRepository;

    public HDBManagerService(IProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
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

        // Check for overlapping projects
        for (BTOProject p : projectRepository.getAllProjects()) {
            if (startDate.before(p.getEndDate()) && endDate.after(p.getStartDate())) {
                // Overlap found
                return false;
            }
        }
        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flatTypes, twoRoomUnits, threeRoomUnits, 10);
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
        return manager.getProjects();
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
    public boolean handleBTOApplication(HDBManager manager, Application application) {
        String projectName = application.getProjectName();
        for (BTOProject project : projectRepository.getAllProjects()) {
            if (project.getProjectName().equalsIgnoreCase(projectName)) {
                if (!project.getManager().equals(manager)) {
                    return false;
                }
                String flatType = application.getFlatType();
                int available = project.getUnits(flatType);
                if (available > 0) {
                    // E.g. set status or reduce units
                    application.setStatus("Successful");
                    project.setUnits(flatType, available - 1);
                    return true;
                }
            }
        }
        return false;
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
}
