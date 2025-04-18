package main;

import main.models.*;
import main.util.*;
import main.repositories.*;
import main.services.*;
import main.repositories.ProjectRepository;
import main.services.EnquiryService;


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
        IEnquiryService     enquirySvc   = new EnquiryService();
        IHDBManagerService  managerSvc   = new HDBManagerService(projectRepo);
        IApplicantService applicantSvc = new ApplicantService(fileManager);
        IHDBOfficerService officerSvc  = new HDBOfficerService(
                (ProjectRepository) projectRepo,
                (EnquiryService) enquirySvc,
                (ApplicantService) applicantSvc    // <-- third argument required!
        );

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
                            runManagerLoop((HDBManager) user, managerSvc, projectRepo, sc);
                            break;
                        case "hdbofficer":
                            runOfficerLoop((HDBOfficer) user,
                                    officerSvc,
                                    (ApplicantService) applicantSvc,
                                    (EnquiryService) enquirySvc,
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
            BTOProject project = new BTOProject(
                manager, projName, neighborhood, sd, ed,
                List.of(t1, t2), u1, u2, maxOfficers
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
                    System.out.print("Project name: ");
                    String pname = sc.nextLine();
                    System.out.print("Message: ");
                    String msg = sc.nextLine();
                    esvc.submitEnquiry(me, pname, msg);
                    break;
                }
                case "6": {
                    for (Enquiry e : esvc.getApplicantEnquiries(me)) {
                        System.out.println(e.getEnquiryId() + ": " + e.getMessage());
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
                                       Scanner sc) {
        while (true) {
            System.out.println("\n-- Manager Menu --");
            System.out.println("1) Create project");
            System.out.println("2) View all projects");
            System.out.println("3) Edit project");
            System.out.println("4) Delete project");
            System.out.println("5) Toggle visibility");
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Name: ");
                    String name = sc.nextLine();
                    System.out.print("Neighborhood: ");
                    String nb = sc.nextLine();
                    System.out.print("Start date (yyyy‑MM‑dd): ");
                    Date sd = Date.from(
                        LocalDate.parse(sc.nextLine())
                                  .atStartOfDay(ZoneId.systemDefault())
                                  .toInstant()
                    );
                    System.out.print("End date (yyyy‑MM‑dd): ");
                    Date ed = Date.from(
                        LocalDate.parse(sc.nextLine())
                                  .atStartOfDay(ZoneId.systemDefault())
                                  .toInstant()
                    );
                    svc.createProject(me, name, nb, sd, ed,
                                      List.of("2-room","3-room"),
                                      10, 10);
                    break;

                case "2":
                    for (BTOProject p : svc.viewAllProjects()) {
                        System.out.printf("%s [%s]%n",
                            p.getProjectName(),
                            p.isVisible() ? "visible" : "hidden"
                        );
                    }
                    break;

                case "3":
                case "4":
                case "5":
                    System.out.println("Not yet implemented in this demo.");
                    break;

                case "0":
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

// Officer menu loop – fully aligned with the new service
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
                    System.out.print("Project ID: ");
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
