package main.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import main.models.Applicant;
import main.models.Application;
import main.models.HDBManager;
import main.models.HDBOfficer;
import main.models.Registration;
import main.models.User;

/**
 * Handles file input/output for users, projects, applications, and
 * registrations.
 * Supports reading from and writing to CSV/TSV-based text files.
 */
public class FileManager {
    /**
     * Reads a tab-separated file and returns parsed data.
     *
     * @param fileName name of file in the /data directory
     * @return list of rows (each row is a list of columns)
     */
    public List<List<String>> readFile(String fileName) {
        List<List<String>> fileList = new ArrayList<>();
        try {
            File myObj = new File("./data/" + fileName);
            Scanner myReader = new Scanner(myObj);

            boolean isFirstLine = true;
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine().trim();
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (!data.isEmpty()) {
                    String[] rawSplitData = data.split("\\s+");
                    List<String> rowData = new ArrayList<>();
                    StringBuilder currentField = new StringBuilder();
                    for (String field : rawSplitData) {
                        if (!currentField.isEmpty()) {
                            currentField.append(" ").append(field);
                            if (!field.endsWith(",")) {
                                rowData.add(currentField.toString());
                                currentField = new StringBuilder();
                            }
                        } else if (field.endsWith(",")) {
                            currentField.append(field);
                        } else {
                            rowData.add(field);
                        }
                    }
                    if (!currentField.isEmpty()) {
                        rowData.add(currentField.toString());
                    }
                    fileList.add(rowData);
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading file: " + fileName);
            e.printStackTrace();
        }
        return fileList;
    }

    /**
     * Returns parsed user/project data grouped by role.
     *
     * @return map of role to parsed CSV data
     */
    public Map<String, List<List<String>>> getDatabyRole() {
        Map<String, List<List<String>>> dataMap = new HashMap<>();
        dataMap.put("Applicant", readFile("ApplicantList.txt"));
        dataMap.put("Manager", readFile("ManagerList.txt"));
        dataMap.put("Officer", readFile("OfficerList.txt"));
        dataMap.put("Project", readFile("ProjectList.txt"));
        return dataMap;
    }

    /**
     * Updates the password for a given user in their role-specific file.
     *
     * @param role        user role (applicant/manager/officer)
     * @param nric        NRIC to update
     * @param newPassword new password to set
     * @throws IOException if file I/O fails
     */
    public void updatePassword(String role, String nric, String newPassword) throws IOException {
        String fileName = switch (role.toLowerCase()) {
            case "applicant" -> "ApplicantList.txt";
            case "manager"   -> "ManagerList.txt";
            case "officer"   -> "OfficerList.txt";
            default           -> throw new IllegalArgumentException("Unknown role: " + role);
        };
        Path path = Paths.get("data", fileName);
        List<String> lines = Files.readAllLines(path);
        String header = lines.remove(0);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split("\\s+");
            if (cols.length < 5) continue;
            if (cols[1].equalsIgnoreCase(nric)) {
                cols[4] = newPassword;
                lines.set(i, String.join("\t", cols));
                break;
            }
        }
        lines.add(0, header);
        Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Loads all officers from a TSV file.
     *
     * @param path path to the officer file
     * @return list of HDBOfficer objects
     */
    public List<HDBOfficer> loadOfficersFromFile(String path) {
        List<HDBOfficer> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                HDBOfficer o = new HDBOfficer(parts[1], parts[0], Integer.parseInt(parts[2]), parts[3], parts[4]);
                o.setRegStatus(HDBOfficer.RegistrationStatus.valueOf(parts[5]));
                o.setHandlingProjectId(parts[6].equals("null") ? null : parts[6]);
                list.add(o);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Saves officer data to the specified TSV file.
     *
     * @param path     file path
     * @param officers list of officers to write
     */
    public void saveOfficersToFile(String path, List<HDBOfficer> officers) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Name\tNRIC\tAge\tMarital Status\tPassword\tRegStatus\tHandlingProjectID");
            Map<String, HDBOfficer> officerMap = officers.stream()
                .collect(Collectors.toMap(HDBOfficer::getNRIC, o -> o, (a, b) -> b));
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t");
                    String nric = parts[1];
                    if (officerMap.containsKey(nric)) {
                        lines.add(officerMap.remove(nric).toCSVRow());
                    } else {
                        lines.add(line);
                    }
                }
            }
            for (HDBOfficer o : officerMap.values()) {
                lines.add(o.toCSVRow());
            }
            Files.write(Paths.get(path), lines, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves a new BTO project to the ProjectList file.
     *
     * @param managerNRIC    the NRIC of the manager creating the project
     * @param managerName    the name of the manager
     * @param projectName    the name of the project
     * @param neighborhood   the neighborhood where the project is located
     * @param startDate      the application opening date
     * @param endDate        the application closing date
     * @param twoRoomUnits   number of 2-room units available
     * @param threeRoomUnits number of 3-room units available
     * @param twoRoomPrice   selling price for 2-room units
     * @param threeRoomPrice selling price for 3-room units
     * @param maxOfficers    maximum number of officers that can be assigned
     * @return true if the project was written successfully; false otherwise
     */
    public boolean saveProject(String managerNRIC, String managerName, String projectName, String neighborhood,
                               Date startDate, Date endDate,
                               int twoRoomUnits, int threeRoomUnits,
                               int twoRoomPrice, int threeRoomPrice, int maxOfficers, boolean vis) {
        try {
            Path path = Paths.get("data", "ProjectList.txt");
            List<String> lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();
            if (lines.isEmpty()) {
                lines.add(
                    "Project Name\tNeighborhood\tType 1\tNumber of units for Type 1\tSelling price for Type 1\tType 2\tNumber of units for Type 2\tSelling price for Type 2\tApplication opening date\tApplication closing date\tManager\tOfficer Slot\tOfficer"
                );
            }
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split("\t");
                if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                    System.out.println("Project with this name already exists.");
                    return false;
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String startDateStr = sdf.format(startDate);
            String endDateStr = sdf.format(endDate);
            StringBuilder newProject = new StringBuilder();
            newProject.append(projectName).append("\t");
            newProject.append(neighborhood).append("\t");
            newProject.append("2-Room").append("\t");
            newProject.append(twoRoomUnits).append("\t");
            newProject.append(twoRoomPrice).append("\t");
            newProject.append("3-Room").append("\t");
            newProject.append(threeRoomUnits).append("\t");
            newProject.append(threeRoomPrice).append("\t");
            newProject.append(startDateStr).append("\t");
            newProject.append(endDateStr).append("\t");
            newProject.append(managerName).append("\t");
            newProject.append(maxOfficers).append("\t");
            newProject.append("NULL").append("\t");
            newProject.append(vis).append("\t");
            lines.add(newProject.toString());
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Project saved successfully to file.");
            return true;
        } catch (IOException e) {
            System.out.println("Error saving project to file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads all users from all role-specific files.
     *
     * @return list of all users
     */
    public List<User> loadAllUser() {
        List<User> users = new ArrayList<>();
        Map<String, List<List<String>>> data = getDatabyRole();
        List<List<String>> applicantData = data.get("Applicant");
        for (List<String> row : applicantData) {
            if (row.size() >= 5) {
                String name = row.get(0);
                String nric = row.get(1);
                int age = Integer.parseInt(row.get(2));
                String maritalStatus = row.get(3);
                String password = row.get(4);
                users.add(new Applicant(nric, name, age, maritalStatus, password));
            }
        }
        List<List<String>> managerData = data.get("Manager");
        for (List<String> row : managerData) {
            if (row.size() >= 5) {
                String name = row.get(0);
                String nric = row.get(1);
                int age = Integer.parseInt(row.get(2));
                String maritalStatus = row.get(3);
                String password = row.get(4);
                users.add(new HDBManager(nric, name, age, maritalStatus, password));
            }
        }
        List<List<String>> officerData = data.get("Officer");
        for (List<String> row : officerData) {
            if (row.size() >= 7) {
                String name = row.get(0);
                String nric = row.get(1);
                int age = Integer.parseInt(row.get(2));
                String maritalStatus = row.get(3);
                String password = row.get(4);
                HDBOfficer.RegistrationStatus regStatus = HDBOfficer.RegistrationStatus.valueOf(row.get(5));
                String handlingProjectId = row.get(6).equals("null") ? null : row.get(6);
                HDBOfficer officer = new HDBOfficer(nric, name, age, maritalStatus, password);
                officer.setRegStatus(regStatus);
                officer.setHandlingProjectId(handlingProjectId);
                users.add(officer);
            }
        }
        return users;
    }

    /**
     * Saves a BTO application to file.
     *
     * @param app application object to save
     */
    public void saveApplication(Application app) {
        try {
            Path path = Paths.get("data", "ApplicationList.txt");
            List<String> lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();
            if (lines.isEmpty()) {
                lines.add("Applicant\tProject name\tType\tStatus\tPrevStatus");
            }
            StringBuilder newApplication = new StringBuilder();
            newApplication.append(app.getApplicant().getNRIC()).append("\t");
            newApplication.append(app.getProjectName()).append("\t");
            newApplication.append(app.getFlatType()).append("\t");
            newApplication.append(app.getStatus()).append("\t");
            newApplication.append(app.getPrevStatus() == null ? "null" : app.getPrevStatus()).append("\t");
            lines.add(newApplication.toString());
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Application saved successfully to file.");
        } catch (IOException e) {
            System.out.println("Error saving application to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the application status for a given user.
     *
     * @param nric the NRIC of the applicant whose record is being updated
     * @param app  the Application object containing the new status and previous status
     */
    public void updateApplication(String nric, Application app) {
        try {
            Path path = Paths.get("data", "ApplicationList.txt");
            if (!Files.exists(path)) {
                System.out.println("ApplicationList.txt not found.");
                return;
            }
            List<String> lines = Files.readAllLines(path);
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("Applicant") || line.isBlank()) {
                    updatedLines.add(line);
                    continue;
                }
                String[] cols = line.split("\t");
                if (cols.length >= 4 && cols[0].equals(nric)) {
                    if(!app.getStatus().equals("Withdrawn")){
                        cols[3] = app.getStatus();
                        cols[4] = app.getPrevStatus();
                        updatedLines.add(String.join("\t", cols));    
                    }
                } else {
                    updatedLines.add(line);
                }
            }
            Files.write(path, updatedLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Application updated successfully.");
        } catch (IOException e) {
            System.out.println("Error updating application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves a new registration record to file.
     *
     * @param regist registration object
     */
    public void saveRegistration(Registration regist) {
        try {
            Path path = Paths.get("data", "RegistrationList.txt");
            List<String> lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();
            if (lines.isEmpty()) {
                lines.add("Officer\tProject name\tStatus");
            }
            StringBuilder newRegistration = new StringBuilder();
            newRegistration.append(regist.getOfficer().getNRIC()).append("\t");
            newRegistration.append(regist.getProject().getProjectName()).append("\t");
            newRegistration.append(regist.getStatus()).append("\t");
            lines.add(newRegistration.toString());
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Registration saved successfully to file.");
        } catch (IOException e) {
            System.out.println("Error saving registration to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates a registration's status for a given officer and project.
     *
     * @param nric        the NRIC of the officer whose registration is being updated
     * @param projectName the name of the project for which registration is updated
     * @param newStatus   the new RegistrationStatus to apply
     */
    public void updateRegistration(String nric, String projectName, HDBOfficer.RegistrationStatus newStatus) {
        try {
            Path path = Paths.get("data", "RegistrationList.txt");
            if (!Files.exists(path)) {
                System.out.println("RegistrationList.txt not found.");
                return;
            }
            List<String> lines = Files.readAllLines(path);
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("Officer") || line.isBlank()) {
                    updatedLines.add(line);
                    continue;
                }
                String[] cols = line.split("\t");
                if (cols.length >= 3 && cols[0].equals(nric) && cols[1].equals(projectName)) {
                    cols[2] = newStatus.toString();
                    updatedLines.add(String.join("\t", cols));
                } else {
                    updatedLines.add(line);
                }
            }
            Files.write(path, updatedLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Registration updated successfully.");
        } catch (IOException e) {
            System.out.println("Error updating registration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates officer status and assignment in officer file.
     *
     * @param projectName  the project name
     * @param officerNRIC  NRIC of the officer
     * @param isAssigning  true to assign, false to remove
     * @return true if update succeeded, false otherwise
     */
    public boolean updateOfficerInProject(String projectName, String officerNRIC, boolean isAssigning) {
        try {
            Path path = Paths.get("./data", "OfficerList.txt");
            if (!Files.exists(path)) {
                System.out.println("OfficerList.txt file not found at " + path.toAbsolutePath());
                return false;
            }
            List<String> lines = Files.readAllLines(path);
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("Officer") || line.isBlank()) {
                    updatedLines.add(line);
                    continue;
                }
                String[] cols = line.split("\t");
                if (cols.length >= 6 && cols[1].equals(officerNRIC)) {
                    // Found the matching line, update the status
                    cols[5] = isAssigning ? HDBOfficer.RegistrationStatus.APPROVED.toString() : HDBOfficer.RegistrationStatus.NONE.toString();
                    cols[6] = projectName;
                    updatedLines.add(String.join("\t", cols));
                } else {
                    updatedLines.add(line);
                }
            }
            Files.write(path, updatedLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Officer updated successfully.");
            return true;
        } catch (IOException e) {
            System.out.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates project’s assigned officer list and slot information.
     *
     * @param projectName name of the project
     * @param officerNRIC NRIC of the officer
     * @param officerName name of the officer
     * @param isAssigning true to assign, false to remove
     * @return true if update succeeded, false otherwise
     */
    public boolean updateProjectOfficer(String projectName, String officerNRIC, String officerName,
                                        boolean isAssigning) {
        try {
            Path path = Paths.get("./data", "ProjectList.txt");
            if (!Files.exists(path)) {
                System.out.println("ProjectList.txt file not found at " + path.toAbsolutePath());
                return false;
            }
            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty()) {
                System.out.println("ProjectList.txt file is empty");
                return false;
            }
            boolean projectFound = false;
            int projectLineIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                String[] cols = lines.get(i).split("\t");
                if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                    projectFound = true;
                    projectLineIndex = i;
                    break;
                }
            }
            if (!projectFound) {
                System.out.println("Project not found: " + projectName);
                return false;
            }
            String line = lines.get(projectLineIndex);
            String[] cols = line.split("\t");
            String assignedOfficers = cols.length > 12 ? cols[12] : "";
            List<String> officerList = new ArrayList<>();
            if (!assignedOfficers.isEmpty()) {
                String[] officersArray = assignedOfficers.split(",");
                List<String> officers = new ArrayList<>(Arrays.asList(officersArray));
                if (!officers.isEmpty() && officers.get(0).equals("NULL")) {
                    officers.remove(0);
                }
                for (String off : officers) {
                    String trimmed = off.trim();
                    if (!trimmed.isEmpty()) officerList.add(trimmed);
                }
            }
            int currentOfficerSlot = cols.length > 11 ? Integer.parseInt(cols[11].trim()) : 10;
            boolean changed = false;
            if (isAssigning) {
                if (!officerList.contains(officerName)) {
                    officerList.add(officerName);
                    currentOfficerSlot = Math.max(currentOfficerSlot, officerList.size());
                    changed = true;
                } else {
                    System.out.println("Officer already assigned to this project");
                    return false;
                }
            } else {
                if (officerList.remove(officerName)) {
                    changed = true;
                    if (officerList.isEmpty()) officerList.add("NULL");
                } else {
                    System.out.println("Officer not found in project");
                    return false;
                }
            }
            if (!changed) return false;
            StringBuilder newLine = new StringBuilder();
            for (int i = 0; i < Math.min(11, cols.length); i++) {
                newLine.append(cols[i]).append("\t");
            }
            newLine.append(currentOfficerSlot);
            newLine.append("\t").append(String.join(", ", officerList));
            lines.set(projectLineIndex, newLine.toString());
            Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Project updated successfully");
            updateOfficerInProject(projectName, officerNRIC, isAssigning);
            return true;
        } catch (IOException e) {
            System.out.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the project details (except price fields).
     *
     * @param projectName      existing project name
     * @param newProjectName   new project name
     * @param neighborhood     new neighborhood
     * @param startDate        new opening date
     * @param endDate          new closing date
     * @param twoRoomUnits     updated 2-room unit count
     * @param threeRoomUnits   updated 3-room unit count
     * @return true if update succeeded; false otherwise
     * @throws IOException if file I/O fails
     */
    public boolean updateProject(String projectName, String newProjectName, String neighborhood,
                                 Date startDate, Date endDate, int twoRoomUnits, int threeRoomUnits) throws IOException {
        Path path = Paths.get("./data/ProjectList.txt");
        if (!Files.exists(path)) {
            System.out.println("Project file not found at: " + path.toAbsolutePath());
            return false;
        }
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            System.out.println("Project file is empty.");
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String startDateStr = sdf.format(startDate);
        String endDateStr = sdf.format(endDate);
        boolean projectFound = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split("\t");
            if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                projectFound = true;
                StringBuilder updatedLine = new StringBuilder();
                updatedLine.append(newProjectName).append("\t");
                updatedLine.append(neighborhood).append("\t");
                updatedLine.append(cols[2]).append("\t");
                updatedLine.append(twoRoomUnits).append("\t");
                updatedLine.append(cols[4]).append("\t");
                updatedLine.append(cols[5]).append("\t");
                updatedLine.append(threeRoomUnits).append("\t");
                updatedLine.append(cols[7]).append("\t");
                updatedLine.append(startDateStr).append("\t");
                updatedLine.append(endDateStr).append("\t");
                for (int j = 10; j < cols.length; j++) {
                    updatedLine.append(cols[j]);
                    if (j < cols.length - 1) updatedLine.append("\t");
                }
                lines.set(i, updatedLine.toString());
                System.out.println("Found and updating project: " + projectName);
                break;
            }
        }
        if (!projectFound) {
            System.out.println("Project not found: " + projectName);
            return false;
        }
        Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Project file updated successfully at: " + path.toAbsolutePath());
        return true;
    }

    public void updateProjectVisibility(String projectName, boolean visibility) throws IOException {
        Path path = Paths.get("./data/ProjectList.txt");
        if (!Files.exists(path)) {
            System.out.println("Project file not found at: " + path.toAbsolutePath());
            return;
        }
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            System.out.println("Project file is empty.");
            return;
        }
    
        boolean projectFound = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split("\t");
            if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {  // match the project name
                projectFound = true;
                StringBuilder updatedLine = new StringBuilder();
                for (int j = 0; j < cols.length - 1; j++) {
                    updatedLine.append(cols[j]);
                    if (j < cols.length - 2) updatedLine.append("\t");
                }
                updatedLine.append("\t").append(visibility);
                lines.set(i, updatedLine.toString());
                System.out.println("Found and updated project: " + cols[0]);
                break;
            }
        }
    
        if (!projectFound) {
            System.out.println("Project not found");
            return;
        }
    
        Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Project file updated successfully at: " + path.toAbsolutePath());
    }
    

    /**
     * Deletes a project from the file based on name.
     *
     * @param projectName name of the project to delete
     * @return true if deletion succeeded; false otherwise
     * @throws IOException if file I/O fails
     */
    public boolean deleteProjectFromFile(String projectName) throws IOException {
        Path path = Paths.get("./data/ProjectList.txt");
        if (!Files.exists(path)) {
            System.out.println("Project file not found at: " + path.toAbsolutePath());
            return false;
        }
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            System.out.println("Project file is empty.");
            return false;
        }
        List<String> updatedLines = new ArrayList<>();
        updatedLines.add(lines.get(0));
        boolean projectFound = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split("\t");
            if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                projectFound = true;
                System.out.println("Found project to delete: " + projectName);
            } else {
                updatedLines.add(lines.get(i));
            }
        }
        if (!projectFound) {
            System.out.println("Project not found in file: " + projectName);
            return false;
        }
        Files.write(path, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Project deleted from file successfully at: " + path.toAbsolutePath());
        return true;
    }
}
