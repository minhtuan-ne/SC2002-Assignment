package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import main.models.*;
import main.util.FileManager;

/**
 * Handles logic related to loading, managing, and retrieving BTO projects.
 * Also links projects to applicants and managers.
 */
public class ProjectService {
    private static final List<BTOProject> projects = new ArrayList<>();
    private final ApplicantService applicantSvc;

    /**
     * Constructs a ProjectService and loads projects from file.
     *
     * @param fileManager  used to retrieve project and manager data
     * @param applicantSvc service to link applications to projects
     */
    public ProjectService(FileManager fileManager, ApplicantService applicantSvc) {
        this.applicantSvc = applicantSvc;
        try {
            Path path = Paths.get("data", "ProjectList.txt");
            List<String> lines = Files.readAllLines(path).stream()
                    .filter(l -> !l.startsWith("Project Name") && !l.isBlank())
                    .collect(Collectors.toList());

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            Map<String, List<List<String>>> userData = fileManager.getDatabyRole();
            List<List<String>> mgrRows = userData.getOrDefault("Manager", Collections.emptyList());
            List<User> allUsers = fileManager.loadAllUser();

            for (String line : lines) {
                String[] cols = line.split("\\t");

                String projName = cols[0];
                String neighborhood = cols[1];
                int u1 = Integer.parseInt(cols[3]);
                int u2 = Integer.parseInt(cols[6]);
                int maxOfficers = Integer.parseInt(cols[11]);

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
                int mgrAge = Integer.parseInt(rec.get(2));
                String mgrMS = rec.get(3);
                String mgrPwd = rec.get(4);

                HDBManager manager = new HDBManager(mgrNric, mgrName, mgrAge, mgrMS, mgrPwd);

                String[] officerNames = cols[12].split(",");
                List<String> officerList = Arrays.stream(officerNames)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                List<Flat> flats = List.of(new TwoRoom(u1, 0), new ThreeRoom(u2, 0));
                List<HDBOfficer> officers = allUsers.stream()
                        .filter(u -> u instanceof HDBOfficer && officerList.contains(u.getName()))
                        .map(u -> (HDBOfficer) u)
                        .collect(Collectors.toList());
                officers.forEach(o -> {
                    o.setHandlingProjectId(projName);
                    o.setRegStatus(HDBOfficer.RegistrationStatus.APPROVED);
                });
                BTOProject project = new BTOProject(manager, projName, neighborhood, sd, ed, flats, maxOfficers,
                        officers);

                projects.add(project);
                manager.addProject(project);
                System.out.printf("Loaded project \"%s\" (manager: %s)%n", projName, mgrName);
            }
        } catch (IOException ex) {
            System.err.println("ERROR loading ProjectList.txt: " + ex.getMessage());
        }
    }

    /**
     * Returns the full list of loaded BTO projects.
     *
     * @return list of projects
     */
    public List<BTOProject> getAllProjects() {
        return projects;
    }

    /**
     * Adds a new project to the internal list.
     *
     * @param project BTO project to add
     */
    public void addProject(BTOProject project) {
        projects.add(project);
    }

    /**
     * Removes a project from the internal list.
     *
     * @param project BTO project to remove
     */
    public void removeProject(BTOProject project) {
        projects.remove(project);
    }

    /**
     * Retrieves a project by its name (case-insensitive).
     *
     * @param name project name
     * @return matching project or null
     */
    public BTOProject getProjectByName(String name) {
        for (BTOProject p : projects)
            if (p.getProjectName().equalsIgnoreCase(name))
                return p;
        return null;
    }

    /**
     * Returns all applications submitted for a given project.
     *
     * @param projectName the project to search for
     * @return list of matching applications
     */
    public List<Application> getApplicationByProject(BTOProject projectName) {
        return applicantSvc.getAllApplication().stream()
                .filter(app -> app.getProjectName().equalsIgnoreCase(projectName.getProjectName()))
                .collect(Collectors.toList());
    }
}
