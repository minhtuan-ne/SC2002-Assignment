package main.models;

public class Registration {
    private final HDBOfficer officer;
    private HDBOfficer.RegistrationStatus status;

    public Registration(HDBOfficer officer, HDBOfficer.RegistrationStatus status){
        this.officer = officer;
        this.status = status;
    }

    public HDBOfficer getOfficer(){
        return officer;
    }

    public HDBOfficer.RegistrationStatus getStatus(){
        return status;
    }

    public void setStatus(HDBOfficer.RegistrationStatus status){
        this.status = status;
    }
}
