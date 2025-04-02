package main.util;

import java.io.File;  
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner; 

public class FileManager {
      private List<List<String>> FileList = new ArrayList<>();  

      public List<List<String>> readFile(String fileName) {  
        try {
            File myObj = new File("./data/"+fileName);
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

      public List<List<String>> getApplicantList() {  
        return readFile("ApplicantList.txt");
      }

      public List<List<String>> getManagerList() {  
        return readFile("ManagerList.txt");
      }

      public List<List<String>> getOfficerList() {  
        return readFile("OfficerList.txt");
      }

      public List<List<String>> getProjectList() {  
        return readFile("ProjectList.txt");
      }
      
}
