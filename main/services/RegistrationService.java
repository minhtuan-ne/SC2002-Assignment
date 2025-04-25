package main.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import main.models.*;
import main.models.HDBOfficer.RegistrationStatus;
import main.util.FileManager;

public class RegistrationService {
    private final List<Registration> registrations;
    private final ApplicantService applicantSvc;
    private final ProjectService projectSvc;
    private final FileManager fileManager;

    public RegistrationService(ApplicantService applicantService, ProjectService projectService, FileManager fileManager){
        this.registrations = new ArrayList<>();
        this.applicantSvc = applicantService;
        this.projectSvc = projectService;
        this.fileManager = fileManager;
        try{
            Path path = Paths.get("data", "RegistrationList.txt");
            List<String> lines = Files.readAllLines(path).stream()
                .filter(l -> !l.startsWith("Officer") && !l.isBlank())
                .collect(Collectors.toList());
            List<User> users = fileManager.loadAllUser();
            Map<String, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getNRIC, u -> u));

            List<BTOProject> allProjects = projectSvc.getAllProjects();

            for (String line : lines) {
                String[] cols = line.split("\\t");

                String applicantNRIC    = cols[0];
                String projectName      = cols[1];
                String status           = cols[2];

                HDBOfficer officer = (HDBOfficer) userMap.get(applicantNRIC);
                if (officer == null) {
                    System.err.println("No user found for NRIC: " + applicantNRIC);
                    continue;
                }

                BTOProject matchedProject = allProjects.stream()
                    .filter(p -> p.getProjectName().equals(projectName))
                    .findFirst()
                    .orElse(null);

                Registration regist = new Registration(officer, RegistrationStatus.valueOf(status.toUpperCase()), matchedProject);
                registrations.add(regist);
            }
        } catch (IOException ex) {
            System.err.println("ERROR loading ApplicationList.txt: " + ex.getMessage());
        }
    }

    public boolean register(HDBOfficer officer, String projectId){
        // Find the target project
        BTOProject project = projectSvc.getProjectByName(projectId);
        if (project == null) {
            System.out.println("Project not found.");
            return false;
        }

        // If officer is handling a project, check if it's expired
        if (officer.isHandlingProject()) {
            BTOProject current = projectSvc.getProjectByName(officer.getHandlingProjectId());
            if (current != null && new Date().after(current.getEndDate())) {
                // Auto-clear expired project
                officer.setHandlingProjectId(null);
                officer.setRegStatus(HDBOfficer.RegistrationStatus.NONE);
                fileManager.updateRegistration(officer.getNRIC(), projectId, HDBOfficer.RegistrationStatus.NONE);
                current.removeOfficer(officer);
                System.out.println("Previous project has ended. Status reset – you may now register.");
            } else {
                System.out.println("You are still handling an ongoing project. Cannot register.");
                return false;
            }
        }

        // Check if officer has applied for this project
        Application app = applicantSvc.getApplication(officer.getNRIC());
        if (app != null && app.getProjectName().equalsIgnoreCase(projectId)) {
            System.out.println("You have already applied for this project as an applicant – cannot register as officer.");
            return false;
        }

        // All checks passed – submit request
        officer.setHandlingProjectId(projectId);
        officer.setRegStatus(HDBOfficer.RegistrationStatus.PENDING);
        Registration regist = new Registration(officer, RegistrationStatus.PENDING, project);
        registrations.add(regist);
        fileManager.saveRegistration(regist);
        System.out.println("Request submitted - awaiting manager approval.");
        return true;
    }
    
    public void cancelRegistration(HDBOfficer officer, String projectName) {
        if (officer.getRegStatus() == HDBOfficer.RegistrationStatus.NONE) {
            System.out.println("No active request / assignment to cancel.");
        } else {
            officer.setHandlingProjectId(null);
            if(officer.getRegStatus().equals(RegistrationStatus.APPROVED)){
                fileManager.updateProjectOfficer(projectName, officer.getNRIC(), officer.getName(), false);
            }
            officer.setRegStatus(HDBOfficer.RegistrationStatus.NONE);
            fileManager.updateRegistration(officer.getNRIC(), projectName, RegistrationStatus.NONE);
            fileManager.updateOfficerInProject(projectName, officer.getNRIC(), false);
            Registration registered = getRegistrationByOfficer(officer, projectName);
            registered.setStatus(RegistrationStatus.NONE);
            
            System.out.println("Registration removed.");
        }
    }

    public Registration getRegistrationByOfficer(HDBOfficer officer, String projectName){
        return registrations.stream()
            .filter(r -> r.getOfficer().getNRIC().equals(officer.getNRIC()) && r.getProject().getProjectName().equals(projectName))
            .findFirst()
            .orElse(null);
    }

    public boolean handleOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer) {
        if (!project.getManager().getNRIC().equalsIgnoreCase(manager.getNRIC())) {
            System.out.println("Wrong Manager.");
            return false;
        }

        if (!officer.isRegistrationPending()
                || !project.getProjectName().equalsIgnoreCase(officer.getHandlingProjectId())) {
            System.out.println("No pending registration from this officer for this project.");
            return false;
        }

        if (project.getOfficers().size() >= project.getMaxOfficers()) {
            System.out.println("Project has reached max officer capacity.");
            return false;
        }

        String projectId = project.getProjectName();
        if (!projectId.equalsIgnoreCase(officer.getHandlingProjectId()))
            throw new IllegalStateException("Project mismatch during approval");
        officer.setRegStatus(RegistrationStatus.APPROVED);
        
        // Update the project file with the new officer assignment
        fileManager.updateRegistration(officer.getNRIC(), project.getProjectName(), RegistrationStatus.APPROVED);
        registrations.removeIf(r -> r.getOfficer().getNRIC().equalsIgnoreCase(officer.getNRIC()));
        project.addOfficer(officer);
        System.out.println("Officer registration approved.");
        return true;
    }

    public List<Registration> getRegistration(){
        return registrations;
    }
}
