package main.app;

import java.util.*;
import java.util.stream.Collectors;
import main.models.*;
import main.services.*;
import main.util.*;
import main.repositories.IProjectRepository;

public class OfficerHandler implements IUserHandler{
    private final IHDBOfficerService officerService;
    private final ApplicantService applicantService;
    private final IEnquiryService enquiryService;
    private final IProjectRepository projectRepo;
    private final Authenticator auth;
    private final IFileManager fileManager;
        
    public OfficerHandler(
            IHDBOfficerService officerService,
            ApplicantService applicantService,
            IEnquiryService enquiryService,
            IProjectRepository projectRepo,
            Authenticator auth,
            IFileManager fileManager
            ) {
        this.officerService = officerService;
        this.applicantService = applicantService;
        this.enquiryService = enquiryService;
        this.projectRepo = projectRepo;
        this.auth = auth;
        this.fileManager = fileManager;
    }


    @Override
    public void run(User user, Scanner sc) {
        runOfficerLoop((HDBOfficer) user,
            officerService,
            applicantService,
            enquiryService,
            projectRepo.getAllProjects(),
            sc,
            auth,
            fileManager);
    }

    public static void runOfficerLoop(HDBOfficer me, IHDBOfficerService svc, IApplicantService applicantSvc,
        IEnquiryService enquirySvc, List<BTOProject> projects, Scanner sc, Authenticator auth, IFileManager fileManager) {
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
                    String pid = sc.nextLine().trim();

                    // Prompt: Do you intend to apply?
                    System.out.print("Do you intend to apply for this project as an applicant? (yes/no): ");
                    String answer = sc.nextLine().trim().toLowerCase();

                    if (answer.equals("yes")) {
                        System.out.println("Registration cancelled – you cannot apply and handle the same project.");
                        break;
                    }

                    // Continue registration only if user said "no"
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
                
                    // 1) locate the project the officer is handling
                    BTOProject myProject = svc.viewHandledProject(me);
                    if (myProject == null) {
                        System.out.println("Project not found.");
                        break;
                    }
                
                    // 2) collect all applications for that project in SUCCESSFUL state
                    List<Application> successApps = new ArrayList<>();
                    for (Application a : myProject.getApplications()) {
                        if ("Successful".equalsIgnoreCase(a.getStatus())) {
                            successApps.add(a);
                        }
                    }
                
                    if (successApps.isEmpty()) {
                        System.out.println("There are no applicants in 'Successful' state for booking.");
                        break;
                    }
                
                    // 3) display list to officer
                    System.out.println("\nApplicants ready for booking:");
                    for (int i = 0; i < successApps.size(); i++) {
                        Application a = successApps.get(i);
                        System.out.printf("%d) %s  |  %s  |  Flat type: %s\n",
                                          i + 1,
                                          a.getApplicant().getName(),
                                          a.getApplicant().getNRIC(),
                                          a.getFlatType());
                    }
                    System.out.print("Select applicant (0 to cancel): ");
                    String sel = sc.nextLine().trim();
                    int idx;
                    try {
                        idx = Integer.parseInt(sel);
                    } catch (NumberFormatException ex) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                    if (idx == 0) {
                        System.out.println("Booking cancelled.");
                        break;
                    }
                    if (idx < 1 || idx > successApps.size()) {
                        System.out.println("Invalid selection.");
                        break;
                    }
                
                    Application chosen = successApps.get(idx - 1);
                    String applicantNric = chosen.getApplicant().getNRIC();
                    String flatType      = chosen.getFlatType();
                
                    // 4) delegate to service – it already validates state & units
                    svc.bookFlat(applicantNric, flatType);
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
                    new ApplicantHandler(applicantSvc, enquirySvc, projects).run(me, sc);
                    break;

                /* 0 ─ Logout */
                case "0": {
                    // Save officer state upon logout
                    List<HDBOfficer> officers = auth.getUsers().stream()
                            .filter(u -> u instanceof HDBOfficer)
                            .map(u -> (HDBOfficer) u)
                            .collect(Collectors.toList());

                    fileManager.saveOfficersToFile("data/OfficerList.txt", officers);

                    System.out.println("Logging out...");
                }
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
