package main.models;

import java.util.ArrayList;
import java.util.List;

public class HDBManager extends User {
    // project list created by each manager
    private List<BTOProject> projects;

    // constructor
    public HDBManager(String nric, String name, int age, String maritalStatus, String password) {
        super(nric, name, age, maritalStatus, password);
        this.projects = new ArrayList<>();
    }

    // get created project by this manager
    public List<BTOProject> getProjects(){
        return projects;
    }

    // add another project to the list
    public void addProject(BTOProject project){
        if(!projects.contains(project)){
            projects.add(project);
        }
    }

    @Override
    public String getRole() { return "HDB Manager"; }

    // remove a project from the list
    public void removeProject(BTOProject project){
        projects.remove(project);
    }

    
}
