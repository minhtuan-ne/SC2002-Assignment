package main.service;

import java.util.ArrayList;
import java.util.Date;
import main.models.BTOProject;
import main.models.HDBManager;
import main.models.ProjectRepository;

public class HDBManagerService {
    // create a new project
    public static boolean createProject(HDBManager manager, String name, String neighborhood, Date startDate, Date endDate, ArrayList<String> flatTypes, int twoRoomUnits, int threeRoomUnits){
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
    public static ArrayList<BTOProject> viewAllProjects(){
        ProjectRepository.getAllProjects();
    }

    // view manager's own project
    public static ArrayList<BTOProject> viewOwnProjects(HDBManager manager){
        return manager.getProjects();
    }

    // change project visibility
    public static void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility){
        if(project.getManager().equals(manager)){
            project.setVisibility(visibility);
        }
    }

    // edit a BTO project
    public static void editBTOProject(HDBManager manager, BTOProject project, String newName, String newNeighborhood, Date newStartDate, Date newEndDate, ArrayList<String> flatTypes, int newTwoRoomUnits, int newThreeRoomUnits){
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
    public static void deleteBTOProject(HDBManager manager, BTOProject project){
        if(project.getManager().equals(manager)){
            manager.removeProject(project);
            ProjectRepository.removeProject(project);
        }
    }

    // officer registration
    public static boolean handleOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer){
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
    public static boolean handleBTOApplication(HDBManager manager, Application application){
        String projectName = application.getProjectName();
        ArrayList<BTOProject> all = ProjectRepository.getAllProjects();
        for(BTOProject project : all){
            if(project.getProjectName().equalsIgnoreCase(projectName)){
                if (!project.getManager().equals(manager)) {
                    return false;
                }
                String flatType = application.getFlatType();
                int available = project.getUnits(flatType);
                if(available > 0){
                    return true;
                }
            }
        }
        return false;
    }

    // handle withdrawal request
    public static void handleWithdrawal(HDBManager manager, BTOApplication application){
        BTOProject projectName = application.getProjectName();
        ArrayList<BTOProject> all = ProjectRepository.getAllProjects();
        for(BTOProject project : all){
            if (project.getManager().equals(manager)) {
                if(project.getProjectName().equalsIgnoreCase(application.getProjectName())){
                    String status = application.getApplicationStatus();
                    if ("Successful".equalsIgnoreCase(status) || "Booked".equalsIgnoreCase(status)) {
                        // restore the flat unit
                        String flatType = application.getFlatType();
                        int current = project.getUnits(flatType);
                        project.setUnits(flatType, current + 1);
                    }
                    application.setApplicationStatus("Withdrawn");
                }
            }
        }
    }

    // generate a report base on 2 filters: 2-room and 3-room
    public static void bookingReport(HDBManager manager, String filter){
        ArrayList<BTOProject> all = manager.getProjects();
        if(filter.equalsIgnoreCase("2-room")){
            for(BTOProject project: all){
                for(Application application : project.getApplications()){
                    if("Booked".equalsIgnoreCase(application.getApplicationStatus())){
                        if(application.getFlatType().equalsIgnoreCase("2-room")){
                            System.out.println("Applicant: " + application.getApplicant().getNRIC() + ", Flat type: " + application.getFlatType() + ", Project name: " + project.getProjectName() + ", Age: " + application.getApplicant().getAge() + ", Marital status: " + application.getApplicant().getMaritalStatus());
                        }
                    }
                }
            }
        } else {
            for(BTOProject project: all){
                for(Application application : project.getApplications()){
                    if("Booked".equalsIgnoreCase(application.getApplicationStatus())){
                        if(application.getFlatType().equalsIgnoreCase("3-room")){
                            System.out.println("Applicant: " + application.getApplicant().getNRIC() + ", Flat type: " + application.getFlatType() + ", Project name: " + project.getProjectName() + ", Age: " + application.getApplicant().getAge() + ", Marital status: " + application.getApplicant().getMaritalStatus());
                        }
                    }
                }
            }
        }
    }
}
