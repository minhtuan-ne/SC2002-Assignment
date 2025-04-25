// src/main/models/BTOProject.java
package main.models;

import java.util.Date;
import java.util.List;

/**
 * Represents a Build-To-Order (BTO) project managed by HDB.
 * Contains project details such as name, neighborhood, application dates,
 * available flats, visibility status, and assigned officers.
 */
public class BTOProject {
    private final HDBManager manager;               // The manager who owns/created this project
    private String projectName;                     // Name of the BTO project
    private String neighborhood;                    // E.g., Yishun, Boon Lay, etc.
    public Date startDate;                          // Application opening date
    public Date endDate;                            // Application closing date
    private final List<Flat> flats;                 // Available flats in this project
    private boolean visibility;                     // Whether the project is visible to applicants
    private final int maxOfficers;                  // Max number of HDB officers that can handle this project
    private List<HDBOfficer> officers;              // The officers assigned to handle this project

    /**
     * Constructs a new BTOProject with the given parameters.
     *
     * @param manager         the HDB manager who created this project
     * @param projectName     the name of the project
     * @param neighborhood    the neighborhood where the project is located
     * @param startDate       application opening date
     * @param endDate         application closing date
     * @param flats           list of flats available in this project
     * @param maxOfficers     maximum number of officers allowed
     * @param assignedOfficer initial list of assigned officers
     */
    public BTOProject(HDBManager manager, String projectName, String neighborhood,
                      Date startDate, Date endDate, List<Flat> flats,
                      int maxOfficers, List<HDBOfficer> assignedOfficer, boolean visibility) {
        this.manager = manager;
        this.projectName = projectName;
        this.neighborhood = neighborhood;
        this.startDate = startDate;
        this.endDate = endDate;
        this.flats = flats;
        this.maxOfficers = maxOfficers;
        this.visibility = visibility;
        this.officers = assignedOfficer;
    }

    public BTOProject(HDBManager manager, String projectName, String neighborhood,
                      Date startDate, Date endDate, List<Flat> flats,
                      int maxOfficers, List<HDBOfficer> assignedOfficer) {
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

    /**
     * Returns the HDB manager for this project.
     * @return manager
     */
    public HDBManager getManager() {
        return manager;
    }

    /**
     * Returns the project name.
     * @return projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Updates the project name.
     * @param projectName new name for the project
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Returns the neighborhood of this project.
     * @return neighborhood
     */
    public String getNeighborhood() {
        return neighborhood;
    }

    /**
     * Updates the neighborhood.
     * @param neighborhood new neighborhood value
     */
    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    /**
     * Returns the application opening date.
     * @return startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Updates the application opening date.
     * @param startDate new start date
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the application closing date.
     * @return endDate
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Updates the application closing date.
     * @param endDate new end date
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Checks if the project is visible to applicants.
     * @return true if visible, false otherwise
     */
    public boolean isVisible() {
        return visibility;
    }

    /**
     * Sets the visibility of this project.
     * @param visibility true to show project, false to hide
     */
    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    /**
     * Retrieves the number of units available for the given flat type.
     * @param type flat type (e.g., "2-room" or "3-room")
     * @return available unit count, or 0 if not found
     */
    public int getUnits(String type) {
        return flats.stream()
            .filter(f -> f.getType().equalsIgnoreCase(type))
            .findFirst()
            .map(Flat::getUnits)
            .orElse(0);
    }

    /**
     * Updates the number of units for the specified flat type.
     * @param type  flat type to update
     * @param units new unit count
     */
    public void setUnits(String type, int units) {
        flats.stream()
            .filter(f -> f.getType().equalsIgnoreCase(type))
            .findFirst()
            .ifPresent(f -> f.setUnits(units));
    }

    /**
     * Returns the maximum number of officers allowed.
     * @return maxOfficers
     */
    public int getMaxOfficers() {
        return maxOfficers;
    }

    /**
     * Gets the list of officers assigned to this project.
     * @return list of HDB officers
     */
    public List<HDBOfficer> getOfficers() {
        return officers;
    }

    /**
     * Replaces the current list of officers.
     * @param officers new list of HDB officers
     */
    public void setOfficers(List<HDBOfficer> officers) {
        this.officers = officers;
    }

    /**
     * Adds an officer to this project if not already assigned.
     * @param officer officer to add
     */
    public void addOfficer(HDBOfficer officer) {
        if (!this.officers.contains(officer)) {
            this.officers.add(officer);
        }
    }

    /**
     * Removes the specified officer from this project.
     * @param officer officer to remove
     */
    public void removeOfficer(HDBOfficer officer) {
        this.officers.remove(officer);
    }

    public void removeOfficerByNRIC(String nric){
        this.officers.removeIf(o -> o.getNRIC().equals(nric));
    }
}