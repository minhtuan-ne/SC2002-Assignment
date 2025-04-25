package main.app;

import java.time.format.DateTimeFormatter;
import java.util.*;
import main.models.*;

/**
 * Interface for all user role menu handlers (Applicant, Manager, Officer).
 * Provides the base menu loop interaction logic.
 */
public interface IUserHandler {
	/** Formatter for date display: dd/MM/yyyy */
    DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /**
     * Launches the role-specific interaction menu for a logged-in user.
     *
     * @param user the authenticated user
     * @param sc   a shared Scanner for console input
     */
    void run(User user, Scanner sc);
}