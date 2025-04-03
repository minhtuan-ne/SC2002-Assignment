package main.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class FileManager {
  private HashMap<String, List<List<String>>> DatabyRole = new HashMap<String, List<List<String>>>();

  public List<List<String>> readFile(String fileName) {
    List<List<String>> FileList = new ArrayList<>();
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
          List<String> applicantData = new ArrayList<>();

          for (String field : splitData) {
            applicantData.add(field);
          }

          FileList.add(applicantData);
        }
      }
      myReader.close();
    } catch (FileNotFoundException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
    return FileList;
  }

  public HashMap<String, List<List<String>>> getDatabyRole() {
    DatabyRole.put("Applicant", readFile("ApplicantList.txt"));
    DatabyRole.put("Manager", readFile("ManagerList.txt"));
    DatabyRole.put("Officer", readFile("OfficerList.txt"));
    DatabyRole.put("Project", readFile("ProjectList.txt"));

    return DatabyRole;
  }
}
