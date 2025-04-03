package main.models;

import java.util.ArrayList;

public class ProjectRepository {
    // one common list to store all projects created by all managers
    private static final ArrayList<BTOProject> PROJECTS = new ArrayList<>();

    // method for managers to view the list of all projects
    public static ArrayList<BTOProject> getAllProjects(){
        return PROJECTS;
    }

    // add new projects created by managers to the common list
    public static void addProject(BTOProject project){
        PROJECTS.add(project);
    }

    // remove a project
    public static void removeProject(BTOProject project){
        PROJECTS.remove(project);
    }

}
