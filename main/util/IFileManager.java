package main.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Date;

public interface IFileManager {
    Map<String, List<List<String>>> getDatabyRole();

    List<List<String>> readFile(String fileName);

    void updatePassword(String role, String nric, String newPassword) throws IOException;

    boolean saveProject(String managerNRIC, String managerName, String projectName, String neighborhood, 
                      Date startDate, Date endDate, List<String> flatTypes,
                      int twoRoomUnits, int threeRoomUnits,
                      int twoRoomPrice, int threeRoomPrice, int maxOfficers);
    
    boolean updateProjectOfficer(String projectName, String officerNRIC, String officerName, boolean isAssigning);
}
