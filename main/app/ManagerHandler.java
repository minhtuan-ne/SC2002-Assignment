package main.app;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import main.models.*;
import main.services.*;

public class ManagerHandler implements IUserHandler {
    private final HDBManagerService managerService;
    private final EnquiryService enquiryService;
    private final RegistrationService registrationService;

    public ManagerHandler(HDBManagerService managerService, EnquiryService enquiryService, RegistrationService registrationService) {
        this.managerService = managerService;
        this.enquiryService = enquiryService;
        this.registrationService = registrationService;
    }

    @Override
    public void run(User user, Scanner sc) {
        runManagerLoop((HDBManager) user, managerService, registrationService, sc, enquiryService);
    }

    public void runManagerLoop(HDBManager me, HDBManagerService svc, RegistrationService rsvc, Scanner sc, EnquiryService iesvc) {
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
                    
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        Date startDate = Date.from(LocalDate.parse(startDateStr, fmt)
                                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                        Date endDate = Date.from(LocalDate.parse(endDateStr, fmt)
                                     .atStartOfDay(ZoneId.systemDefault()).toInstant());
                        
                        System.out.print("Number of 2-room units: ");
                        int twoRoomUnits = Integer.parseInt(sc.nextLine());
                        System.out.print("Number of 3-room units: ");
                        int threeRoomUnits = Integer.parseInt(sc.nextLine());
                        
                        List<String> flatTypes = List.of("2-room", "3-room");
                        boolean success = svc.createProject(me, name, neighborhood, startDate, 
                                             endDate, flatTypes, twoRoomUnits, threeRoomUnits);
                        
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
                    List<BTOProject> allProjects = svc.viewAllProjects();
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
                                p.getTwoRoomUnitsAvailable(), p.getThreeRoomUnitsAvailable());
                        }
                    }
                    break;
                }
                
                case "3": { // View my projects
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                                p.getTwoRoomUnitsAvailable(), p.getThreeRoomUnitsAvailable());
                        }
                    }
                    break;
                }
                
                case "4": { // Edit project
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                    
                    System.out.print("New project name (press Enter to keep current): ");
                    String name = sc.nextLine();
                    if (name.isEmpty()) name = selected.getProjectName();
                    
                    System.out.print("New neighborhood (press Enter to keep current): ");
                    String neighborhood = sc.nextLine();
                    if (neighborhood.isEmpty()) neighborhood = selected.getNeighborhood();
                    
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
                    
                    System.out.print("New 2-room units (press Enter to keep current): ");
                    String twoRoomStr = sc.nextLine();
                    int twoRoomUnits = selected.getTwoRoomUnitsAvailable();
                    if (!twoRoomStr.isEmpty()) twoRoomUnits = Integer.parseInt(twoRoomStr);
                    
                    System.out.print("New 3-room units (press Enter to keep current): ");
                    String threeRoomStr = sc.nextLine();
                    int threeRoomUnits = selected.getThreeRoomUnitsAvailable();
                    if (!threeRoomStr.isEmpty()) threeRoomUnits = Integer.parseInt(threeRoomStr);
                    
                    svc.editBTOProject(me, selected, name, neighborhood, startDate, endDate, 
                                      List.of("2-room", "3-room"), twoRoomUnits, threeRoomUnits);
                    System.out.println("Project updated successfully.");
                    break;
                }
                
                case "5": { // Delete project
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                        svc.deleteBTOProject(me, selected);
                        System.out.println("Project deleted successfully.");
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;
                }
                
                case "6": { // Toggle project visibility
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                    boolean newVisibility = !selected.isVisible();
                    svc.toggleVisibility(me, selected, newVisibility);
                    System.out.printf("Project is now %s.%n", newVisibility ? "visible" : "hidden");
                    break;
                }

                case "7": {
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
                    if (myProjects.isEmpty()) {
                        System.out.println("You have no projects.");
                        break;
                    }

                    for (BTOProject project : myProjects) {
                        System.out.println("\nProject: " + project.getProjectName());
                        System.out.println("Assigned Officers:");

                        List<HDBOfficer> assigned = project.getHDBOfficers();
                        if (assigned.isEmpty()) {
                            System.out.println("  (none)");
                        } else {
                            for (HDBOfficer o : assigned) {
                                System.out.printf("  - %s (NRIC: %s)\n", o.getName(), o.getNRIC());
                            }
                        }

                        System.out.println("Pending Registrations:");
                        List<Registration> pending = registrationService.getRegistration(); 
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

                case "8": {
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                    List<Registration> pending = registrationService.getRegistration();

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
                    boolean success = rsvc.handleOfficerRegistration(me, project, selectedOfficer);
                    if (success) {
                        System.out.println("Officer approved successfully.");
                    } else {
                        System.out.println("Approval failed.");
                    }
                    break;
                }
                
                case "9": { // Process BTO application
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                    List<Application> existingApps = selected.getApplications();
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
                        boolean success = svc.handleBTOApplication(me, applicationToProcess, approve);
                        
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

                // Case 10: Process withdrawal request - Already works, just minor improvements
                case "10": { // Process withdrawal request
                    List<BTOProject> myProjects = svc.viewOwnProjects(me);
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
                    List<Application> applications = selected.getApplications();
                    
                    // Display existing applications
                    System.out.println("\n===== Current Applications =====");
                    if (applications.isEmpty()) {
                        System.out.println("No applications found for this project.");
                        break;
                    }
                    
                    int count = 0;
                    for (Application app : applications) {
                        if ("Successful".equals(app.getStatus()) || "Pending".equals(app.getStatus())) {
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
                            ("Successful".equals(app.getStatus()) || "Pending".equals(app.getStatus()))) {
                            foundApp = app;
                            break;
                        }
                    }
                    
                    if (foundApp == null) {
                        System.out.println("No eligible application found for this NRIC.");
                        break;
                    }
                    
                    // Process the withdrawal
                    svc.handleWithdrawal(me, foundApp);
                    System.out.println("Withdrawal processed successfully.");
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
                    svc.bookingReport(me, filter);
                    break;
                }
                
                case "12": { // View all enquiries
                    System.out.println("\n===== All Enquiries =====");
                    
                    // Use the same enquiryService that was declared in main()
                    List<Enquiry> allEnquiries = iesvc.getAllEnquiries();
                    
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
                
                // Case 13: Reply to enquiry - Modified to show enquiries first
                case "13": { // Reply to enquiry
                    System.out.println("\n===== All Enquiries =====");
                    
                    List<Enquiry> allEnquiries = iesvc.getAllEnquiries();
                    
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
                    
                    System.out.print("Reply message: ");
                    String reply = sc.nextLine();
                    
                    iesvc.replyToEnquiry(selected.getEnquiryId(), reply);
                    break;
                }
                case "14": {
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