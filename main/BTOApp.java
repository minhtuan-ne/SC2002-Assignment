package main;

import main.models.*;
import main.services.*;
import main.util.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BTOApp {

    private static Scanner scanner = new Scanner(System.in);
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static User currentUser = null;
    private static ArrayList<BTOProject> allProjects = new ArrayList<>();
    private static HashMap<String, User> userDatabase = new HashMap<>();

    public static void main(String[] args) {
        initializeSystem();

        boolean running = true;
        while (running) {
            if (currentUser == null) {
                displayMainMenu();
                int choice = getIntInput("Enter your choice: ");

                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        running = false;
                        System.out.println("Thank you for using the BTO Application System. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } else {
                String role = currentUser.getRole();

                switch (role) {
                    case "Applicant":
                        handleApplicantMenu((Applicant) currentUser);
                        break;
                    case "HDB Manager":
                        handleManagerMenu((HDBManager) currentUser);
                        break;
                    case "HDBOfficer":
                        handleOfficerMenu((HDBOfficer) currentUser);
                        break;
                    default:
                        System.out.println("Unknown user role.");
                        currentUser = null;
                }
            }
        }

        scanner.close();
    }

    // Initialize the system with data from CSV files
    private static void initializeSystem() {
        try {
            // Load and add all managers
            List<HDBManager> managers = FileManager.loadManagers();
            for (HDBManager manager : managers) {
                userDatabase.put(manager.getNRIC(), manager);
            }
            System.out.println("Loaded " + managers.size() + " managers.");
            
            // Load and add all officers
            List<HDBOfficer> officers = FileManager.loadOfficers();
            for (HDBOfficer officer : officers) {
                userDatabase.put(officer.getNRIC(), officer);
            }
            System.out.println("Loaded " + officers.size() + " officers.");
            
            // Load and add all applicants
            List<Applicant> applicants = FileManager.loadApplicants();
            for (Applicant applicant : applicants) {
                userDatabase.put(applicant.getNRIC(), applicant);
            }
            System.out.println("Loaded " + applicants.size() + " applicants.");
            
            // Load all projects
            List<BTOProject> projects = FileManager.loadProjects(userDatabase);
            for (BTOProject project : projects) {
                ProjectRepository.addProject(project);
            }
            System.out.println("Loaded " + projects.size() + " projects.");
            
            // Update allProjects reference
            allProjects = ProjectRepository.getAllProjects();
            
            System.out.println("System initialized with data from CSV files.");
            
        } catch (Exception e) {
            System.out.println("Error initializing system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Display the main menu
    private static void displayMainMenu() {
        System.out.println("\n===== BTO Application System =====");
        System.out.println("1. Login");
        System.out.println("2. Exit");
    }

    // User login
    private static void login() {
        System.out.println("\n===== Login =====");
        System.out.print("NRIC: ");
        String nric = scanner.nextLine().toUpperCase();

        if (!User.isValidNRIC(nric)) {
            System.out.println("Invalid NRIC format.");
            return;
        }

        if (!userDatabase.containsKey(nric)) {
            System.out.println("User not found.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = userDatabase.get(nric);
        if (user.checkPassword(password)) {
            currentUser = user;
            System.out.println("Login successful. Welcome, " + user.getName() + "!");
        } else {
            System.out.println("Incorrect password.");
        }
    }

    // Handle applicant menu
    private static void handleApplicantMenu(Applicant applicant) {
        while (true) {
            System.out.println("\n===== Applicant Menu =====");
            System.out.println("1. View Available Projects");
            System.out.println("2. Apply for Project");
            System.out.println("3. View My Application");
            System.out.println("4. Withdraw Application");
            System.out.println("5. Submit Enquiry");
            System.out.println("6. View My Enquiries");
            System.out.println("7. Edit Enquiry");
            System.out.println("8. Delete Enquiry");
            System.out.println("9. Logout");

            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    viewAvailableProjects(applicant);
                    break;
                case 2:
                    applyForProject(applicant);
                    break;
                case 3:
                    viewApplication(applicant);
                    break;
                case 4:
                    withdrawApplication(applicant);
                    break;
                case 5:
                    submitEnquiry(applicant);
                    break;
                case 6:
                    viewEnquiries(applicant);
                    break;
                case 7:
                    editEnquiry(applicant);
                    break;
                case 8:
                    deleteEnquiry(applicant);
                    break;
                case 9:
                    System.out.println("Logging out...");
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Handle manager menu
    private static void handleManagerMenu(HDBManager manager) {
        while (true) {
            System.out.println("\n===== HDB Manager Menu =====");
            System.out.println("1. Create New Project");
            System.out.println("2. View All Projects");
            System.out.println("3. View My Projects");
            System.out.println("4. Edit Project");
            System.out.println("5. Toggle Project Visibility");
            System.out.println("6. Delete Project");
            System.out.println("7. Handle Applications");
            System.out.println("8. Booking Report");
            System.out.println("9. Logout");

            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    createProject(manager);
                    break;
                case 2:
                    viewAllProjects();
                    break;
                case 3:
                    viewManagerProjects(manager);
                    break;
                case 4:
                    editProject(manager);
                    break;
                case 5:
                    toggleProjectVisibility(manager);
                    break;
                case 6:
                    deleteProject(manager);
                    break;
                case 7:
                    handleApplications(manager);
                    break;
                case 8:
                    generateBookingReport(manager);
                    break;
                case 9:
                    System.out.println("Logging out...");
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Handle officer menu
    private static void handleOfficerMenu(HDBOfficer officer) {
        HDBOfficerService officerService = new HDBOfficerService();

        while (true) {
            System.out.println("\n===== HDB Officer Menu =====");
            System.out.println("1. View Project Assignment");
            System.out.println("2. Book Flat for Applicant");
            System.out.println("3. Reply to Enquiry");
            System.out.println("4. Logout");

            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    System.out.println("\n===== Project Assignment =====");
                    if (officer.isHandlingProject()) {
                        System.out.println("Currently assigned to project: " + officer.getHandlingProjectId());
                    } else {
                        System.out.println("Not currently assigned to any project.");
                    }
                    break;
                case 2:
                    System.out.println("\n===== Book Flat =====");
                    System.out.print("Enter applicant NRIC: ");
                    String applicantNric = scanner.nextLine();
                    System.out.print("Enter flat type (2-room/3-room): ");
                    String flatType = scanner.nextLine();
                    officerService.bookFlat(applicantNric, flatType);
                    break;
                case 3:
                    System.out.println("\n===== Reply to Enquiry =====");
                    System.out.print("Enter enquiry ID: ");
                    String enquiryId = scanner.nextLine();
                    System.out.print("Enter reply message: ");
                    String message = scanner.nextLine();
                    officerService.replyToEnquiry(enquiryId, message);
                    break;
                case 4:
                    System.out.println("Logging out...");
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Applicant: View available projects
    private static void viewAvailableProjects(Applicant applicant) {
        System.out.println("\n===== Available Projects =====");
        List<BTOProject> availableProjects = ApplicantService.viewAvailableProjects(applicant, allProjects);

        if (availableProjects.isEmpty()) {
            System.out.println("No available projects for your eligibility criteria.");
            return;
        }

        for (int i = 0; i < availableProjects.size(); i++) {
            BTOProject project = availableProjects.get(i);
            System.out.println((i + 1) + ". " + project.getProjectName() + " - " + project.getNeighborhood());
            System.out.println("   Application Period: " + dateFormat.format(project.getStartDate()) +
                    " to " + dateFormat.format(project.getEndDate()));

            if (project.getFlatTypes().contains("2-room")) {
                System.out.println("   2-room units available: " + project.getTwoRoomUnitsAvailable());
            }

            if (project.getFlatTypes().contains("3-room")) {
                System.out.println("   3-room units available: " + project.getThreeRoomUnitsAvailable());
            }

            System.out.println();
        }
    }

    // Applicant: Apply for project
    private static void applyForProject(Applicant applicant) {
        System.out.println("\n===== Apply for Project =====");
        List<BTOProject> availableProjects = ApplicantService.viewAvailableProjects(applicant, allProjects);

        if (availableProjects.isEmpty()) {
            System.out.println("No available projects for your eligibility criteria.");
            return;
        }

        for (int i = 0; i < availableProjects.size(); i++) {
            System.out.println((i + 1) + ". " + availableProjects.get(i).getProjectName());
        }

        int projectChoice = getIntInput("Select project number: ");
        if (projectChoice < 1 || projectChoice > availableProjects.size()) {
            System.out.println("Invalid project selection.");
            return;
        }

        BTOProject selectedProject = availableProjects.get(projectChoice - 1);

        System.out.println("Available flat types:");
        ArrayList<String> flatOptions = new ArrayList<>();

        boolean isSingle = applicant.getMaritalStatus().equalsIgnoreCase("Single");
        boolean isMarried = applicant.getMaritalStatus().equalsIgnoreCase("Married");
        int age = applicant.getAge();

        if ((isSingle && age >= 35 || isMarried) && selectedProject.getTwoRoomUnitsAvailable() > 0) {
            System.out.println("1. 2-room");
            flatOptions.add("2-room");
        }

        if (isMarried && selectedProject.getThreeRoomUnitsAvailable() > 0) {
            System.out.println(flatOptions.size() + 1 + ". 3-room");
            flatOptions.add("3-room");
        }

        if (flatOptions.isEmpty()) {
            System.out.println("No suitable flat types available for your eligibility.");
            return;
        }

        int flatChoice = getIntInput("Select flat type number: ");
        if (flatChoice < 1 || flatChoice > flatOptions.size()) {
            System.out.println("Invalid flat type selection.");
            return;
        }

        String selectedFlatType = flatOptions.get(flatChoice - 1);
        ApplicantService.apply(applicant, selectedProject, selectedFlatType);
    }

    // Applicant: View application
    private static void viewApplication(Applicant applicant) {
        System.out.println("\n===== My Application =====");
        ApplicantService.viewAppliedProject(applicant, allProjects);
    }

    // Applicant: Withdraw application
    private static void withdrawApplication(Applicant applicant) {
        System.out.println("\n===== Withdraw Application =====");
        ApplicantService.requestWithdrawal(applicant);
    }

    // Applicant: Submit enquiry
    private static void submitEnquiry(Applicant applicant) {
        System.out.println("\n===== Submit Enquiry =====");

        System.out.println("Select project to enquire about:");
        for (int i = 0; i < allProjects.size(); i++) {
            BTOProject project = allProjects.get(i);
            if (project.isVisible()) {
                System.out.println((i + 1) + ". " + project.getProjectName());
            }
        }

        int projectChoice = getIntInput("Select project number (0 to cancel): ");
        if (projectChoice == 0)
            return;

        if (projectChoice < 1 || projectChoice > allProjects.size()
                || !allProjects.get(projectChoice - 1).isVisible()) {
            System.out.println("Invalid project selection.");
            return;
        }

        String projectName = allProjects.get(projectChoice - 1).getProjectName();

        System.out.print("Enter your message: ");
        String message = scanner.nextLine();

        ApplicantService.submitEnquiry(applicant, projectName, message);
    }

    // Applicant: View enquiries
    private static void viewEnquiries(Applicant applicant) {
        System.out.println("\n===== My Enquiries =====");
        List<Enquiry> enquiries = ApplicantService.getApplicantEnquiries(applicant);

        if (enquiries.isEmpty()) {
            System.out.println("You have no enquiries.");
            return;
        }

        for (Enquiry e : enquiries) {
            System.out.println("ID: " + e.getEnquiryId());
            System.out.println("Project: " + e.getProjectName());
            System.out.println("Message: " + e.getMessage());
            System.out.println();
        }
    }

    // Applicant: Edit enquiry
    private static void editEnquiry(Applicant applicant) {
        System.out.println("\n===== Edit Enquiry =====");
        List<Enquiry> enquiries = ApplicantService.getApplicantEnquiries(applicant);

        if (enquiries.isEmpty()) {
            System.out.println("You have no enquiries to edit.");
            return;
        }

        for (int i = 0; i < enquiries.size(); i++) {
            Enquiry e = enquiries.get(i);
            System.out.println((i + 1) + ". ID: " + e.getEnquiryId() + " - Project: " + e.getProjectName());
        }

        int enquiryChoice = getIntInput("Select enquiry to edit (0 to cancel): ");
        if (enquiryChoice == 0)
            return;

        if (enquiryChoice < 1 || enquiryChoice > enquiries.size()) {
            System.out.println("Invalid enquiry selection.");
            return;
        }

        Enquiry selectedEnquiry = enquiries.get(enquiryChoice - 1);
        System.out.println("Current message: " + selectedEnquiry.getMessage());

        System.out.print("Enter new message: ");
        String newMessage = scanner.nextLine();

        ApplicantService.editEnquiry(applicant, selectedEnquiry.getEnquiryId(), newMessage);
    }

    // Applicant: Delete enquiry
    private static void deleteEnquiry(Applicant applicant) {
        System.out.println("\n===== Delete Enquiry =====");
        List<Enquiry> enquiries = ApplicantService.getApplicantEnquiries(applicant);

        if (enquiries.isEmpty()) {
            System.out.println("You have no enquiries to delete.");
            return;
        }

        for (int i = 0; i < enquiries.size(); i++) {
            Enquiry e = enquiries.get(i);
            System.out.println((i + 1) + ". ID: " + e.getEnquiryId() + " - Project: " + e.getProjectName());
        }

        int enquiryChoice = getIntInput("Select enquiry to delete (0 to cancel): ");
        if (enquiryChoice == 0)
            return;

        if (enquiryChoice < 1 || enquiryChoice > enquiries.size()) {
            System.out.println("Invalid enquiry selection.");
            return;
        }

        Enquiry selectedEnquiry = enquiries.get(enquiryChoice - 1);
        ApplicantService.deleteEnquiry(applicant, selectedEnquiry.getEnquiryId());
    }

    // Manager: Create new project
    private static void createProject(HDBManager manager) {
        System.out.println("\n===== Create New Project =====");

        System.out.print("Project Name: ");
        String name = scanner.nextLine();

        System.out.print("Neighborhood: ");
        String neighborhood = scanner.nextLine();

        Date startDate = getDateInput("Start Date (dd/MM/yyyy): ");
        if (startDate == null)
            return;

        Date endDate = getDateInput("End Date (dd/MM/yyyy): ");
        if (endDate == null)
            return;

        if (startDate.after(endDate)) {
            System.out.println("Start date cannot be after end date.");
            return;
        }

        System.out.println("Flat Types:");
        System.out.println("1. 2-room");
        System.out.println("2. 3-room");
        System.out.println("3. Both");

        int flatTypeChoice = getIntInput("Select flat types: ");
        ArrayList<String> flatTypes = new ArrayList<>();

        switch (flatTypeChoice) {
            case 1:
                flatTypes.add("2-room");
                break;
            case 2:
                flatTypes.add("3-room");
                break;
            case 3:
                flatTypes.add("2-room");
                flatTypes.add("3-room");
                break;
            default:
                System.out.println("Invalid flat type selection.");
                return;
        }

        int twoRoomUnits = 0;
        int threeRoomUnits = 0;

        if (flatTypes.contains("2-room")) {
            twoRoomUnits = getIntInput("Number of 2-room units: ");
            if (twoRoomUnits < 0) {
                System.out.println("Number of units cannot be negative.");
                return;
            }
        }

        if (flatTypes.contains("3-room")) {
            threeRoomUnits = getIntInput("Number of 3-room units: ");
            if (threeRoomUnits < 0) {
                System.out.println("Number of units cannot be negative.");
                return;
            }
        }

        boolean success = HDBManagerService.createProject(manager, name, neighborhood, startDate, endDate, flatTypes,
                twoRoomUnits, threeRoomUnits);

        if (success) {
            System.out.println("Project created successfully!");
            allProjects = ProjectRepository.getAllProjects();
        } else {
            System.out.println("Failed to create project. Date overlap with existing projects.");
        }
    }

    // Manager: View all projects
    private static void viewAllProjects() {
        System.out.println("\n===== All Projects =====");
        ArrayList<BTOProject> projects = HDBManagerService.viewAllProjects();

        if (projects.isEmpty()) {
            System.out.println("No projects found.");
            return;
        }

        for (BTOProject project : projects) {
            displayProjectDetails(project);
        }
    }

    // Manager: View manager's projects
    private static void viewManagerProjects(HDBManager manager) {
        System.out.println("\n===== My Projects =====");
        ArrayList<BTOProject> projects = HDBManagerService.viewOwnProjects(manager);

        if (projects.isEmpty()) {
            System.out.println("You have no projects.");
            return;
        }

        for (BTOProject project : projects) {
            displayProjectDetails(project);
        }
    }

    // Manager: Edit project
    private static void editProject(HDBManager manager) {
        System.out.println("\n===== Edit Project =====");
        ArrayList<BTOProject> projects = HDBManagerService.viewOwnProjects(manager);

        if (projects.isEmpty()) {
            System.out.println("You have no projects to edit.");
            return;
        }

        System.out.println("Select project to edit:");
        for (int i = 0; i < projects.size(); i++) {
            System.out.println((i + 1) + ". " + projects.get(i).getProjectName());
        }

        int projectChoice = getIntInput("Enter project number (0 to cancel): ");
        if (projectChoice == 0)
            return;

        if (projectChoice < 1 || projectChoice > projects.size()) {
            System.out.println("Invalid project selection.");
            return;
        }

        BTOProject selectedProject = projects.get(projectChoice - 1);

        System.out.print("New Project Name (current: " + selectedProject.getProjectName() + "): ");
        String newName = scanner.nextLine();
        if (newName.trim().isEmpty()) {
            newName = selectedProject.getProjectName();
        }

        System.out.print("New Neighborhood (current: " + selectedProject.getNeighborhood() + "): ");
        String newNeighborhood = scanner.nextLine();
        if (newNeighborhood.trim().isEmpty()) {
            newNeighborhood = selectedProject.getNeighborhood();
        }

        System.out.print("New Start Date (current: " + dateFormat.format(selectedProject.getStartDate())
                + ", format dd/MM/yyyy): ");
        String startDateStr = scanner.nextLine();
        Date newStartDate = selectedProject.getStartDate();
        if (!startDateStr.trim().isEmpty()) {
            try {
                newStartDate = dateFormat.parse(startDateStr);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Using current start date.");
            }
        }

        System.out.print(
                "New End Date (current: " + dateFormat.format(selectedProject.getEndDate()) + ", format dd/MM/yyyy): ");
        String endDateStr = scanner.nextLine();
        Date newEndDate = selectedProject.getEndDate();
        if (!endDateStr.trim().isEmpty()) {
            try {
                newEndDate = dateFormat.parse(endDateStr);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Using current end date.");
            }
        }

        if (newStartDate.after(newEndDate)) {
            System.out.println("Start date cannot be after end date. Project not updated.");
            return;
        }

        int newTwoRoomUnits = selectedProject.getTwoRoomUnitsAvailable();
        if (selectedProject.getFlatTypes().contains("2-room")) {
            System.out.print(
                    "New number of 2-room units (current: " + selectedProject.getTwoRoomUnitsAvailable() + "): ");
            String twoRoomStr = scanner.nextLine();
            if (!twoRoomStr.trim().isEmpty()) {
                try {
                    newTwoRoomUnits = Integer.parseInt(twoRoomStr);
                    if (newTwoRoomUnits < 0) {
                        System.out.println("Number of units cannot be negative. Using current value.");
                        newTwoRoomUnits = selectedProject.getTwoRoomUnitsAvailable();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Using current value.");
                }
            }
        }

        int newThreeRoomUnits = selectedProject.getThreeRoomUnitsAvailable();
        if (selectedProject.getFlatTypes().contains("3-room")) {
            System.out.print(
                    "New number of 3-room units (current: " + selectedProject.getThreeRoomUnitsAvailable() + "): ");
            String threeRoomStr = scanner.nextLine();
            if (!threeRoomStr.trim().isEmpty()) {
                try {
                    newThreeRoomUnits = Integer.parseInt(threeRoomStr);
                    if (newThreeRoomUnits < 0) {
                        System.out.println("Number of units cannot be negative. Using current value.");
                        newThreeRoomUnits = selectedProject.getThreeRoomUnitsAvailable();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Using current value.");
                }
            }
        }

        HDBManagerService.editBTOProject(manager, selectedProject, newName, newNeighborhood, newStartDate, newEndDate,
                selectedProject.getFlatTypes(), newTwoRoomUnits, newThreeRoomUnits);

        System.out.println("Project updated successfully!");
    }

    // Manager: Toggle project visibility
    private static void toggleProjectVisibility(HDBManager manager) {
        System.out.println("\n===== Toggle Project Visibility =====");
        ArrayList<BTOProject> projects = HDBManagerService.viewOwnProjects(manager);

        if (projects.isEmpty()) {
            System.out.println("You have no projects.");
            return;
        }

        System.out.println("Select project to toggle visibility:");
        for (int i = 0; i < projects.size(); i++) {
            BTOProject p = projects.get(i);
            System.out.println((i + 1) + ". " + p.getProjectName() + " (Currently: "
                    + (p.isVisible() ? "Visible" : "Hidden") + ")");
        }

        int projectChoice = getIntInput("Enter project number (0 to cancel): ");
        if (projectChoice == 0)
            return;

        if (projectChoice < 1 || projectChoice > projects.size()) {
            System.out.println("Invalid project selection.");
            return;
        }

        BTOProject selectedProject = projects.get(projectChoice - 1);
        boolean newVisibility = !selectedProject.isVisible();
        HDBManagerService.toggleVisibility(manager, selectedProject, newVisibility);
        System.out.println("Project visibility toggled to: " + (newVisibility ? "Visible" : "Hidden"));
    }

    // Manager: Delete project
    private static void deleteProject(HDBManager manager) {
        System.out.println("\n===== Delete Project =====");
        ArrayList<BTOProject> projects = HDBManagerService.viewOwnProjects(manager);

        if (projects.isEmpty()) {
            System.out.println("You have no projects to delete.");
            return;
        }

        System.out.println("Select project to delete:");
        for (int i = 0; i < projects.size(); i++) {
            System.out.println((i + 1) + ". " + projects.get(i).getProjectName());
        }

        int projectChoice = getIntInput("Enter project number (0 to cancel): ");
        if (projectChoice == 0)
            return;

        if (projectChoice < 1 || projectChoice > projects.size()) {
            System.out.println("Invalid project selection.");
            return;
        }

        BTOProject selectedProject = projects.get(projectChoice - 1);

        System.out.print("Are you sure you want to delete this project? (Y/N): ");
        String confirmation = scanner.nextLine();
        if (confirmation.equalsIgnoreCase("Y")) {
            HDBManagerService.deleteBTOProject(manager, selectedProject);
            allProjects = ProjectRepository.getAllProjects();
            System.out.println("Project deleted successfully!");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    // Manager: Handle applications
    private static void handleApplications(HDBManager manager) {
        System.out.println("\n===== Handle Applications =====");
        ArrayList<BTOProject> projects = HDBManagerService.viewOwnProjects(manager);

        if (projects.isEmpty()) {
            System.out.println("You have no projects.");
            return;
        }

        System.out.println("Select project to handle applications:");
        for (int i = 0; i < projects.size(); i++) {
            System.out.println((i + 1) + ". " + projects.get(i).getProjectName());
        }

        int projectChoice = getIntInput("Enter project number (0 to cancel): ");
        if (projectChoice == 0)
            return;

        if (projectChoice < 1 || projectChoice > projects.size()) {
            System.out.println("Invalid project selection.");
            return;
        }

        BTOProject selectedProject = projects.get(projectChoice - 1);
        ArrayList<Application> applications = selectedProject.getApplications();

        if (applications.isEmpty()) {
            System.out.println("No applications for this project.");
            return;
        }

        System.out.println("Applications for " + selectedProject.getProjectName() + ":");
        for (int i = 0; i < applications.size(); i++) {
            Application app = applications.get(i);
            System.out.println((i + 1) + ". NRIC: " + app.getApplicant().getNRIC() +
                    " | Status: " + app.getStatus() +
                    " | Flat Type: " + app.getFlatType());
        }

        int appChoice = getIntInput("Select application to handle (0 to cancel): ");
        if (appChoice == 0)
            return;

        if (appChoice < 1 || appChoice > applications.size()) {
            System.out.println("Invalid application selection.");
            return;
        }

        Application selectedApp = applications.get(appChoice - 1);

        System.out.println("Actions:");
        System.out.println("1. Approve Application");
        System.out.println("2. Reject Application");
        System.out.println("3. Cancel");

        int actionChoice = getIntInput("Select action: ");

        switch (actionChoice) {
            case 1:
                if (selectedApp.getStatus().equalsIgnoreCase("Pending")) {
                    if (HDBManagerService.handleBTOApplication(manager, selectedApp)) {
                        selectedApp.setStatus("Successful");

                        String flatType = selectedApp.getFlatType();
                        int currentUnits = selectedProject.getUnits(flatType);
                        selectedProject.setUnits(flatType, currentUnits - 1);

                        System.out.println("Application approved successfully!");
                    } else {
                        System.out.println(
                                "Failed to approve application. No available units or insufficient permissions.");
                    }
                } else {
                    System.out.println("Cannot approve application in current status: " + selectedApp.getStatus());
                }
                break;
            case 2:
                if (selectedApp.getStatus().equalsIgnoreCase("Pending") ||
                        selectedApp.getStatus().equalsIgnoreCase("Successful")) {

                    selectedApp.setStatus("Unsuccessful");
                    System.out.println("Application rejected successfully!");
                } else {
                    System.out.println("Cannot reject application in current status: " + selectedApp.getStatus());
                }
                break;
            case 3:
                System.out.println("Operation cancelled.");
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    // Manager: Generate booking report
    private static void generateBookingReport(HDBManager manager) {
        System.out.println("\n===== Booking Report =====");
        System.out.println("Select flat type filter:");
        System.out.println("1. 2-room");
        System.out.println("2. 3-room");
        System.out.println("3. All");

        int filterChoice = getIntInput("Enter your choice: ");

        switch (filterChoice) {
            case 1:
                System.out.println("\n===== Booking Report for 2-room Flats =====");
                HDBManagerService.bookingReport(manager, "2-room");
                break;
            case 2:
                System.out.println("\n===== Booking Report for 3-room Flats =====");
                HDBManagerService.bookingReport(manager, "3-room");
                break;
            case 3:
                System.out.println("\n===== Booking Report for All Flats =====");
                System.out.println("2-room Flats:");
                HDBManagerService.bookingReport(manager, "2-room");
                System.out.println("\n3-room Flats:");
                HDBManagerService.bookingReport(manager, "3-room");
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    // Utility method to display project details
    private static void displayProjectDetails(BTOProject project) {
        System.out.println("Project Name: " + project.getProjectName());
        System.out.println("Neighborhood: " + project.getNeighborhood());
        System.out.println("Application Period: " + dateFormat.format(project.getStartDate()) +
                " to " + dateFormat.format(project.getEndDate()));
        System.out.println("Visibility: " + (project.isVisible() ? "Visible" : "Hidden"));
        System.out.println("Manager: " + project.getManager().getName());

        if (project.getFlatTypes().contains("2-room")) {
            System.out.println("2-room units available: " + project.getTwoRoomUnitsAvailable());
        }

        if (project.getFlatTypes().contains("3-room")) {
            System.out.println("3-room units available: " + project.getThreeRoomUnitsAvailable());
        }

        System.out.println();
    }

    // Utility method to get integer input
    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Utility method to get date input
    private static Date getDateInput(String prompt) {
        System.out.print(prompt);
        String dateStr = scanner.nextLine();
        try {
            return dateFormat.parse(dateStr);
        } catch (java.text.ParseException e) {
            System.out.println("Invalid date format. Please use dd/MM/yyyy.");
            return null;
        }
    }
}
