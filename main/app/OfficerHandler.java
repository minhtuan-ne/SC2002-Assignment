package main.app;

import java.util.*;
import java.util.stream.Collectors;
import main.models.*;
import main.services.*;
import main.util.*;


/**
 * Handles all CLI interactions for an HDB Officer.
 * Combines officer tasks like registration and booking with optional applicant functions.
 */
public class OfficerHandler implements IUserHandler{
    private final HDBOfficerService officerSvc;
    private final ApplicantService applicantSvc;
    private final EnquiryService enquirySvc;
    private final RegistrationService registrationService;
    private final ProjectService projectSvc;
    private final Authenticator auth;
    private final FileManager fileManager;
    
    /**
     * Constructs a new OfficerHandler instance.
     *
     * @param officerService      officer-specific logic
     * @param applicantService    shared applicant services
     * @param enquiryService      service to handle enquiries
     * @param registrationService registration logic
     * @param projectService      service for project data
     * @param auth                authentication manager
     * @param fileManager         file I/O handler
     */
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
    
    /**
     * Runs the officer’s main interaction loop.
     *
     * @param user the logged-in officer
     * @param sc   scanner for CLI input
     */
    @Override
    public void run(User user, Scanner sc) {
        runOfficerLoop((HDBOfficer) user, officerSvc, applicantSvc, projectSvc, enquirySvc, registrationService,
            projectSvc.getAllProjects(), sc, auth, fileManager);
    }

    /**
     * Full interaction flow for officers including registration and booking duties.
     *
     * @param me         the logged-in officer
     * @param svc        officer logic service
     * @param applicantSvc shared applicant services
     * @param projectSvc project data service
     * @param enquirySvc enquiry service
     * @param registrationSvc registration manager
     * @param projects   list of available BTO projects
     * @param sc         scanner for input
     * @param auth       login manager
     * @param fileManager persistence handler
     */
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
            System.out.println("2) View registered project");
            System.out.println("3) Cancel / remove my registration");
            System.out.println("4) View details of my project");
            System.out.println("5) View enquiries for my project");
            System.out.println("6) Reply to an enquiry");
            System.out.println("7) Book a flat for an applicant");
            System.out.println("8) Generate booking receipt");
            System.out.println("9) Switch to Applicant functions");
            System.out.println("10) Change password");
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
                    //if intend to apply then cant register
                    if (answer.equals("yes")) {
                        System.out.println("Registration cancelled – you cannot apply and handle the same project.");
                        break;
                    }

                    // Continue registration only if user said "no"
                    registrationSvc.register(me, pid);
                    break;
                }

                case "2":
                    List<Registration> registrations = registrationSvc.getRegistration().stream()
                    .filter(r -> r.getOfficer().getNRIC().equals(me.getNRIC()) && !r.getStatus().equals(HDBOfficer.RegistrationStatus.NONE))
                    .collect(Collectors.toList());

                    //check if there is any registration
                    if (registrations.isEmpty()) {
                        System.out.println("You have no registrations.");
                    } else {
                        System.out.println("Your Registrations:");
                        for (Registration r : registrations) {
                            System.out.println("----------------------------------");
                            System.out.println("Project Name: " + r.getProject().getProjectName());
                            System.out.println("Registration Status: " + r.getStatus());
                        }
                    }
                    break;

                case "3":
                    List<Registration> myRegs = registrationSvc.getRegistration().stream()
                        .filter(r -> r.getOfficer().getNRIC().equals(me.getNRIC()))
                        .collect(Collectors.toList());
                    // scan myRegs to get registrations
                    if (myRegs.isEmpty()) {
                        System.out.println("You have no registrations to cancel.");
                        break;
                    }
                    //if myRegs not empty
                    System.out.println("Select a registration to cancel:");
                    for (int i = 0; i < myRegs.size(); i++) {
                        Registration r = myRegs.get(i);
                        System.out.printf("  [%d] %s (Status: %s)\n", i + 1, r.getProject().getProjectName(), r.getStatus());
                    }
                    // then prompt user to choose what to cancel
                    System.out.print("Enter number: ");
                    int ch = -1;
                    try {
                        ch = Integer.parseInt(sc.nextLine());
                        if (ch < 1 || ch > myRegs.size()) {
                            System.out.println("Invalid selection.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input.");
                        break;
                    }
                
                    Registration selected = myRegs.get(ch - 1);
                    registrationSvc.cancelRegistration(me, selected.getProject().getProjectName());
                    break;

                case "4": {
                    //get the project officer is handling
                    BTOProject p = svc.viewHandledProject(me);
                    if (p == null) {
                        System.out.println("You are not handling any project.");
                    //display
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

                case "5": {
                    //get all enquiries
                    List<Enquiry> all = enquirySvc.getAllEnquiries();
                    List<Enquiry> list = new ArrayList<>();
                    //filter enquiries of officer's project
                    if (me.isHandlingProject()){
                        String pid = me.getHandlingProjectId();
                        for (Enquiry e : all)
                            if (e.getProjectName().equalsIgnoreCase(pid))
                                list.add(e);    
                    }                
                    if (list.isEmpty()) {
                        System.out.println("No enquiries for your project.");
                    //display all enquiries of the project
                    } else {
                        for (Enquiry e : list)
                            System.out.printf("[%s] %s : %s%n",
                                    e.getEnquiryId(), e.getUserNric(), e.getMessage());
                    }
                    break;
                }

                case "6": {
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    System.out.print("Reply message: ");
                    String msg = sc.nextLine();
                    //go to enquiryService to create a reply
                    enquirySvc.replyToEnquiry(id, msg);
                    break;
                }

                case "7": {
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
                    fileManager.updateApplication(applicantNric, chosen);
                    BTOProject project = projectSvc.getAllProjects().stream()
                        .filter(p -> p.getProjectName().equals(chosen.getProjectName()))
                        .findFirst()
                        .orElse(null);
                    try {
                        fileManager.updateProject(project.getProjectName(), project.getProjectName(), project.getNeighborhood(), project.getStartDate(), project.getEndDate(), project.getUnits("2-room"), project.getUnits("3-room"));
                    } catch (Exception e) {
                        System.out.println("Update project failed");
                    }
                    break;
                }

                case "8": {
                    System.out.print("Applicant NRIC: ");
                    String aNric = sc.nextLine();
                    svc.generateReceipt(aNric);
                    break;
                }

                case "9":
                    // move to applicant services
                    new ApplicantHandler(applicantSvc, enquirySvc, projectSvc, fileManager).run(me, sc);
                    break;

                case "10": {
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
