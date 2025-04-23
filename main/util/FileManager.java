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
import main.models.HDBOfficer;

public class FileManager {
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
                    // Split by tab or multiple spaces
                    String[] rawSplitData = data.split("\\s+");
                    List<String> rowData = new ArrayList<>();
                    
                    // Process the split data to handle comma-separated values
                    StringBuilder currentField = new StringBuilder();
                    for (String field : rawSplitData) {
                        // If we have an incomplete field (ends with comma)
                        if (!currentField.isEmpty()) {
                            currentField.append(" ").append(field);
                            // If this field doesn't end with comma, we're done with this merged field
                            if (!field.endsWith(",")) {
                                rowData.add(currentField.toString());
                                currentField = new StringBuilder();
                            }
                        } else if (field.endsWith(",")) {
                            // Start of a potential multi-part field
                            currentField.append(field);
                        } else {
                            // Normal field
                            rowData.add(field);
                        }
                    }
                    
                    // Add any remaining field
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

    public Map<String, List<List<String>>> getDatabyRole() {
        Map<String, List<List<String>>> dataMap = new HashMap<>();
        dataMap.put("Applicant", readFile("ApplicantList.txt"));
        dataMap.put("Manager",   readFile("ManagerList.txt"));
        dataMap.put("Officer",   readFile("OfficerList.txt"));
        dataMap.put("Project",   readFile("ProjectList.txt"));
        return dataMap;
    }

    public void updatePassword(String role, String nric, String newPassword) throws IOException {
        // map role → filename
        String fileName = switch(role.toLowerCase()) {
            case "applicant"  -> "ApplicantList.txt";
            case "manager"    -> "ManagerList.txt";
            case "officer"    -> "OfficerList.txt";
            default -> throw new IllegalArgumentException("Unknown role: "+role);
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
                cols[4] = newPassword;                  // replace password
                lines.set(i, String.join("\t", cols));  // re‑build line
                break;
            }
        }
        lines.add(0, header);
        Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
    }
    public List<HDBOfficer> loadOfficersFromFile(String path) {
        List<HDBOfficer> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            reader.readLine(); // skip header
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

    public void saveOfficersToFile(String path, List<HDBOfficer> officers) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Name\tNRIC\tAge\tMarital Status\tPassword\tRegStatus\tHandlingProjectID");

            // Create a map for fast lookup
            Map<String, HDBOfficer> officerMap = officers.stream()
                    .collect(Collectors.toMap(HDBOfficer::getNRIC, o -> o, (a, b) -> b));  // overwrite on duplicate NRIC

            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                reader.readLine(); // skip header
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t");
                    String nric = parts[1];
                    if (officerMap.containsKey(nric)) {
                        lines.add(officerMap.remove(nric).toCSVRow());
                    } else {
                        lines.add(line); // keep untouched lines
                    }
                }
            }

            // Append any remaining new officers
            for (HDBOfficer o : officerMap.values()) {
                lines.add(o.toCSVRow());
            }

            // Write updated lines
            Files.write(Paths.get(path), lines, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    public boolean saveProject(String managerNRIC, String managerName, String projectName, String neighborhood, 
                            Date startDate, Date endDate,
                            int twoRoomUnits, int threeRoomUnits, 
                            int twoRoomPrice, int threeRoomPrice, int maxOfficers) {
        try {
            Path path = Paths.get("data", "ProjectList.txt");
            List<String> lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();
            
            // If file is empty or doesn't exist, add header
            if (lines.isEmpty()) {
                lines.add("Project Name\tNeighborhood\tType 1\tNumber of units for Type 1\tSelling price for Type 1\tType 2\tNumber of units for Type 2\tSelling price for Type 2\tApplication opening date\tApplication closing date\tManager\tOfficer Slot\tOfficer");
            }
            
            // Check if project already exists
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split("\\t");
                if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                    System.out.println("Project with this name already exists.");
                    return false;
                }
            }
            
            // Format dates in dd/MM/yyyy format
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String startDateStr = sdf.format(startDate);
            String endDateStr = sdf.format(endDate);
            
            // Create a new project line
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
            newProject.append("");  // Empty assigned officers list initially
            
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
    
   
    public boolean updateProjectOfficer(String projectName, String officerNRIC, String officerName, boolean isAssigning) {
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
            
            // Find the project line
            for (int i = 0; i < lines.size(); i++) {
                String[] cols = lines.get(i).split("\\t");
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
            
            // Get the project line and its components
            String line = lines.get(projectLineIndex);
            String[] cols = line.split("\\t");
            
            // Parse the officers column (last column)
            String assignedOfficers = "";
            if (cols.length > 12) {
                assignedOfficers = cols[12];
            }
            
            List<String> officerList = new ArrayList<>();
            if (!assignedOfficers.isEmpty()) {
                String[] officers = assignedOfficers.split(",");
                for (String officer : officers) {
                    String trimmedOfficer = officer.trim();
                    if (!trimmedOfficer.isEmpty()) {
                        officerList.add(trimmedOfficer);
                    }
                }
            }
            
            // Get current officer slot count
            int currentOfficerSlot = 10; // Default
            if (cols.length > 11) {
                try {
                    currentOfficerSlot = Integer.parseInt(cols[11].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Warning: Invalid officer slot number: " + cols[11]);
                }
            }
            
            boolean changed = false;
            
            // Handle assignment or removal
            if (isAssigning) {
                if (!officerList.contains(officerName)) {
                    officerList.add(officerName);
                    // Update the officer slot count
                    currentOfficerSlot = Math.max(currentOfficerSlot, officerList.size());
                    changed = true;
                } else {
                    System.out.println("Officer already assigned to this project");
                    return false;
                }
            } else { // removing
                if (officerList.contains(officerName)) {
                    officerList.remove(officerName);
                    changed = true;
                    // We don't decrease the officer slot count when removing
                } else {
                    System.out.println("Officer not found in project");
                    return false;
                }
            }
            
            if (!changed) {
                return false;
            }
            
            // Build a new line preserving all columns up to Manager (index 10)
            StringBuilder newLine = new StringBuilder();
            
            // Add all columns up to Manager (index 10)
            for (int i = 0; i < Math.min(11, cols.length); i++) {
                newLine.append(cols[i]).append("\t");
            }
            
            // Add the updated Officer Slot (index 11)
            newLine.append(currentOfficerSlot);
            
            // Add the updated officer list
            if (!officerList.isEmpty()) {
                newLine.append("\t").append(String.join(", ", officerList));
            }
            
            // Update the line in the file
            lines.set(projectLineIndex, newLine.toString());
            
            // Write all lines back to the file
            Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Project updated successfully");
            
            return true;
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

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
        
        // Format dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String startDateStr = sdf.format(startDate);
        String endDateStr = sdf.format(endDate);
        
        boolean projectFound = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split("\\t");
            if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                projectFound = true;
                
                // Create a new line with updated values
                StringBuilder updatedLine = new StringBuilder();
                updatedLine.append(newProjectName).append("\t");
                updatedLine.append(neighborhood).append("\t");
                
                // Reuse flat types (columns 2-3)
                updatedLine.append(cols[2]).append("\t");
                
                // Update 2-room units (column 3)
                updatedLine.append(twoRoomUnits).append("\t");
                
                // Keep price (column 4)
                updatedLine.append(cols[4]).append("\t");
                
                // Reuse flat type (column 5)
                updatedLine.append(cols[5]).append("\t");
                
                // Update 3-room units (column 6)
                updatedLine.append(threeRoomUnits).append("\t");
                
                // Keep price (column 7)
                updatedLine.append(cols[7]).append("\t");
                
                // Update dates (columns 8-9)
                updatedLine.append(startDateStr).append("\t");
                updatedLine.append(endDateStr).append("\t");
                
                // Keep remaining columns (manager, max officers, assigned officers)
                for (int j = 10; j < cols.length; j++) {
                    updatedLine.append(cols[j]);
                    if (j < cols.length - 1) {
                        updatedLine.append("\t");
                    }
                }
                
                // Replace the line
                lines.set(i, updatedLine.toString());
                System.out.println("Found and updating project: " + projectName);
                break;
            }
        }
        
        if (!projectFound) {
            System.out.println("Project not found: " + projectName);
            return false;
        }
        
        // Write updated content back to file
        Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Project file updated successfully at: " + path.toAbsolutePath());
        return true;
    }
    
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
        updatedLines.add(lines.get(0)); // Keep the header
        
        boolean projectFound = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] cols = lines.get(i).split("\\t");
            if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                projectFound = true;
                System.out.println("Found project to delete: " + projectName);
                // Skip this line (don't add to updatedLines)
            } else {
                updatedLines.add(lines.get(i));
            }
        }
        
        if (!projectFound) {
            System.out.println("Project not found in file: " + projectName);
            return false;
        }
        
        // Write updated content back to file
        Files.write(path, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Project deleted from file successfully at: " + path.toAbsolutePath());
        return true;
    }
}
