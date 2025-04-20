package main.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import main.models.*;

public class HDBManagerService {

    // Create a new project
    public static boolean createProject(HDBManager manager, String name, String neighborhood, Date startDate,
            Date endDate, ArrayList<String> flatTypes, int twoRoomUnits, int threeRoomUnits) {
        ArrayList<BTOProject> existingProjects = manager.getProjects();

        for (BTOProject project : existingProjects) {
            // Fix: use getter methods assuming start/endDate are private
            if (startDate.before(project.getEndDate()) && endDate.after(project.getStartDate())) {
                return false; // Overlap found
            }
        }

        BTOProject newProject = new BTOProject(manager, name, neighborhood, startDate, endDate, flatTypes, twoRoomUnits,
                threeRoomUnits, 10);
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
    public static void editBTOProject(HDBManager manager, BTOProject project, String newName, String newNeighborhood,
            Date newStartDate, Date newEndDate, ArrayList<String> flatTypes, int newTwoRoomUnits,
            int newThreeRoomUnits) {
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

    public static List<HDBOfficer> viewPendingOfficerRegistrations(HDBManager manager, BTOProject project) {
        List<HDBOfficer> pendingOfficers = new ArrayList<>();
        // Find officers who aren't handling a project yet
        for (User user : UserRepository.getAllUsers()) {
            if (user instanceof HDBOfficer) {
                HDBOfficer officer = (HDBOfficer) user;
                if (!officer.isHandlingProject()) {
                    pendingOfficers.add(officer);
                }
            }
        }
        return pendingOfficers;
    }

    public static boolean approveOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer) {
        // Check if manager owns the project
        if (!project.getManager().equals(manager)) {
            return false;
        }

        // Check if project has available slots
        if (project.getHDBOfficers().size() >= project.getMaxOfficers()) {
            return false;
        }

        // Assign officer to project
        project.getHDBOfficers().add(officer);
        officer.assignToProject(project.getProjectName());
        return true;
    }

    public static boolean rejectOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer) {
        return true;
    }

    public static boolean approveWithdrawalRequest(HDBManager manager, Application application) {
        // Check if manager is in charge of the project
        BTOProject project = null;
        for (BTOProject p : ProjectRepository.getAllProjects()) {
            if (p.getProjectName().equals(application.getProjectName()) && p.getManager().equals(manager)) {
                project = p;
                break;
            }
        }

        if (project == null) {
            return false; // Manager isn't in charge of this project
        }

        // Process withdrawal
        if ("Withdrawal Requested".equals(application.getStatus())) {
            application.setStatus("Withdrawn");

            // If the application was previously approved, return the unit to available pool
            if ("Successful".equals(application.getStatus())) {
                String flatType = application.getFlatType();
                int currentUnits = project.getUnits(flatType);
                project.setUnits(flatType, currentUnits + 1);
            }

            return true;
        }

        return false;
    }

    public static boolean rejectWithdrawalRequest(HDBManager manager, Application application) {
        // Check if manager is in charge of the project
        BTOProject project = null;
        for (BTOProject p : ProjectRepository.getAllProjects()) {
            if (p.getProjectName().equals(application.getProjectName()) && p.getManager().equals(manager)) {
                project = p;
                break;
            }
        }

        if (project == null) {
            return false; // Manager isn't in charge of this project
        }

        // Reject withdrawal by returning status to previous status
        if ("Withdrawal Requested".equals(application.getStatus())) {
            // We don't know the previous status, so we'll set it to "Pending" again
            application.setStatus("Pending");
            return true;
        }

        return false;
    }
    public static List<Enquiry> viewAllEnquiries() {
        return EnquiryRepository.getAllEnquiries();
    }
    
    public static List<Enquiry> viewProjectEnquiries(HDBManager manager, String projectName) {
        List<Enquiry> projectEnquiries = new ArrayList<>();
        for (Enquiry enquiry : EnquiryRepository.getAllEnquiries()) {
            if (enquiry.getProjectName().equals(projectName)) {
                projectEnquiries.add(enquiry);
            }
        }
        return projectEnquiries;
    }
    
    public static boolean replyToEnquiry(HDBManager manager, Enquiry enquiry, String reply) {
        // Check if manager is in charge of this project
        boolean isManagerInCharge = false;
        for (BTOProject project : manager.getProjects()) {
            if (project.getProjectName().equals(enquiry.getProjectName())) {
                isManagerInCharge = true;
                break;
            }
        }
        
        if (!isManagerInCharge) {
            return false;
        }
        
        enquiry.setReply(reply);
        return true;
    }
}
