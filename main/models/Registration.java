package main.models;

/**
 * Represents an officer's registration to handle a BTO project.
 * Associates an HDBOfficer, the registration status, and the project.
 */
public class Registration {
    private final HDBOfficer officer;
    private HDBOfficer.RegistrationStatus status;
    private final BTOProject project;

    /**
     * Constructs a new Registration record.
     *
     * @param officer the officer requesting registration
     * @param status  the current registration status
     * @param project the target BTO project
     */
    public Registration(HDBOfficer officer, HDBOfficer.RegistrationStatus status, BTOProject project) {
        this.officer = officer;
        this.status = status;
        this.project = project;
    }

    /**
     * Returns the officer in this registration.
     *
     * @return HDBOfficer
     */
    public HDBOfficer getOfficer() {
        return officer;
    }

    /**
     * Returns the project the officer registered for.
     *
     * @return BTOProject
     */
    public BTOProject getProject() {
        return project;
    }

    /**
     * Returns the current status of the registration.
     *
     * @return registration status
     */
    public HDBOfficer.RegistrationStatus getStatus() {
        return status;
    }

    /**
     * Updates the registration status.
     *
     * @param status new status
     */
    public void setStatus(HDBOfficer.RegistrationStatus status) {
        this.status = status;
    }
}
