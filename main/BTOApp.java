package main;

import main.models.*;
import main.util.*;
import main.repositories.*;
import main.services.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BTOApp {

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) {
        // 1) Core infra
        IFileManager fileManager       = new FileManager();
        Authenticator auth             = new Authenticator(fileManager);
        IProjectRepository projectRepo = new ProjectRepository();

        // 2) Preload projects from data/ProjectList.txt
        try {
            preloadProjects(fileManager, projectRepo);
        } catch (IOException ex) {
            System.err.println("ERROR loading ProjectList.txt: " + ex.getMessage());
        }

        // 3) Services
        IHDBManagerService  managerSvc   = new HDBManagerService(projectRepo, fileManager);
        IEnquiryService enquirySvc    = new EnquiryService();
        IApplicantService applicantSvc = new ApplicantService(fileManager, enquirySvc);
        IHDBOfficerService officerSvc =
                new HDBOfficerService((ProjectRepository) projectRepo, (EnquiryService) enquirySvc, (ApplicantService) applicantSvc);

        // 4) Main login/logout loop
        Scanner sc = new Scanner(System.in);
        MAIN_LOOP:
        while (true) {
            System.out.println("\n=== BTO Application System ===");
            System.out.println("1) Login");
            System.out.println("0) Exit");
            System.out.print("> ");
            String mainChoice = sc.nextLine().trim();

            switch (mainChoice) {
                case "1":
                    User user = auth.logIn();
                    String role = user.getRole().toLowerCase();
                    switch (role) {
                        case "applicant":
                            runApplicantLoop((Applicant) user, applicantSvc, enquirySvc, projectRepo.getAllProjects(), sc);
                            break;
                        case "hdb manager":
                            runManagerLoop((HDBManager) user, managerSvc, projectRepo, sc, enquirySvc);
                            break;
                        case "hdbofficer":
                            runOfficerLoop((HDBOfficer) user,
                                    officerSvc,
                                    applicantSvc,
                                    enquirySvc,
                                    projectRepo.getAllProjects(),
                                    sc);
                            break;
                        default:
                            System.out.println("Unknown role: " + user.getRole());
                    }
                    break;
                case "0":
                    System.out.println("Thank you for using the BTO System. Goodbye!");
                    break MAIN_LOOP;
                default:
                    System.out.println("Invalid choice, try again.");
            }
        }

        sc.close();
    }

    /**
     * Loads data/ProjectList.txt (tab‑separated), skips header,
     * parses each row, finds matching manager by name, creates
     * BTOProject and adds to projectRepo and to the manager.
     */
    private static void preloadProjects(IFileManager fileManager,
                                        IProjectRepository projectRepo) throws IOException {
        Path path = Paths.get("data", "ProjectList.txt");
        List<String> lines = Files.readAllLines(path).stream()
            .filter(l -> !l.startsWith("Project Name") && !l.isBlank())
            .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Map<String, List<List<String>>> userData = fileManager.getDatabyRole();
        List<List<String>> mgrRows = userData.getOrDefault("Manager", Collections.emptyList());

        for (String line : lines) {
            String[] cols = line.split("\\t");

            String projName    = cols[0];
            String neighborhood= cols[1];
            String t1          = cols[2];
            int    u1          = Integer.parseInt(cols[3]);
            String t2          = cols[5];
            int    u2          = Integer.parseInt(cols[6]);
            int    maxOfficers = Integer.parseInt(cols[11]);

            LocalDate sLd = LocalDate.parse(cols[8], fmt);
            LocalDate eLd = LocalDate.parse(cols[9], fmt);
            Date sd = Date.from(sLd.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date ed = Date.from(eLd.atStartOfDay(ZoneId.systemDefault()).toInstant());

            String mgrName = cols[10];
            Optional<List<String>> maybeMgr = mgrRows.stream()
                .filter(r -> r.get(0).equalsIgnoreCase(mgrName))
                .findFirst();

            if (maybeMgr.isEmpty()) {
                System.err.printf("  ! skipping \"%s\": no manager named \"%s\"%n",
                                  projName, mgrName);
                continue;
            }

            List<String> rec = maybeMgr.get();
            String mgrNric = rec.get(1);
            int    mgrAge  = Integer.parseInt(rec.get(2));
            String mgrMS   = rec.get(3);
            String mgrPwd  = rec.get(4);

            HDBManager manager = new HDBManager(mgrNric, mgrName, mgrAge, mgrMS, mgrPwd);

            String[] officerNames = cols[12].split(",");
            List<String> officerList = Arrays.stream(officerNames)
                                            .map(String::trim)
                                            .filter(s -> !s.isEmpty())
                                            .toList();

            BTOProject project = new BTOProject(
                manager, projName, neighborhood, sd, ed,
                List.of(t1, t2), u1, u2, maxOfficers, officerList
            );

            projectRepo.addProject(project);
            manager.addProject(project);
            System.out.printf("Loaded project \"%s\" (manager: %s)%n", projName, mgrName);
        }
    }

    // ------------------------------------------------------------------------
    // Applicant menu loop
    // ------------------------------------------------------------------------
    private static void runApplicantLoop(Applicant me,
                                         IApplicantService svc,
                                         IEnquiryService esvc,
                                         List<BTOProject> projects,
                                         Scanner sc) {
        while (true) {
            System.out.println("\n-- Applicant Menu --");
            System.out.println("1) View available projects");
            System.out.println("2) Apply for a project");
            System.out.println("3) View my application");
            System.out.println("4) Withdraw application");
            System.out.println("5) Submit enquiry");
            System.out.println("6) View my enquiries");
            System.out.println("7) Edit enquiry");
            System.out.println("8) Delete enquiry");
            System.out.println("9) Change password");
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": {
                    System.out.println("\n====== Available Projects ======");
                    // only list those visible *and* whose dates include today
                    List<BTOProject> available = svc.viewAvailableProjects(me, projects);
                    if (available.isEmpty()) {
                        System.out.println("No projects are accepting applications right now.");
                    } else {
                        for (int i = 0; i < available.size(); i++) {
                            BTOProject p = available.get(i);
                            // 1. Acacia Breeze – Yishun
                            System.out.printf("%d. %s - %s%n",
                                i + 1,
                                p.getProjectName(),
                                p.getNeighborhood());
                
                            // Application Period: 15/02/2025 to 20/03/2025
                            String start = p.getStartDate().toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                            .format(DISPLAY_FMT);
                            String end   = p.getEndDate().toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                            .format(DISPLAY_FMT);
                            System.out.printf("   Application Period: %s to %s%n%n", start, end);
                        }
                    }
                    break;
                }
                case "2": {
                    // 1) get only those projects currently open & visible
                    List<BTOProject> available = svc.viewAvailableProjects(me, projects);
                    if (available.isEmpty()) {
                        System.out.println("No projects available to apply for at the moment.");
                        break;
                    }

                    // 2) list them out
                    System.out.println("\n-- Available Projects --");
                    for (int i = 0; i < available.size(); i++) {
                        BTOProject p = available.get(i);
                        System.out.printf("%d) %s - %s%n", i + 1,
                            p.getProjectName(), p.getNeighborhood());
                    }

                    // 3) let user pick one
                    System.out.print("> ");
                    int projChoice = sc.nextInt();
                    sc.nextLine();  // consume the '\n' left behind by nextInt()
                    if (projChoice == 0) {
                        System.out.println("Application cancelled.");
                        break;
                    }
                    if (projChoice < 1 || projChoice > available.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    BTOProject selected = available.get(projChoice - 1);

                    // 4) now loop for flat‑type until they succeed or cancel
                    while (true) {
                        System.out.print("Flat type (2-room/3-room, or 0 to cancel): ");
                        String ftype = sc.nextLine().trim();
                        if (ftype.equals("0")) {
                            System.out.println("Application cancelled.");
                            break;
                        }
                        // have apply(...) return boolean success
                        if (svc.apply(me, selected, ftype)) {
                            // once it succeeds, stop retrying
                            break;
                        }
                        // else it printed a validation error; retry
                    }
                    break;
                }
                case "3":
                    svc.viewAppliedProject(me, projects);
                    break;
                case "4":
                    svc.requestWithdrawal(me);
                    break;
                case "5": {
                    // 1) get only those projects currently open & visible
                    List<BTOProject> available = svc.viewAvailableProjects(me, projects);
                    if (available.isEmpty()) {
                        System.out.println("No projects available to submit an enquiry at the moment.");
                        break;
                    }

                    // 2) list them out
                    System.out.println("\n-- Available Projects --");
                    for (int i = 0; i < available.size(); i++) {
                        BTOProject p = available.get(i);
                        System.out.printf("%d) %s - %s%n",
                            i + 1,
                            p.getProjectName(),
                            p.getNeighborhood());
                    }

                    // 3) let user pick one
                    System.out.print("Select project (0 to cancel): ");
                    int projChoice = sc.nextInt();
                    sc.nextLine();  // consume the '\n'
                    if (projChoice == 0) {
                        System.out.println("Enquiry cancelled.");
                        break;
                    }
                    if (projChoice < 1 || projChoice > available.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    BTOProject selected = available.get(projChoice - 1);

                    // 4) ask for the message
                    System.out.print("Message: ");
                    String msg = sc.nextLine();
                    esvc.submitEnquiry(me, selected.getProjectName(), msg);
                    break;
                }
                case "6": {
                    for (Enquiry e : esvc.getApplicantEnquiries(me)) {
                        System.out.println(e.getEnquiryId() + ": " + e.getMessage());
                        if (e.getReply() != null) {
                            System.out.println("Reply  : " + e.getReply());
                        }
                    }
                    break;
                }
                case "7": {
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    System.out.print("New message: ");
                    String msg = sc.nextLine();
                    esvc.editEnquiry(me, id, msg);
                    break;
                }
                case "8": {
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    esvc.deleteEnquiry(me, id);
                    break;
                }
                case "9": {
                    System.out.print("Current password: ");
                    String oldP = sc.nextLine();
                    System.out.print("New password: ");
                    String newP = sc.nextLine();
                    svc.changePassword(me, oldP, newP);
                    break;
                }
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // ------------------------------------------------------------------------
    // Manager menu loop
    // ------------------------------------------------------------------------
    private static void runManagerLoop(HDBManager me,
                                IHDBManagerService svc,
                                IProjectRepository repo,
                                Scanner sc,
                                IEnquiryService iesvc) {
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
                            System.out.println("Failed to create project. You still have a visible project, or the dates are wrong");
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
                        List<HDBOfficer> pending = project.getPendingRegistrations();
                        if (pending.isEmpty()) {
                            System.out.println("  (none)");
                        } else {
                            for (HDBOfficer o : pending) {
                                System.out.printf("  - %s (NRIC: %s)\n", o.getName(), o.getNRIC());
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
                    List<HDBOfficer> pending = project.getPendingRegistrations();

                    if (pending.isEmpty()) {
                        System.out.println("No pending officer registrations.");
                        break;
                    }

                    System.out.println("Pending officers:");
                    for (int i = 0; i < pending.size(); i++) {
                        HDBOfficer o = pending.get(i);
                        System.out.printf("%d) %s (NRIC: %s)\n", i + 1, o.getName(), o.getNRIC());
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

                    HDBOfficer selectedOfficer = pending.get(officerChoice - 1);
                    boolean success = svc.handleOfficerRegistration(me, project, selectedOfficer);
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
                    svc.changePassword(me, oldP, newP);
                    break;
                }

                case "0":
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // Officer menu loop
    // ------------------------------------------------------------------------
    private static void runOfficerLoop(HDBOfficer me,
                                       IHDBOfficerService svc,
                                       IApplicantService  applicantSvc,
                                       IEnquiryService    enquirySvc,
                                       List<BTOProject>   projects,
                                       Scanner sc) {

        while (true) {

            // header
            System.out.println("\n-- Officer Menu --");
            System.out.printf("Registration status : %s%n", me.getRegStatus());
            if (me.isHandlingProject())
                System.out.println("Handling project    : " + me.getHandlingProjectId());

            // options
            System.out.println("1) Register to handle a project");
            System.out.println("2) Cancel / remove my registration");
            System.out.println("3) View details of my project");
            System.out.println("4) View enquiries for my project");
            System.out.println("5) Reply to an enquiry");
            System.out.println("6) Book a flat for an applicant");
            System.out.println("7) Generate booking receipt");
            System.out.println("8) Switch to Applicant functions");
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {

                /* 1 ─ Register request */
                case "1": {
                    System.out.print("Project name: ");
                    String pid = sc.nextLine();
                    svc.registerToHandleProject(me, pid);
                    break;
                }

                /* 2 ─ Cancel / remove */
                case "2":
                    svc.cancelRegistration(me);
                    break;

                /* 3 ─ View my project (visibility ignored) */
                case "3": {
                    BTOProject p = svc.viewHandledProject(me);
                    if (p == null) {
                        System.out.println("You are not handling any project.");
                    } else {
                        System.out.printf("\n%s – %s%n", p.getProjectName(), p.getNeighborhood());
                        System.out.printf("Application Period : %s to %s%n",
                                p.getStartDate(), p.getEndDate());
                        System.out.printf("Visible to public   : %s%n", p.isVisible());
                        System.out.printf("2‑Room left         : %d%n", p.getUnits("2-room"));
                        System.out.printf("3‑Room left         : %d%n", p.getUnits("3-room"));
                    }
                    break;
                }

                /* 4 ─ View enquiries */
                case "4": {
                    var list = svc.viewProjectEnquiries(me);
                    if (list.isEmpty()) {
                        System.out.println("No enquiries for your project.");
                    } else {
                        for (Enquiry e : list)
                            System.out.printf("[%s] %s : %s%n",
                                    e.getEnquiryId(), e.getUserNric(), e.getMessage());
                    }
                    break;
                }

                /* 5 ─ Reply enquiry */
                case "5": {
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    System.out.print("Reply message: ");
                    String msg = sc.nextLine();
                    svc.replyToEnquiry(id, msg);
                    break;
                }

                /* 6 ─ Book flat */
                case "6": {
                    if (!me.isHandlingProject()) {
                        System.out.println("You must be handling a project first.");
                        break;
                    }
                    System.out.print("Applicant NRIC: ");
                    String aNric = sc.nextLine();
                    System.out.print("Flat type (2-room / 3-room): ");
                    String ftype = sc.nextLine();
                    svc.bookFlat(aNric, ftype);
                    break;
                }

                /* 7 ─ Generate receipt */
                case "7": {
                    System.out.print("Applicant NRIC: ");
                    String aNric = sc.nextLine();
                    svc.generateReceipt(aNric);
                    break;
                }

                /* 8 ─ Switch to Applicant menu (re‑use existing loop) */
                case "8":
                    runApplicantLoop(me, applicantSvc, enquirySvc, projects, sc);
                    break;

                /* 0 ─ Logout */
                case "0":
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
    // ------------------------------------------------------------------------
    // Helper to find a project by name
    // ------------------------------------------------------------------------
    private static BTOProject findProject(List<BTOProject> list, String name) {
        return list.stream()
                   .filter(p -> p.getProjectName().equalsIgnoreCase(name))
                   .findFirst()
                   .orElse(null);
    }
}
