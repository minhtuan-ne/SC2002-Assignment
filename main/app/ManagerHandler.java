package main.app;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import main.models.*;
import main.services.*;
import main.util.FileManager;

/**
 * Handles all CLI interactions for an HDB Manager.
 * Provides access to create, manage, and moderate BTO projects and registrations.
 */
public class ManagerHandler implements IUserHandler {
    private final HDBManagerService managerSvc;
    private final EnquiryService enquirySvc;
    private final RegistrationService registrationSvc;
    private final ProjectService projectSvc;
    private final FileManager fileManager;

    /**
     * Constructs a new ManagerHandler instance.
     *
     * @param managerService     manager-related logic service
     * @param enquiryService     enquiry processing service
     * @param registrationService officer registration handler
     * @param projectService     project management service
     * @param fileManager        file operations handler
     */
    public ManagerHandler(HDBManagerService managerService, EnquiryService enquiryService, RegistrationService registrationService, ProjectService projectService, FileManager fileManager) {
        this.managerSvc = managerService;
        this.enquirySvc = enquiryService;
        this.registrationSvc = registrationService;
        this.projectSvc = projectService;
        this.fileManager = fileManager;
    }
    
    /**
     * Launches the manager's CLI interface loop.
     *
     * @param user the logged-in HDB manager
     * @param sc   scanner for user input
     */
    @Override
    public void run(User user, Scanner sc) {
        runManagerLoop((HDBManager) user, sc);
    }
    
    /**
     * Contains full logic for HDB Manager command handling.
     *
     * @param me the current logged-in HDB manager
     * @param sc scanner for console input
     */
    public void runManagerLoop(HDBManager me, Scanner sc) {
        while (true) {
            System.out.println("\n-- Manager Menu --");
            System.out.println("1) Create project");
            System.out.println("2) View all projects");
            System.out.println("3) View my projects");
            System.out.println("4) Edit project");
            System.out.println("5) Delete project");
            System.out.println("6) Toggle project visibility");
            System.out.println("7) View pending officer registrations");
            System.out.println("8) Approve/reject officer registration");
            System.out.println("9) Process BTO application");
            System.out.println("10) Process withdrawal request");
            System.out.println("11) Generate booking report");
            System.out.println("12) View all enquiries");
            System.out.println("13) Reply to enquiry");
            System.out.println("14) Change password");
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": { // Create project

                    System.out.print("Project name: ");
                    String name = sc.nextLine();
                    System.out.print("Neighborhood: ");
                    String neighborhood = sc.nextLine();
                    
                    System.out.print("Start date (dd/MM/yyyy): ");
                    String startDateStr = sc.nextLine();
                    System.out.print("End date (dd/MM/yyyy): ");
                    String endDateStr = sc.nextLine();
                    
                    // Check overlapping time
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate startLocal = LocalDate.parse(startDateStr, fmt);
                    LocalDate endLocal = LocalDate.parse(endDateStr, fmt);
                    List<BTOProject> createdProjects = managerSvc.viewOwnProjects(me);
                    boolean overlap = false;
                    for (BTOProject project : createdProjects) {
                        LocalDate existingStart = project.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate existingEnd = project.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                        if (!(endLocal.isBefore(existingStart) || startLocal.isAfter(existingEnd))) {
                            if(project.isVisible()){
                                System.out.printf("Project overlaps with: %s (%s to %s)\n", 
                                    project.getProjectName(),
                                    existingStart.format(fmt),
                                    existingEnd.format(fmt)
                                );
                                overlap = true;
                                break;
                            }
                        }
                    }

                    if (overlap) {
                        System.out.println("Cannot create project. The date range overlaps with another project.");
                        break;
                    }
                    // Check whether the manager can create the project or not (overlapping time, still has visible project, etc.)
                    try {
                        Date startDate = Date.from(LocalDate.parse(startDateStr, fmt)
                                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                        Date endDate = Date.from(LocalDate.parse(endDateStr, fmt)
                                     .atStartOfDay(ZoneId.systemDefault()).toInstant());                        
                        System.out.print("Number of 2-room units: ");
                        int twoRoomUnits = Integer.parseInt(sc.nextLine());
                        System.out.print("Number of 3-room units: ");
                        int threeRoomUnits = Integer.parseInt(sc.nextLine());
                        boolean success = managerSvc.createProject(me, name, neighborhood, startDate, endDate, twoRoomUnits, threeRoomUnits);
                        
                        if (success) {
                            System.out.println("Project created successfully.");
                        } else {
                            System.out.println("Failed to create project. You still have a visible project, or the dates are wrong.");
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                }
                
                case "2": { // View all projects
                    List<BTOProject> allProjects = managerSvc.viewAllProjects();
                    System.out.println("\n====== All Projects ======");
                    if (allProjects.isEmpty()) {
                        System.out.println("No projects found.");
                    } else {
                        for (BTOProject p : allProjects) {
                            System.out.printf("%s - %s (Manager: %s)%n", 
                                p.getProjectName(), p.getNeighborhood(), p.getManager().getName());
                            String start = p.getStartDate().toInstant()
                                           .atZone(ZoneId.systemDefault())
                                           .toLocalDate().format(DISPLAY_FMT);
                            String end = p.getEndDate().toInstant()
                                         .atZone(ZoneId.systemDefault())
                                         .toLocalDate().format(DISPLAY_FMT);
                            System.out.printf("  Period: %s to %s, Visible: %s%n", 
                                start, end, p.isVisible() ? "Yes" : "No");
                            System.out.printf("  Units - 2-room: %d, 3-room: %d%n", 
                                p.getUnits("2-room"), p.getUnits("3-room"));
                        }
                    }
                    break;
                }
                
                case "3": { // View my projects
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    System.out.println("\n====== My Projects ======");
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects.");
                    } else {
                        for (int i = 0; i < myProjects.size(); i++) {
                            BTOProject p = myProjects.get(i);
                            System.out.printf("%d. %s - %s%n", i + 1, p.getProjectName(), p.getNeighborhood());
                            String start = p.getStartDate().toInstant()
                                           .atZone(ZoneId.systemDefault())
                                           .toLocalDate().format(DISPLAY_FMT);
                            String end = p.getEndDate().toInstant()
                                         .atZone(ZoneId.systemDefault())
                                         .toLocalDate().format(DISPLAY_FMT);
                            System.out.printf("   Period: %s to %s, Visible: %s%n", 
                                start, end, p.isVisible() ? "Yes" : "No");
                            System.out.printf("   Units - 2-room: %d, 3-room: %d%n", 
                                p.getUnits("2-room"), p.getUnits("3-room"));
                            
                            List<HDBOfficer> officers = p.getOfficers();
                            System.out.println("   Assigned Officers:");
                            if (officers.isEmpty()) {
                                System.out.println("      (none)");
                            } else {
                                for (HDBOfficer o : officers) {
                                    System.out.printf("      - %s (NRIC: %s, Status: %s)%n", 
                                        o.getName(), o.getNRIC(), o.getRegStatus());
                                }
                            }
                        }
                    }
                    break;
                }
                
                case "4": { // Edit project
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects to edit.");
                        break;
                    }
                    
                    System.out.println("Select a project to edit:");
                    for (int i = 0; i < myProjects.size(); i++) {
                        System.out.printf("%d) %s%n", i + 1, myProjects.get(i).getProjectName());
                    }
                    
                    System.out.print("> ");
                    int projChoice = Integer.parseInt(sc.nextLine());
                    if (projChoice < 1 || projChoice > myProjects.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    
                    BTOProject selected = myProjects.get(projChoice - 1);

                    // Edit name
                    System.out.print("New project name (press Enter to keep current): ");
                    String name = sc.nextLine();
                    if (name.isEmpty()) name = selected.getProjectName();
                    
                    // Edit location
                    System.out.print("New neighborhood (press Enter to keep current): ");
                    String neighborhood = sc.nextLine();
                    if (neighborhood.isEmpty()) neighborhood = selected.getNeighborhood();

                    // Edit date
                    System.out.print("New start date (dd/MM/yyyy, press Enter to keep current): ");
                    String startDateStr = sc.nextLine();
                    Date startDate = selected.getStartDate();
                    Date endDate = selected.getEndDate();
                    
                    if (!startDateStr.isEmpty()) {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        startDate = Date.from(LocalDate.parse(startDateStr, fmt)
                                  .atStartOfDay(ZoneId.systemDefault()).toInstant());
                                  
                        System.out.print("New end date (dd/MM/yyyy, press Enter to keep current): ");
                        String endDateStr = sc.nextLine();
                        
                        // Fix: Only parse the end date if it's not empty
                        if (!endDateStr.isEmpty()) {
                            endDate = Date.from(LocalDate.parse(endDateStr, fmt)
                                    .atStartOfDay(ZoneId.systemDefault()).toInstant());
                        }
                        // If endDateStr is empty, we'll keep using the original endDate
                    }
                    
                    // Edit units of each room
                    System.out.print("New 2-room units (press Enter to keep current): ");
                    String twoRoomStr = sc.nextLine();
                    int twoRoomUnits = selected.getUnits("2-room");
                    if (!twoRoomStr.isEmpty()) twoRoomUnits = Integer.parseInt(twoRoomStr);
                    
                    System.out.print("New 3-room units (press Enter to keep current): ");
                    String threeRoomStr = sc.nextLine();
                    int threeRoomUnits = selected.getUnits("3-room");
                    if (!threeRoomStr.isEmpty()) threeRoomUnits = Integer.parseInt(threeRoomStr);
                    
                    managerSvc.editBTOProject(me, selected, name, neighborhood, startDate, endDate, twoRoomUnits, threeRoomUnits);
                    System.out.println("Project updated successfully.");
                    break;
                }
                
                case "5": { // Delete project
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects to delete.");
                        break;
                    }
                    
                    System.out.println("Select a project to delete:");
                    for (int i = 0; i < myProjects.size(); i++) {
                        System.out.printf("%d) %s%n", i + 1, myProjects.get(i).getProjectName());
                    }
                    
                    System.out.print("> ");
                    int projChoice = Integer.parseInt(sc.nextLine());
                    if (projChoice < 1 || projChoice > myProjects.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    
                    BTOProject selected = myProjects.get(projChoice - 1);
                    System.out.print("Are you sure you want to delete this project? (y/n): ");
                    String confirm = sc.nextLine().toLowerCase();
                    if (confirm.equals("y")) {
                        managerSvc.deleteBTOProject(me, selected);
                        System.out.println("Project deleted successfully.");
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;
                }
                
                case "6": { // Toggle project visibility
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects to toggle visibility.");
                        break;
                    }
                    
                    System.out.println("Select a project to toggle visibility:");
                    for (int i = 0; i < myProjects.size(); i++) {
                        System.out.printf("%d) %s (currently %s)%n", i + 1, 
                                myProjects.get(i).getProjectName(), 
                                myProjects.get(i).isVisible() ? "visible" : "hidden");
                    }
                    
                    System.out.print("> ");
                    int projChoice = Integer.parseInt(sc.nextLine());
                    if (projChoice < 1 || projChoice > myProjects.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    
                    BTOProject selected = myProjects.get(projChoice - 1);

                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate startLocal = selected.getStartDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();                        
                    LocalDate endLocal = selected.getEndDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();        
                    List<BTOProject> createdProjects = managerSvc.viewOwnProjects(me);
                    boolean overlap = false;
                    for (BTOProject project : createdProjects) {
                        LocalDate existingStart = project.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate existingEnd = project.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                        if (!(endLocal.isBefore(existingStart) || startLocal.isAfter(existingEnd))) {
                            if(project.isVisible()){
                                System.out.printf("Project overlaps with: %s (%s to %s)\n", 
                                    project.getProjectName(),
                                    existingStart.format(fmt),
                                    existingEnd.format(fmt)
                                );
                                overlap = true;
                                break;
                            }
                        }
                    }

                    if (overlap) {
                        System.out.println("Cannot update project visibility. The date range overlaps with another project.");
                        break;
                    }

                    boolean newVisibility = !selected.isVisible();
                    managerSvc.toggleVisibility(me, selected, newVisibility);
                    try {
                        fileManager.updateProjectVisibility(newVisibility);
                    } catch (Exception e) {}
                    System.out.printf("Project is now %s.%n", newVisibility ? "visible" : "hidden");
                    break;
                }

                case "7": { // View pending officer registrations
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects.");
                        break;
                    }

                    for (BTOProject project : myProjects) {
                        System.out.println("\nProject: " + project.getProjectName());
                        System.out.println("Assigned Officers:");

                        List<HDBOfficer> assigned = project.getOfficers();
                        if (assigned.isEmpty()) {
                            System.out.println("  (none)");
                        } else {
                            for (HDBOfficer o : assigned) {
                                System.out.printf("  - %s (NRIC: %s)\n", o.getName(), o.getNRIC());
                            }
                        }

                        System.out.println("Pending Registrations:");
                        List<Registration> pending = registrationSvc.getRegistration().stream()
                            .filter(r -> r.getProject() != null && r.getProject().getProjectName().equals(project.getProjectName()) && r.getStatus().equals(HDBOfficer.RegistrationStatus.PENDING))
                            .collect(Collectors.toList());
                        if (pending.isEmpty()) {
                            System.out.println("  (none)");
                        } else {
                            for (Registration o : pending) {
                                System.out.printf("  - %s (NRIC: %s)\n", o.getOfficer().getName(), o.getOfficer().getNRIC());
                            }
                        }
                    }
                    break;
                }

                case "8": { // Manage officer registration
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects to manage.");
                        break;
                    }

                    System.out.println("Select a project:");
                    for (int i = 0; i < myProjects.size(); i++) {
                        System.out.printf("%d) %s\n", i + 1, myProjects.get(i).getProjectName());
                    }

                    System.out.print("> ");
                    int projChoice = Integer.parseInt(sc.nextLine());
                    if (projChoice < 1 || projChoice > myProjects.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }

                    BTOProject project = myProjects.get(projChoice - 1);
                    List<Registration> pending = registrationSvc.getRegistration().stream()
                            .filter(r -> r.getProject() != null && r.getProject().getProjectName().equals(project.getProjectName()) && r.getStatus().equals(HDBOfficer.RegistrationStatus.PENDING))
                            .collect(Collectors.toList());

                    if (pending.isEmpty()) {
                        System.out.println("No pending officer registrations.");
                        break;
                    }

                    System.out.println("Pending officers:");
                    for (int i = 0; i < pending.size(); i++) {
                        Registration o = pending.get(i);
                        System.out.printf("%d) %s (NRIC: %s)\n", i + 1, o.getOfficer().getName(), o.getOfficer().getNRIC());
                    }

                    System.out.print("Select officer to approve (0 to cancel): ");
                    int officerChoice = Integer.parseInt(sc.nextLine());
                    if (officerChoice == 0) {
                        System.out.println("Approval cancelled.");
                        break;
                    }
                    if (officerChoice < 1 || officerChoice > pending.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }

                    HDBOfficer selectedOfficer = pending.get(officerChoice - 1).getOfficer();
                    boolean success = registrationSvc.handleOfficerRegistration(me, project, selectedOfficer);
                    boolean assignSuccess = managerSvc.assignOfficerToProject(me, project, selectedOfficer);
                    if (success && assignSuccess) {
                        System.out.println("Officer approved successfully.");
                    } else {
                        System.out.println("Approval failed.");
                    }
                    break;
                }
                
                case "9": { // Process BTO application
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects to process applications for.");
                        break;
                    }
                    
                    System.out.println("\nSelect a project:");
                    for (int i = 0; i < myProjects.size(); i++) {
                        System.out.printf("%d) %s%n", i + 1, myProjects.get(i).getProjectName());
                    }
                    
                    System.out.print("> ");
                    int projChoice = Integer.parseInt(sc.nextLine());
                    if (projChoice < 1 || projChoice > myProjects.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    
                    BTOProject selected = myProjects.get(projChoice - 1);
                    
                    // Show existing applications for this project
                    List<Application> existingApps = projectSvc.getApplicationByProject(selected);
                    System.out.println("\n===== Pending Applications =====");
                    if (existingApps.isEmpty()) {
                        System.out.println("No applications found for this project.");
                    } else {
                        int appCount = 0;
                        for (Application app : existingApps) {
                            if (app.getStatus().equalsIgnoreCase("Pending")) {
                                appCount++;
                                System.out.printf("%d) NRIC: %s, Name: %s, Flat Type: %s%n",
                                    appCount, 
                                    app.getApplicant().getNRIC(),
                                    app.getApplicant().getName(),
                                    app.getFlatType());
                            }
                        }
                        
                        if (appCount == 0) {
                            System.out.println("No pending applications to process.");
                            break;
                        }
                        
                        System.out.print("\nEnter applicant NRIC to process: ");
                        String nric = sc.nextLine();
                        
                        // Find the application
                        Application applicationToProcess = null;
                        for (Application app : existingApps) {
                            if (app.getApplicant().getNRIC().equalsIgnoreCase(nric) && 
                                app.getStatus().equalsIgnoreCase("Pending")) {
                                applicationToProcess = app;
                                break;
                            }
                        }
                    
                        if (applicationToProcess == null) {
                            System.out.println("Application not found with that NRIC or not in Pending status.");
                            break;
                        }
                        
                        System.out.println("\nDo you want to approve or reject this application?");
                        System.out.println("1) Approve");
                        System.out.println("2) Reject");
                        System.out.print("> ");
                        int choice9 = Integer.parseInt(sc.nextLine());
                        
                        boolean approve = (choice9 == 1);
                        boolean success = managerSvc.handleBTOApplication(me, applicationToProcess, approve);
                        
                        if (success) {
                            if (approve) {
                                System.out.println("Application approved successfully.");
                            } else {
                                System.out.println("Application rejected successfully.");
                            }
                        } else {
                            if (approve) {
                                System.out.println("Failed to approve application. No available units?");
                            } else {
                                System.out.println("Failed to reject application.");
                            }
                        }
                        break;
                    }
                }

                case "10": { // Process withdrawal request
                    List<BTOProject> myProjects = managerSvc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects to process withdrawal requests for.");
                        break;
                    }
                    
                    System.out.println("\nSelect a project:");
                    for (int i = 0; i < myProjects.size(); i++) {
                        System.out.printf("%d) %s%n", i + 1, myProjects.get(i).getProjectName());
                    }
                    
                    System.out.print("> ");
                    int projChoice = Integer.parseInt(sc.nextLine());
                    if (projChoice < 1 || projChoice > myProjects.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    
                    BTOProject selected = myProjects.get(projChoice - 1);

                    // Get applications from the project
                    List<Application> applications =  projectSvc.getApplicationByProject(selected);
                    
                    // Display existing applications
                    System.out.println("\n===== Current Applications =====");
                    if (applications.isEmpty()) {
                        System.out.println("No applications found for this project.");
                        break;
                    }
                    
                    int count = 0;
                    for (Application app : applications) {
                        if ("Withdrawing".equals(app.getStatus())) {
                            count++;
                            System.out.printf("%d) NRIC: %s, Name: %s, Status: %s, Flat Type: %s%n", count,
                                app.getApplicant().getNRIC(),
                                app.getApplicant().getName(),
                                app.getStatus(),
                                app.getFlatType());
                        }
                    }
                    
                    if (count == 0) {
                        System.out.println("No applications eligible for withdrawal.");
                        break;
                    }
                    
                    System.out.print("\nEnter applicant NRIC to process withdrawal: ");
                    String nric = sc.nextLine();
                    
                    // Find the application
                    Application foundApp = null;
                    for (Application app : applications) {
                        if (app.getApplicant().getNRIC().equalsIgnoreCase(nric) && 
                            ("Withdrawing".equals(app.getStatus()))) {
                            foundApp = app;
                            break;
                        }
                    }
                    
                    if (foundApp == null) {
                        System.out.println("No eligible application found for this NRIC.");
                        break;
                    }
                    
                    System.out.print("Do you approve this withdrawal? (yes/no): ");
                    String approve = sc.nextLine();

                    // Process the withdrawal
                    if(approve.equals("yes")){
                        if(managerSvc.handleWithdrawal(me, foundApp)){
                            System.out.println("Withdrawal processed successfully.");                        
                        }
                        else{
                            System.out.println("Error while withdrawing.");
                        }    
                    }
                    else{
                        foundApp.setStatus(foundApp.getPrevStatus());
                        foundApp.setPrevStatus("null");
                        fileManager.updateApplication(nric, foundApp);
                    }
                    break;
                }
                
                case "11": { // Generate booking report
                    System.out.print("Filter by flat type (2-room/3-room): ");
                    String filter = sc.nextLine().trim();
                    
                    if (!filter.equalsIgnoreCase("2-room") && !filter.equalsIgnoreCase("3-room")) {
                        System.out.println("Invalid flat type. Please use '2-room' or '3-room'.");
                        break;
                    }
                    
                    System.out.println("\n===== Booking Report - " + filter + " =====");
                    managerSvc.bookingReport(me, filter);
                    break;
                }
                
                case "12": { // View all enquiries
                    System.out.println("\n===== All Enquiries =====");
                    
                    // Use the same enquiryService that was declared in main()
                    List<Enquiry> allEnquiries = enquirySvc.getAllEnquiries();
                    
                    if (allEnquiries.isEmpty()) {
                        System.out.println("No enquiries found.");
                    } else {
                        for (Enquiry e : allEnquiries) {
                            System.out.printf("ID: %s, Project: %s%n", e.getEnquiryId(), e.getProjectName());
                            System.out.printf("From: %s%n", e.getUserNric());
                            System.out.printf("Message: %s%n%n", e.getMessage());
                        }
                    }
                    break;
                }
                
                case "13": { // Reply to enquiry
                    System.out.println("\n===== All Enquiries =====");
                    
                    List<Enquiry> allEnquiries = enquirySvc.getAllEnquiries();
                    
                    if (allEnquiries.isEmpty()) {
                        System.out.println("No enquiries to reply to.");
                        break;
                    }
                    
                    // Display all enquiries with ID numbers
                    for (int i = 0; i < allEnquiries.size(); i++) {
                        Enquiry e = allEnquiries.get(i);
                        System.out.printf("%d) ID: %s, Project: %s%n", i + 1, e.getEnquiryId(), e.getProjectName());
                        System.out.printf("   From: %s%n", e.getUserNric());
                        System.out.printf("   Message: %s%n%n", e.getMessage());
                    }
                    
                    System.out.print("Select enquiry to reply to (enter number): ");
                    int choice13 = Integer.parseInt(sc.nextLine());
                    
                    if (choice13 < 1 || choice13 > allEnquiries.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    
                    Enquiry selected = allEnquiries.get(choice13 - 1);
                    
                    // Reply
                    System.out.print("Reply message: ");
                    String reply = sc.nextLine();
                    
                    enquirySvc.replyToEnquiry(selected.getEnquiryId(), reply);
                    break;
                }
                case "14": { // Change password
                    System.out.print("Current password: ");
                    String oldP = sc.nextLine();
                    System.out.print("New password: ");
                    String newP = sc.nextLine();
                    me.changePassword(oldP, newP);
                    break;
                }

                case "0":
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}