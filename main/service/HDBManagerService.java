package main.service;

import java.util.ArrayList;
import java.util.Date;
import main.models.BTOProject;
import main.models.HDBManager;
import main.models.ProjectRepository;

public class HDBManagerService {
    // create a new project
    public boolean createProject(HDBManager manager, String name, String neighborhood, Date startDate, Date endDate, ArrayList<String> flatTypes, int twoRoomUnits, int threeRoomUnits){
        ArrayList<BTOProject> existingProjects = manager.getProjects();
        for(BTOProject project : existingProjects){
            // check is there any overlapping between this project and current projects
            if(startDate.before(project.endDate) || endDate.after(project.startDate)){
                return false;
            }
        }
        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flatTypes, twoRoomUnits, threeRoomUnits, 10);
        ProjectRepository.addProject(newProject);
        manager.addProject(newProject);
        return true;
    }

    // view all projects 
    public ArrayList<BTOProject> viewAllProjects(){
        ProjectRepository.getAllProjects();
    }

    // view manager's own project
    public ArrayList<BTOProject> viewOwnProjects(HDBManager manager){
        return manager.getProjects();
    }

    // change project visibility
    public void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility){
        if(project.getManager().equals(manager)){
            project.setVisibility(visibility);
        }
    }

    // edit a BTO project
    public void editBTOProject(HDBManager manager, BTOProject project, String newName, String newNeighborhood, Date newStartDate, Date newEndDate, ArrayList<String> flatTypes, int newTwoRoomUnits, int newThreeRoomUnits){
        if(project.getManager().equals(manager)){
            project.setProjectName(newName);
            project.setNeighborhood(newNeighborhood);
            project.setStartDate(newStartDate);
            project.setEndDate(newEndDate);
            project.setTwoRoomUnitsAvailable(newTwoRoomUnits);
            project.setThreeRoomUnitsAvailable(newThreeRoomUnits);
        }
    }

    // delete a project
    public void deleteBTOProject(HDBManager manager, BTOProject project){
        if(project.getManager().equals(manager)){
            manager.removeProject(project);
            ProjectRepository.removeProject(project);
        }
    }

    // officer registration
    public boolean handleOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer){
        if(!project.getManager().equals(manager)){
            return false;
        }
        if(project.getHDBOfficers().size() < project.getMaxOfficers()){
            project.getHDBOfficers().add(officer);
            return true;
        }
        return false;
    }

    // handle BTO application
    public boolean handleBTOApplication(HDBManager manager, Application application){
        BTOProject project = application.getProject();
        if (!project.getManager().equals(manager)) {
            return false;
        }
        String flatType = application.getFlatType();
        int available = project.getUnits(flatType);
        if(available > 0){
            return true;
        }
        return false;
    }

    // handle withdrawal request
    public void handleWithdrawal(HDBManager manager, BTOApplication application, boolean approve){
        BTOProject project = application.getProject();
        if (project.getManager().equals(manager)) {
            if (approve) {
                String status = application.getApplicationStatus();
                if ("Successful".equals(status) || "Booked".equals(status)) {
                    // restore the flat unit
                    String flatType = application.getFlatType();
                    int current = project.getUnits(flatType);
                    project.setUnits(flatType, current + 1);
                }
                application.setApplicationStatus("Withdrawn");
            } else {
                // haven't think of any case for rejecting withdrawal request
            }
        }
    }

    // generate a report base on 2 filters: room type and marital status
    public void bookingReport(HDBManager manager, String filter){
        ArrayList<BTOProject> all = ProjectRepository.getAllProjects();
        if(filter.equals("Room type")){
            for(BTOProject project: all){
                for(Application application : project.getApplications()){
                    if("Booked".equals(application.getApplicationStatus())){
                        if("")
                        System.out.println("Applicant: " + application.getApplicant().getNRIC() + ", Flat type: " + application.getFlatType() + ", Project name: " + project.getProjectName() + ", Age: " + application.getApplicant().getAge() + ", Marital status: " + application.getApplicant().getMaritalStatus());
                    }
                }
            }
        } else {
            for(BTOProject project: all){
                for(Application application : project.getApplications()){
                    if("Married".equals(application.getApplicationStatus())){
                        System.out.println("Applicant: " + application.getApplicant().getNRIC() + ", Flat type: " + application.getFlatType() + ", Project name: " + project.getProjectName() + ", Age: " + application.getApplicant().getAge() + ", Marital status: " + application.getApplicant().getMaritalStatus());
                    }
                }
            }
        }
    }
}
