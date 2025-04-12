package main.services;

import java.util.*;
import main.models.*;

public class ApplicantService {
    private static List<Application> applications = new ArrayList<>();
    private static List<Enquiry> enquiries = new ArrayList<>();

    public static void apply(Applicant applicant, BTOProject project, String flatType) {
        if (hasApplied(applicant)) {
            System.out.println("You already have an active application.");
            return;
        }

        boolean isSingle = applicant.getMaritalStatus().equalsIgnoreCase("Single");
        boolean isMarried = applicant.getMaritalStatus().equalsIgnoreCase("Married");
        int age = applicant.getAge();

        if (flatType.equalsIgnoreCase("2-room")) {
            if (!(isSingle && age >= 35) && !(isMarried && age >= 21)) {
                System.out.println("Only singles aged 35+ or married applicants aged 21+ can apply for 2-room flats.");
                return;
            }
        } else if (flatType.equalsIgnoreCase("3-room")) {
            if (!(isMarried && age >= 21)) {
                System.out.println("Only married applicants aged 21+ can apply for 3-room flats.");
                return;
            }
        } else {
            System.out.println("Invalid flat type.");
            return;
        }

        Application application = new Application(applicant, project.getProjectName(), flatType);
        applications.add(application);
        project.addApplication(application);
        System.out.println("Application submitted successfully.");
    }

    public static boolean hasApplied(Applicant applicant) {
        for (Application a : applications) {
            if (a.getApplicant().getNRIC().equals(applicant.getNRIC()) && !a.getStatus().equalsIgnoreCase("Unsuccessful")) {
                return true;
            }
        }
        return false;
    }

    public static Application getApplication(String nric) {
        for (Application a : applications) {
            if (a.getApplicant().getNRIC().equals(nric)) {
                return a;
            }
        }
        return null;
    }

    public static void viewAppliedProject(Applicant applicant, List<BTOProject> allProjects) {
        Application app = getApplication(applicant.getNRIC());
        if (app == null) {
            System.out.println("No application found.");
            return;
        }

        System.out.println("Status: " + app.getStatus());
        System.out.println("Flat Type: " + app.getFlatType());

        for (BTOProject p : allProjects) {
            if (p.getProjectName().equals(app.getProjectName())) {
                System.out.println("Project Name: " + p.getProjectName());
                System.out.println("Neighborhood: " + p.getNeighborhood());
                System.out.println("Application Period: " + p.getStartDate() + " to " + p.getEndDate());
                System.out.println("Manager: " + p.getManager().getNRIC());
                return;
            }
        }
        System.out.println("Applied project details are not found.");
    }

    public static boolean requestWithdrawal(Applicant applicant) {
        Application app = getApplication(applicant.getNRIC());
        if (app == null) {
            System.out.println("No application to withdraw.");
            return false;
        }

        if (app.getStatus().equalsIgnoreCase("Pending") || app.getStatus().equalsIgnoreCase("Successful")) {
            app.setStatus("Unsuccessful");
            System.out.println("Application withdrawn.");
            return true;
        }

        System.out.println("Cannot withdraw application in current state: " + app.getStatus());
        return false;
    }

    public static List<BTOProject> viewAvailableProjects(Applicant applicant, List<BTOProject> allProjects) {
        List<BTOProject> result = new ArrayList<>();
        Date today = new Date();

        for (BTOProject project : allProjects) {
            if (!project.isVisible()) continue;
            if (today.before(project.getStartDate()) || today.after(project.getEndDate())) continue;

            boolean isSingle = applicant.getMaritalStatus().equalsIgnoreCase("Single") && applicant.getAge() >= 35;
            boolean isMarried = applicant.getMaritalStatus().equalsIgnoreCase("Married") && applicant.getAge() >= 21;

            if (isSingle && project.getUnits("2-room") > 0) {
                result.add(project);
            } else if (isMarried && (project.getUnits("2-room") > 0 || project.getUnits("3-room") > 0)) {
                result.add(project);
            }
        }
        return result;
    }

    public static void submitEnquiry(Applicant applicant, String projectName, String message) {
        String enquiryId = UUID.randomUUID().toString();
        Enquiry enquiry = new Enquiry(enquiryId, applicant.getNRIC(), projectName, message);
        enquiries.add(enquiry);
        System.out.println("Enquiry submitted successfully.");
    }

    public static List<Enquiry> getApplicantEnquiries(Applicant applicant) {
        List<Enquiry> result = new ArrayList<>();
        for (Enquiry e : enquiries) {
            if (e.getUserNric().equals(applicant.getNRIC())) {
                result.add(e);
            }
        }
        return result;
    }

    public static boolean deleteEnquiry(Applicant applicant, String enquiryId) {
        Iterator<Enquiry> iterator = enquiries.iterator();
        while (iterator.hasNext()) {
            Enquiry e = iterator.next();
            if (e.getEnquiryId().equals(enquiryId) && e.getUserNric().equals(applicant.getNRIC())) {
                iterator.remove();
                System.out.println("Enquiry deleted successfully.");
                return true;
            }
        }
        System.out.println("Enquiry not found or you don't have permission to delete it.");
        return false;
    }

    public static boolean editEnquiry(Applicant applicant, String enquiryId, String newMessage) {
        for (Enquiry e : enquiries) {
            if (e.getEnquiryId().equals(enquiryId) && e.getUserNric().equals(applicant.getNRIC())) {
                e.setMessage(newMessage);
                System.out.println("Enquiry updated successfully.");
                return true;
            }
        }
        System.out.println("Enquiry not found or you don't have permission to edit it.");
        return false;
    }
}
