package main.app;

import java.util.*;
import main.models.*;
import main.services.*;
import main.util.*;


/**
 * Entry point of the BTO Management System CLI application.
 * Handles initialization, authentication, and routing users to the appropriate handler.
 */
public class BTOApp {
	/**
     * Starts the CLI-based BTO system.
     *
     * Initializes services and enters a login loop where users are routed to role-based menus.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        // 1) Core infra
        FileManager fileManager       = new FileManager();
        Authenticator auth             = new Authenticator(fileManager);

        // 2) Services
        ApplicantService applicantSvc = new ApplicantService(fileManager);
        ProjectService projectSvc = new ProjectService(fileManager, applicantSvc);
        HDBManagerService  managerSvc   = new HDBManagerService(projectSvc, fileManager);
        EnquiryService enquirySvc    = new EnquiryService();
        HDBOfficerService officerSvc = new HDBOfficerService(projectSvc, applicantSvc);
        RegistrationService registrationSvc = new RegistrationService(applicantSvc, projectSvc, fileManager);

        // 3) Main login/logout loop
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
                    Map<String, IUserHandler> roleHandlers = new HashMap<>();
                    roleHandlers.put("applicant", new ApplicantHandler(applicantSvc, enquirySvc, projectSvc, fileManager));
                    roleHandlers.put("hdb manager", new ManagerHandler(managerSvc, enquirySvc, registrationSvc, projectSvc, fileManager));
                    roleHandlers.put("hdbofficer", new OfficerHandler(officerSvc, applicantSvc, enquirySvc, registrationSvc, projectSvc, auth, fileManager));
                    IUserHandler handler = roleHandlers.get(role);
                    if (handler != null) {
                        handler.run(user, sc);
                    } else {
                        System.out.println("Unknown role: " + role);
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
}
