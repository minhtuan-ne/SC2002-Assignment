package main;

import main.models.*;
import main.util.*;
import main.repositories.*;
import main.services.*;

import java.util.List;
import java.util.Scanner;
import java.util.Date;

public class BTOApp {
    public static void main(String[] args) {
        // set up core infrastructure
        IFileManager fileManager = new FileManager();
        Authenticator auth = new Authenticator(fileManager);
        IProjectRepository projectRepo = new ProjectRepository();
        IHDBManagerService managerService = new HDBManagerService(projectRepo);
        IHDBOfficerService officerService = new HDBOfficerService();
        IApplicantService applicantService = new ApplicantService();
        IEnquiryService enquiryService = new EnquiryService();

        // login
        User user = auth.logIn();
        Scanner sc = new Scanner(System.in);
        String role = user.getRole().toLowerCase();

        switch (role) {
            case "applicant":
                Applicant me = (Applicant) user;
                runApplicantLoop(me, applicantService, enquiryService, projectRepo.getAllProjects(), sc);
                break;

            case "manager":
                HDBManager mgr = (HDBManager) user;
                runManagerLoop(mgr, managerService, projectRepo, sc);
                break;

            case "officer":
                HDBOfficer off = (HDBOfficer) user;
                runOfficerLoop(off, officerService, projectRepo.getAllProjects(), sc);
                break;
        }

        System.out.println("Goodbye!");
        sc.close();
    }

    private static void runApplicantLoop(Applicant me, IApplicantService svc, IEnquiryService esvc, List<BTOProject> projects, Scanner sc) {
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
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.println("\n-- All Projects --");
                    for (int i = 0; i < projects.size(); i++) {
                        BTOProject p = projects.get(i);
                        System.out.printf(
                            "%d) %s [%s]  %s → %s%n",
                            i + 1,
                            p.getProjectName(),
                            p.isVisible() ? "visible" : "hidden",
                            p.getStartDate(),
                            p.getEndDate()
                        );
                    }
                    break;
                case "2":
                    System.out.print("Project name: ");
                    String pname = sc.nextLine();
                    System.out.print("Flat type (2-room/3-room): ");
                    String ftype = sc.nextLine();
                    svc.apply(me, findProject(projects, pname), ftype);
                    break;
                case "3":
                    svc.viewAppliedProject(me, projects);
                    break;
                case "4":
                    svc.requestWithdrawal(me);
                    break;
                case "5":
                    System.out.print("Project name: ");
                    pname = sc.nextLine();
                    System.out.print("Message: ");
                    String msg = sc.nextLine();
                    esvc.submitEnquiry(me, pname, msg);
                    break;
                case "6":
                    for (Enquiry e : esvc.getApplicantEnquiries(me)) {
                        System.out.println(e.getEnquiryId() + ": " + e.getMessage());
                    }
                    break;
                case "7":
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    System.out.print("New message: ");
                    msg = sc.nextLine();
                    esvc.editEnquiry(me, id, msg);
                    break;
                case "8":
                    System.out.print("Enquiry ID: ");
                    id = sc.nextLine();
                    esvc.deleteEnquiry(me, id);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void runManagerLoop(HDBManager me, IHDBManagerService svc,
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
                    Date sd = Date.from(java.time.LocalDate.parse(sc.nextLine())
                                      .atStartOfDay(java.time.ZoneId.systemDefault())
                                      .toInstant());
                    System.out.print("End date (yyyy‑MM‑dd): ");
                    Date ed = Date.from(java.time.LocalDate.parse(sc.nextLine())
                                      .atStartOfDay(java.time.ZoneId.systemDefault())
                                      .toInstant());
                    svc.createProject(
                      me,
                      name, nb, sd, ed,
                      List.of("2-room","3-room"),
                      10, 10);
                    break;
                case "2":
                    for (BTOProject p : svc.viewAllProjects()) {
                        System.out.printf("%s [%s]%n", p.getProjectName(),
                            p.isVisible() ? "visible" : "hidden");
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

    private static void runOfficerLoop(HDBOfficer me, IHDBOfficerService svc, List<BTOProject> projects, Scanner sc) {
        while (true) {
            System.out.println("\n-- Officer Menu --");
            System.out.println("1) Assign to project");
            System.out.println("2) Remove from project");
            System.out.println("3) Book flat");
            System.out.println("4) Reply to enquiry");
            System.out.println("0) Logout");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.print("Officer NRIC: ");
                    String oNric = sc.nextLine();
                    System.out.print("Project ID: ");
                    String pid = sc.nextLine();
                    svc.assignToProject(
                        me, pid);
                    break;
                case "2":
                    svc.removeFromProject(
                        me);
                    break;
                case "3":
                    System.out.print("Applicant NRIC: ");
                    String aNric = sc.nextLine();
                    System.out.print("Flat type: ");
                    String ftype = sc.nextLine();
                    svc.bookFlat(aNric, ftype);
                    break;
                case "4":
                    System.out.print("Enquiry ID: ");
                    String id = sc.nextLine();
                    System.out.print("Message: ");
                    String msg = sc.nextLine();
                    svc.replyToEnquiry(id, msg);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static BTOProject findProject(List<BTOProject> list, String name) {
        return list.stream()
            .filter(p -> p.getProjectName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }
}
