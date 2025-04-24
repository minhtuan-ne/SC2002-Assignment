package main.app;

import java.util.*;
import java.util.stream.Collectors;
import main.models.*;
import main.services.*;
import main.util.*;

public class OfficerHandler implements IUserHandler{
    private final HDBOfficerService officerSvc;
    private final ApplicantService applicantSvc;
    private final EnquiryService enquirySvc;
    private final RegistrationService registrationService;
    private final ProjectService projectSvc;
    private final Authenticator auth;
    private final FileManager fileManager;
        
    public OfficerHandler(
            HDBOfficerService officerService,
            ApplicantService applicantService,
            EnquiryService enquiryService,
            RegistrationService registrationService,
            ProjectService projectService,
            Authenticator auth,
            FileManager fileManager            
            ) {
        this.officerSvc = officerService;
        this.applicantSvc = applicantService;
        this.enquirySvc = enquiryService;
        this.projectSvc = projectService;
        this.auth = auth;
        this.fileManager = fileManager;
        this.registrationService = registrationService;
    }

    @Override
    public void run(User user, Scanner sc) {
        runOfficerLoop((HDBOfficer) user, officerSvc, applicantSvc, projectSvc, enquirySvc, registrationService,
            projectSvc.getAllProjects(), sc, auth, fileManager);
    }

    public static void runOfficerLoop(HDBOfficer me, HDBOfficerService svc, ApplicantService applicantSvc, ProjectService projectSvc, EnquiryService enquirySvc,
        RegistrationService registrationSvc, List<BTOProject> projects, Scanner sc, Authenticator auth, FileManager fileManager) {
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
            System.out.println("9) Change password");
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();

            switch (choice) {
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
                    registrationSvc.register(me, pid);
                    break;
                }

                case "2":
                    registrationSvc.cancelRegistration(me);
                    break;

                case "3": {
                    BTOProject p = svc.viewHandledProject(me);
                    if (p == null) {
                        System.out.println("You are not handling any project.");
                    } else {
                        System.out.printf("\n%s - %s%n", p.getProjectName(), p.getNeighborhood());
                        System.out.printf("Application Period : %s to %s%n",
                                p.getStartDate(), p.getEndDate());
                        System.out.printf("Visible to public   : %s%n", p.isVisible());
                        System.out.printf("2-Room left         : %d%n", p.getUnits("2-room"));
                        System.out.printf("3-Room left         : %d%n", p.getUnits("3-room"));
                    }
                    break;
                }

                case "4": {
                    List<Enquiry> all = enquirySvc.getAllEnquiries();
                    List<Enquiry> list = new ArrayList<>();
                    if (me.isHandlingProject()){
                        String pid = me.getHandlingProjectId();
                        for (Enquiry e : all)
                            if (e.getProjectName().equalsIgnoreCase(pid))
                                list.add(e);    
                    }                
                    if (list.isEmpty()) {
                        System.out.println("No enquiries for your project.");
                    } else {
                        for (Enquiry e : list)
                            System.out.printf("[%s] %s : %s%n",
                                    e.getEnquiryId(), e.getUserNric(), e.getMessage());
                    }
                    break;
                }

                case "5": {
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    System.out.print("Reply message: ");
                    String msg = sc.nextLine();
                    enquirySvc.replyToEnquiry(id, msg);
                    break;
                }

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
                    for (Application a : projectSvc.getApplicationByProject(myProject)) {
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

                case "7": {
                    System.out.print("Applicant NRIC: ");
                    String aNric = sc.nextLine();
                    svc.generateReceipt(aNric);
                    break;
                }

                case "8":
                    new ApplicantHandler(applicantSvc, enquirySvc, projectSvc, fileManager).run(me, sc);
                    break;

                case "9": {
                    System.out.print("Current password: ");
                    String oldP = sc.nextLine();
                    System.out.print("New password: ");
                    String newP = sc.nextLine();
                    me.changePassword(oldP, newP);
                    break;
                }

                case "0": {
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
