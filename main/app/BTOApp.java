package main.app;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import main.models.*;
import main.repositories.*;
import main.services.*;
import main.util.*;

public class BTOApp {
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
        HDBManagerService  managerSvc   = new HDBManagerService(projectRepo, fileManager);
        IEnquiryService enquirySvc    = new EnquiryService();
        ApplicantService applicantSvc = new ApplicantService();
        HDBOfficerService officerSvc = new HDBOfficerService((ProjectRepository) projectRepo, (ApplicantService) applicantSvc);

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
                    Map<String, IUserHandler> roleHandlers = new HashMap<>();
                    roleHandlers.put("applicant", new ApplicantHandler(applicantSvc, enquirySvc, projectRepo.getAllProjects()));
                    roleHandlers.put("hdb manager", new ManagerHandler(managerSvc, projectRepo, enquirySvc));
                    roleHandlers.put("hdbofficer", new OfficerHandler(officerSvc, applicantSvc, enquirySvc, projectRepo, auth, fileManager));
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

    /*
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
}
