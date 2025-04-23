package main.app;

import java.time.ZoneId;
import java.util.*;
import main.models.*;
import main.services.*;

public class ApplicantHandler implements IUserHandler {
    private final ApplicantService applicantSvc;
    private final IEnquiryService enquirySvc;
    private final List<BTOProject> allProjects;

    public ApplicantHandler(ApplicantService applicantSvc, IEnquiryService enquirySvc, List<BTOProject> allProjects) {
        this.applicantSvc = applicantSvc;
        this.enquirySvc = enquirySvc;
        this.allProjects = allProjects;
    }

    @Override
    public void run(User user, Scanner sc) {
        runApplicantLoop((Applicant) user, applicantSvc, enquirySvc, allProjects, sc);
    }

    public static void runApplicantLoop(Applicant me, ApplicantService svc, IEnquiryService esvc, List<BTOProject> projects, Scanner sc) {
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

