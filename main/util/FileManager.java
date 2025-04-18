package main.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
}