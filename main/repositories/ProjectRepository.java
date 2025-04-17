package main.repositories;

import main.models.BTOProject;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository implements IProjectRepository {
    // one common list to store all projects created by all managers
    private static final List<BTOProject> PROJECTS = new ArrayList<>();


    @Override
    public List<BTOProject> getAllProjects(){
        return PROJECTS;
    }

    @Override
    public void addProject(BTOProject project){
        PROJECTS.add(project);
    }

    @Override
    public void removeProject(BTOProject project){
        PROJECTS.remove(project);
    }

}
