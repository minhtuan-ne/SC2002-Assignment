package main.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileManager implements IFileManager {
    @Override
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

    @Override
    public Map<String, List<List<String>>> getDatabyRole() {
        Map<String, List<List<String>>> dataMap = new HashMap<>();
        dataMap.put("Applicant", readFile("ApplicantList.txt"));
        dataMap.put("Manager",   readFile("ManagerList.txt"));
        dataMap.put("Officer",   readFile("OfficerList.txt"));
        dataMap.put("Project",   readFile("ProjectList.txt"));
        return dataMap;
    }

    @Override
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
    
 
    public boolean saveProject(String managerNRIC, String managerName, String projectName, String neighborhood, 
                            Date startDate, Date endDate, List<String> flatTypes,
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
            Path path = Paths.get("data", "ProjectList.txt");
            if (!Files.exists(path)) {
                System.out.println("DEBUG: ProjectList.txt file not found at " + path.toAbsolutePath());
                return false;
            }
            
            // Read all lines from the file
            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty()) {
                System.out.println("DEBUG: ProjectList.txt file is empty");
                return false;
            }
            
            System.out.println("DEBUG: Found " + lines.size() + " lines in file");
            System.out.println("DEBUG: Action: " + (isAssigning ? "Assigning" : "Removing") + 
                               " officer " + officerName + " to/from project " + projectName);
            
            boolean projectFound = false;
            int projectLineIndex = -1;
            
            // Find the project line
            for (int i = 0; i < lines.size(); i++) {
                String[] cols = lines.get(i).split("\\t");
                if (cols.length > 0 && cols[0].equalsIgnoreCase(projectName)) {
                    projectFound = true;
                    projectLineIndex = i;
                    System.out.println("DEBUG: Found project at line " + i + ": " + lines.get(i));
                    break;
                }
            }
            
            if (!projectFound) {
                System.out.println("DEBUG: Project not found: " + projectName);
                return false;
            }
            
            // Get the project line and its components
            String[] cols = lines.get(projectLineIndex).split("\\t");
            System.out.println("DEBUG: Project line has " + cols.length + " columns");
            
            // Parse the officers column (last column)
            String assignedOfficers = cols.length > 12 ? cols[12] : "";
            System.out.println("DEBUG: Current officers: " + assignedOfficers);
            
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
            System.out.println("DEBUG: Parsed officer list: " + officerList);
            
            boolean changed = false;
            
            // Handle assignment or removal
            if (isAssigning) {
                if (!officerList.contains(officerName)) {
                    officerList.add(officerName);
                    changed = true;
                    System.out.println("DEBUG: Added officer " + officerName);
                } else {
                    System.out.println("DEBUG: Officer already in list");
                    return false;
                }
            } else { // removing
                if (officerList.contains(officerName)) {
                    officerList.remove(officerName);
                    changed = true;
                    System.out.println("DEBUG: Removed officer " + officerName);
                } else {
                    System.out.println("DEBUG: Officer not found in list");
                    return false;
                }
            }
            
            if (!changed) {
                System.out.println("DEBUG: No changes made to officer list");
                return false;
            }
            
            // Create a new line
            StringBuilder newLine = new StringBuilder();
            for (int i = 0; i < Math.min(cols.length, 12); i++) {
                newLine.append(cols[i]).append("\t");
            }
            
            // Add officer slot column if needed
            if (cols.length > 11) {
                newLine.append(cols[11]);
            } else {
                newLine.append("10"); // Default max officers
            }
            
            // Add the updated officer list
            if (!officerList.isEmpty()) {
                newLine.append("\t").append(String.join(", ", officerList));
            }
            
            System.out.println("DEBUG: New line: " + newLine.toString());
            
            // Update the line in the file
            lines.set(projectLineIndex, newLine.toString());
            
            // Write all lines back to the file
            Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("DEBUG: File updated successfully");
            
            return true;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}