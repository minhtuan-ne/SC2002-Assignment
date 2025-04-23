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

    public boolean isRegistrationPending() { return regStatus == RegistrationStatus.PENDING; }
    public boolean isHandlingProject()     { return regStatus == RegistrationStatus.APPROVED; }
    public String  getHandlingProjectId()  { return handlingProjectId; }
    public RegistrationStatus getRegStatus(){ return regStatus; }

    @Override public String getRole() { return "HDBOfficer"; }

    @Override
    public String toString() {
        String s =
                regStatus == RegistrationStatus.NONE ? "None"
                        : regStatus == RegistrationStatus.PENDING  ? "PENDING‑" + handlingProjectId
                        : handlingProjectId;
        return super.toString() + " | Handling: " + s;
    }
    public void setRegStatus(RegistrationStatus status) {
        this.regStatus = status;
    }

    public void setHandlingProjectId(String id) {
        this.handlingProjectId = id;
    }

    public String toCSVRow() {
        return String.join("\t", getName(), getNRIC(), String.valueOf(getAge()),
                getMaritalStatus(), getPassword(), regStatus.name(),
                handlingProjectId == null ? "null" : handlingProjectId);
    }
}
