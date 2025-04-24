package main.models;

import java.util.Date;
import java.util.List;

public class BTOProject {
    private final HDBManager manager;               // The manager who owns/created this project
    private String projectName;                     // Name of the BTO project
    private String neighborhood;                    // E.g., Yishun, Boon Lay, etc.
    public Date startDate;                          // Application opening date
    public Date endDate;                            // Application closing date
    private final List<Flat> flats;                 // Available flat in this project
    private boolean visibility;                     // Whether the project is visible to applicants
    private final int maxOfficers;                  // Max number of HDB officers that can handle this project
    private List<HDBOfficer> officers;              // The officers assigned to handle

    public BTOProject(HDBManager manager, String projectName, String neighborhood, Date startDate, Date endDate, List<Flat> flats, int maxOfficers, List<HDBOfficer> assignedOfficer) {
        this.manager = manager;
        this.projectName = projectName;
        this.neighborhood = neighborhood;
        this.startDate = startDate;
        this.endDate = endDate;
        this.flats = flats;
        this.maxOfficers = maxOfficers;
        this.visibility = true; 
        this.officers = assignedOfficer;
    }
    
    public HDBManager getManager() { return manager; }

    public String getProjectName() { return projectName; }

    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getNeighborhood() { return neighborhood; }

    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

    public Date getStartDate() { return startDate; }

    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }

    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public boolean isVisible() { return visibility; }

    public void setVisibility(boolean visibility) { this.visibility = visibility; }

    public int getUnits(String type) { 
        return flats.stream().filter(f -> f.getType().equalsIgnoreCase(type))
            .findFirst()
            .map(Flat::getUnits)
            .orElse(0);
    }

    public void setUnits(String type, int units){
        flats.stream().filter(f -> f.getType().equalsIgnoreCase(type))
            .findFirst()
            .ifPresent(f -> f.setUnits(units));
    }

    public int getMaxOfficers() { return maxOfficers; }

    public List<HDBOfficer> getOfficers() { return officers; }

    public void setOfficers(List<HDBOfficer> officers) {
        this.officers = officers;
    }
    
    public void addOfficer(HDBOfficer officer) {
        if (!this.officers.contains(officer)) {
            this.officers.add(officer);
        }
    }
    
    public void removeOfficer(HDBOfficer officer) {
        this.officers.remove(officer);
    }
}
