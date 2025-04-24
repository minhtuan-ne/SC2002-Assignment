package main.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a HDB Manager in the BTO system.
 * A manager can create, view, and manage BTO projects.
 */
public class HDBManager extends User {
    private final List<BTOProject> projects;

    /**
     * Constructs a new HDB Manager with the given personal details.
     *
     * @param nric          NRIC of the manager
     * @param name          Name of the manager
     * @param age           Age of the manager
     * @param maritalStatus Marital status of the manager
     * @param password      Password for login
     */
    public HDBManager(String nric, String name, int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
        this.projects = new ArrayList<>();
    }

    /**
     * Returns the list of BTO projects created by this manager.
     *
     * @return list of BTOProject
     */
    public List<BTOProject> getProjects() {
        return projects;
    }

    /**
     * Adds a BTO project to this manager's project list.
     *
     * @param project the project to add
     */
    public void addProject(BTOProject project) {
        if (!projects.contains(project)) {
            projects.add(project);
        }
    }

    /**
     * Removes a BTO project from this manager's project list.
     *
     * @param project the project to remove
     */
    public void removeProject(BTOProject project) {
        projects.remove(project);
    }

    /**
     * Returns the role of this user.
     *
     * @return always returns "HDB Manager"
     */
    @Override
    public String getRole() {
        return "HDB Manager";
    }
}
