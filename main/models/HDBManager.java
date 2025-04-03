package main.models;

import java.util.ArrayList;

class HDBManager {
    // project list created by each manager
    private ArrayList<BTOProject> projects;

    // constructor
    public HDBManager(String nric, String password, int age, String maritalStatus){
        super(nric, password, age, maritalStatus);
        projects = new ArrayList<>();
    }

    // get created project by this manager
    public ArrayList<BTOProject> getProjects(){
        return projects;
    }

    // add another project to the list
    public void addProject(BTOProject project){
        if(!projects.contains(project)){
            projects.add(project);
        }
    }

    // remove a project from the list
    public void removeProject(BTOProject project){
        projects.remove(project);
    }

    
}
