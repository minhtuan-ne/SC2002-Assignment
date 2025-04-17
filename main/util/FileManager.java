package main.util;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import main.models.*;

public class FileManager {
    private static final String APPLICANT_FILE = "./data/ApplicantList.csv";
    private static final String MANAGER_FILE = "./data/ManagerList.csv";
    private static final String OFFICER_FILE = "./data/OfficerList.csv";
    private static final String PROJECT_FILE = "./data/ProjectList.csv";
    private static final String CSV_DELIMITER = ";";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Loads all applicants from the CSV file
     */
    public static List<Applicant> loadApplicants() {
        List<Applicant> applicants = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(APPLICANT_FILE))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header line
                }
                
                String[] data = line.split(CSV_DELIMITER);
                if (data.length >= 5) {
                    String name = data[0];
                    String nric = data[1];
                    int age = Integer.parseInt(data[2]);
                    String maritalStatus = data[3];
                    String password = data[4];
                    
                    Applicant applicant = new Applicant(nric, name, age, maritalStatus, password);
                    applicants.add(applicant);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading applicants: " + e.getMessage());
        }
        
        return applicants;
    }
    
    /**
     * Loads all managers from the CSV file
     */
    public static List<HDBManager> loadManagers() {
        List<HDBManager> managers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(MANAGER_FILE))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header line
                }
                
                String[] data = line.split(CSV_DELIMITER);
                if (data.length >= 5) {
                    String name = data[0];
                    String nric = data[1];
                    int age = Integer.parseInt(data[2]);
                    String maritalStatus = data[3];
                    String password = data[4];
                    
                    HDBManager manager = new HDBManager(nric, name, age, maritalStatus, password);
                    managers.add(manager);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading managers: " + e.getMessage());
        }
        
        return managers;
    }
    
    /**
     * Loads all officers from the CSV file
     */
    public static List<HDBOfficer> loadOfficers() {
        List<HDBOfficer> officers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(OFFICER_FILE))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header line
                }
                
                String[] data = line.split(CSV_DELIMITER);
                if (data.length >= 5) {
                    String name = data[0];
                    String nric = data[1];
                    int age = Integer.parseInt(data[2]);
                    String maritalStatus = data[3];
                    String password = data[4];
                    
                    HDBOfficer officer = new HDBOfficer(nric, name, age, maritalStatus, password);
                    officers.add(officer);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading officers: " + e.getMessage());
        }
        
        return officers;
    }
    
    /**
     * Loads all projects from the CSV file
     */
    public static List<BTOProject> loadProjects(Map<String, User> userDatabase) {
        List<BTOProject> projects = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(PROJECT_FILE))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header line
                }
                
                String[] data = line.split(CSV_DELIMITER);
                if (data.length >= 13) {
                    String projectName = data[0];
                    String neighborhood = data[1];
                    
                    // Flat types
                    String type1 = data[2]; 
                    int units1 = Integer.parseInt(data[3]);
                    
                    String type2 = data[5]; 
                    int units2 = Integer.parseInt(data[6]);
                    
                    // Create flat types list
                    ArrayList<String> flatTypes = new ArrayList<>();
                    flatTypes.add(type1);
                    flatTypes.add(type2);
                    
                    // Dates
                    Date openingDate = dateFormat.parse(data[8]);
                    Date closingDate = dateFormat.parse(data[9]);
                    
                    // Find manager by name
                    String managerName = data[10];
                    HDBManager manager = null;
                    for (User user : userDatabase.values()) {
                        if (user instanceof HDBManager && user.getName().equals(managerName)) {
                            manager = (HDBManager) user;
                            break;
                        }
                    }
                    
                    if (manager == null) {
                        System.out.println("Warning: Manager not found for project " + projectName);
                        continue;
                    }
                    
                    // Officer slots
                    int officerSlots = Integer.parseInt(data[11]);
                    
                    // Create project with the available information
                    BTOProject project = new BTOProject(
                        manager,
                        projectName,
                        neighborhood,
                        openingDate,
                        closingDate,
                        flatTypes,
                        getTwoRoomUnits(type1, units1, type2, units2),
                        getThreeRoomUnits(type1, units1, type2, units2),
                        officerSlots
                    );
                    
                    // Find and assign officers
                    String[] officerNames = data[12].split(",");
                    for (String officerName : officerNames) {
                        for (User user : userDatabase.values()) {
                            if (user instanceof HDBOfficer && user.getName().equals(officerName.trim())) {
                                assignOfficerToProject((HDBOfficer) user, project);
                                break;
                            }
                        }
                    }
                    
                    projects.add(project);
                    manager.addProject(project);
                }
            }
        } catch (IOException | ParseException e) {
            System.out.println("Error loading projects: " + e.getMessage());
        }
        
        return projects;
    }
    
    /**
     * Helper method to determine the number of 2-room units
     */
    private static int getTwoRoomUnits(String type1, int units1, String type2, int units2) {
        if (type1.toLowerCase().contains("2-room")) {
            return units1;
        } else if (type2.toLowerCase().contains("2-room")) {
            return units2;
        }
        return 0;
    }
    
    /**
     * Helper method to determine the number of 3-room units
     */
    private static int getThreeRoomUnits(String type1, int units1, String type2, int units2) {
        if (type1.toLowerCase().contains("3-room")) {
            return units1;
        } else if (type2.toLowerCase().contains("3-room")) {
            return units2;
        }
        return 0;
    }
    
    /**
     * Helper method to assign an officer to a project
     */
    private static void assignOfficerToProject(HDBOfficer officer, BTOProject project) {
      project.getHDBOfficers().add(officer);
      
      officer.assignToProject(project.getProjectName());
      
      System.out.println("Officer " + officer.getName() + " assigned to project: " + project.getProjectName());
  }
    
    /**
     * Saves all applicants to the CSV file
     */
    public static void saveApplicants(List<Applicant> applicants) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(APPLICANT_FILE))) {
            // Write header
            writer.println("Name;NRIC;Age;Marital Status;Password");
            
            // Write data
            for (Applicant applicant : applicants) {
                writer.println(String.format("%s;%s;%d;%s;%s",
                    applicant.getName(),
                    applicant.getNRIC(),
                    applicant.getAge(),
                    applicant.getMaritalStatus(),
                    applicant.getPassword()));
            }
        } catch (IOException e) {
            System.out.println("Error saving applicants: " + e.getMessage());
        }
    }
    
    /**
     * Saves all projects to the CSV file
     */
    public static void saveProjects(List<BTOProject> projects) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PROJECT_FILE))) {
            // Write header
            writer.println("Project Name;Neighborhood;Type 1;Number of units for Type 1;Selling price for Type 1;Type 2;Number of units for Type 2;Selling price for Type 2;Application opening date;Application closing date;Manager;Officer Slot;Officer");
            
            // Write data
            for (BTOProject project : projects) {
                ArrayList<String> flatTypes = project.getFlatTypes();
                
                if (flatTypes.size() >= 2) {
                    String type1 = flatTypes.get(0);
                    String type2 = flatTypes.get(1);
                    
                    
                    // Build officer string
                    StringBuilder officerStr = new StringBuilder();
                    List<HDBOfficer> officers = project.getHDBOfficers();
                    for (int i = 0; i < officers.size(); i++) {
                        if (i > 0) {
                            officerStr.append(",");
                        }
                        officerStr.append(officers.get(i).getName());
                    }
                    
                    writer.println(String.format("%s;%s;%s;%d;%d;%s;%d;%d;%s;%s;%s;%d;%s",
                        project.getProjectName(),
                        project.getNeighborhood(),
                        type1,
                        project.getUnits(type1),
                        
                        type2,
                        project.getUnits(type2),
                        
                        dateFormat.format(project.getStartDate()),
                        dateFormat.format(project.getEndDate()),
                        project.getManager().getName(),
                        project.getMaxOfficers(),
                        officerStr.toString()));
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving projects: " + e.getMessage());
        }
    }
}
