package main.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class BTOProject {
    
    private HDBManager manager;               // The manager who owns/created this project
    private String projectName;               // Name of the BTO project
    private String neighborhood;              // E.g., Yishun, Boon Lay, etc.
    public Date startDate;                    // Application opening date
    public Date endDate;                      // Application closing date
    private List<String> flatTypes;      // ["2-room", "3-room"]
    
    private int twoRoomUnitsAvailable;        // How many 2-room units are available
    private int threeRoomUnitsAvailable;      // How many 3-room units are available
    
    private boolean visibility;               // Whether the project is visible to applicants
    private int maxOfficers;                  // Max number of HDB officers that can handle this project
    private List<HDBOfficer> HDBOfficers;// The officers assigned to handle
    private List<Application> applications;

    public BTOProject(HDBManager manager, String projectName, String neighborhood, Date startDate, Date endDate, List<String> flatTypes, int twoRoomUnits, int threeRoomUnits, int maxOfficers) {
        this.manager = manager;
        this.projectName = projectName;
        this.neighborhood = neighborhood;
        this.startDate = startDate;
        this.endDate = endDate;
        this.flatTypes = flatTypes;
        this.twoRoomUnitsAvailable = twoRoomUnits;
        this.threeRoomUnitsAvailable = threeRoomUnits;
        this.maxOfficers = maxOfficers;
        this.visibility = true; 
        this.HDBOfficers = new ArrayList<>();
        this.applications = new ArrayList<>();
    }
    
    public HDBManager getManager() {
        return manager;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<String> getFlatTypes() {
        return flatTypes;
    }

    public void setFlatTypes(ArrayList<String> flatTypes) {
        this.flatTypes = flatTypes;
    }

    public boolean isVisible() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public int getTwoRoomUnitsAvailable() {
        return twoRoomUnitsAvailable;
    }

    public void setTwoRoomUnitsAvailable(int twoRoomUnitsAvailable) {
        this.twoRoomUnitsAvailable = twoRoomUnitsAvailable;
    }

    public int getThreeRoomUnitsAvailable() {
        return threeRoomUnitsAvailable;
    }

    public void setThreeRoomUnitsAvailable(int threeRoomUnitsAvailable) {
        this.threeRoomUnitsAvailable = threeRoomUnitsAvailable;
    }

    public int getMaxOfficers() {
        return maxOfficers;
    }

    public List<HDBOfficer> getHDBOfficers() {
        return HDBOfficers;
    }

    public List<Application> getApplications() {
        return applications; 
    }

    public void addApplication(Application application) {
        if (!applications.contains(application)) {
            applications.add(application);
        }
    }

    public void removeApplication(Application application) {
        applications.remove(application);
    }



    // get available units for each flat type
    public int getUnits(String flatType) {
        if ("2-room".equalsIgnoreCase(flatType)) {
            return twoRoomUnitsAvailable;
        } else if ("3-room".equalsIgnoreCase(flatType)) {
            return threeRoomUnitsAvailable;
        } else {
            // If more flat types exist, handle them or default to 0
            return 0;
        }
    }

    // set units for each flat type
    public void setUnits(String flatType, int units) {
        if ("2-room".equalsIgnoreCase(flatType)) {
            this.twoRoomUnitsAvailable = units;
        } else if ("3-room".equalsIgnoreCase(flatType)) {
            this.threeRoomUnitsAvailable = units;
        } else {
            // If more flat types exist, handle them accordingly
        }
    }

    public boolean decrementFlatCount(String type) {
        int remain = getUnits(type);
        if (remain <= 0) return false;
        setUnits(type, remain - 1);   // create setUnits(..) if missing
        return true;
    }
}
