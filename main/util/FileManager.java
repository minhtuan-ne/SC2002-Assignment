package main.util;

import java.io.File;
import java.io.FileNotFoundException;
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
                    String[] splitData = data.split("\\s+");
                    fileList.add(Arrays.asList(splitData));
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
}
