package main.models;

public class HDBOfficer extends Applicant {

    public enum RegistrationStatus { NONE, PENDING, APPROVED }

    private String handlingProjectId;          // project ID once approved
    private RegistrationStatus regStatus;      // workflow flag

    public HDBOfficer(String nric, String name,
                      int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
        this.regStatus = RegistrationStatus.NONE;
    }

    /* ---------------  Registration workflow  --------------- */

    /** Officer submits request – state → PENDING               */
    public void submitRegistration(String projectId) {
        this.regStatus         = RegistrationStatus.PENDING;
        this.handlingProjectId = projectId;
    }

    /** Manager approves – state → APPROVED                     */
    public void approveRegistration(String projectId) {
        if (!projectId.equalsIgnoreCase(this.handlingProjectId))
            throw new IllegalStateException("Project mismatch during approval");
        this.regStatus = RegistrationStatus.APPROVED;
    }

    /** Manager rejects OR officer cancels                      */
    public void cancelRegistration() {
        this.regStatus         = RegistrationStatus.NONE;
        this.handlingProjectId = null;
    }

    /* ---------------  Convenience  --------------- */

    public boolean isRegistrationPending() { return regStatus == RegistrationStatus.PENDING; }
    public boolean isHandlingProject()     { return regStatus == RegistrationStatus.APPROVED; }
    public String  getHandlingProjectId()  { return handlingProjectId; }
    public RegistrationStatus getRegStatus(){ return regStatus; }

    @Override public String getRole() { return "HDBOfficer"; }

    @Override
    public String toString() {
        String s =
                regStatus == RegistrationStatus.NONE     ? "None"
                        : regStatus == RegistrationStatus.PENDING  ? "PENDING‑" + handlingProjectId
                        : handlingProjectId;
        return super.toString() + " | Handling: " + s;
    }
}
