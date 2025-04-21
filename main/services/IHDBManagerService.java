package main.services;

import main.models.HDBManager;
import main.models.BTOProject;
import main.models.HDBOfficer;
import main.models.Application;

import java.util.Date;
import java.util.List;

public interface IHDBManagerService {
    boolean createProject(HDBManager manager, 
                          String name, 
                          String neighborhood, 
                          Date startDate, 
                          Date endDate,
                          List<String> flatTypes, 
                          int twoRoomUnits, 
                          int threeRoomUnits,
                          int twoRoomPrice,
                          int threeRoomPrice);

    List<BTOProject> viewAllProjects();
    List<BTOProject> viewOwnProjects(HDBManager manager);

    void toggleVisibility(HDBManager manager, BTOProject project, boolean visibility);

    void editBTOProject(HDBManager manager, 
                        BTOProject project, 
                        String newName, 
                        String newNeighborhood, 
                        Date newStartDate, 
                        Date newEndDate, 
                        List<String> flatTypes, 
                        int newTwoRoomUnits, 
                        int newThreeRoomUnits);

    void deleteBTOProject(HDBManager manager, BTOProject project);

    boolean handleOfficerRegistration(HDBManager manager, BTOProject project, HDBOfficer officer);

    boolean handleBTOApplication(HDBManager manager, Application application, boolean approve);

    void handleWithdrawal(HDBManager manager, Application application);

    void bookingReport(HDBManager manager, String filter);

    boolean changePassword(HDBManager manager, String oldPassword, String newPassword);

    boolean assignOfficerToProject(HDBManager manager, BTOProject project, String officerNRIC);
    boolean removeOfficerFromProject(HDBManager manager, BTOProject project, String officerNRIC);

}