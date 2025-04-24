package main.models;

public class Registration {
    private final HDBOfficer officer;
    private HDBOfficer.RegistrationStatus status;
    private final BTOProject project;

    public Registration(HDBOfficer officer, HDBOfficer.RegistrationStatus status, BTOProject project){
        this.officer = officer;
        this.status = status;
        this.project = project;
    }

    public HDBOfficer getOfficer(){
        return officer;
    }

    public BTOProject getProject(){
        return project;
    }

    public HDBOfficer.RegistrationStatus getStatus(){
        return status;
    }

    public void setStatus(HDBOfficer.RegistrationStatus status){
        this.status = status;
    }
}
