package main.repositories;

import main.models.BTOProject;
import java.util.List;

public interface IProjectRepository {
    List<BTOProject> getAllProjects();
    void addProject(BTOProject project);
    void removeProject(BTOProject project);
} 