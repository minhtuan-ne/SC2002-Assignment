package main.util;

import main.models.Applicant;
import main.models.HDBManager;
import main.models.HDBOfficer;
import main.models.User;

import java.util.*;

public class Authenticator {
    private final IFileManager fileManager;
    private final Scanner sc;
    private List<User> users = new ArrayList<>();

    public Authenticator(IFileManager fileManager) {
        this.fileManager = fileManager;
        this.sc = new Scanner(System.in);
    }
    public List<User> getUsers() {
        return users;
    }

    /**
     * Prompts for NRIC/password, then returns the real User object.
     * Checks roles in the fixed order: Manager → Officer → Applicant.
     */
    public User logIn() {
        Map<String, List<List<String>>> data = fileManager.getDatabyRole();
        // Fixed lookup order:
        List<String> rolesOrder = Arrays.asList("Manager", "Officer", "Applicant");

        System.out.println("Welcome back. Please log in.");
        while (true) {
            System.out.print("User ID: ");
            String userID = sc.nextLine().trim().toUpperCase();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            // Validate format
            if (!User.isValidNRIC(userID)) {
                System.out.println("Incorrect NRIC format. Please try again.");
                continue;
            }

            boolean foundAny = false;
            // Check each role in order
            for (String role : rolesOrder) {
                List<List<String>> list = data.get(role);
                if (list == null) continue;

                for (List<String> record : list) {
                    // record = [ name, nric, age, maritalStatus, password ]
                    String recNric = record.get(1).toUpperCase();
                    String recPass = record.get(4);

                    if (!recNric.equals(userID)) continue;
                    // We found a matching NRIC
                    foundAny = true;

                    if (!recPass.equals(password)) {
                        System.out.println("Incorrect password. Please try again.");
                        // break out of the record‐loop back to the while(true)
                        break;
                    }

                    // Successful login! build the right User subtype:
                    String name          = record.get(0);
                    int    age           = Integer.parseInt(record.get(2));
                    String maritalStatus = record.get(3);

                    User u= null;
                    switch (role) {
                        case "Manager":
                            u = new HDBManager(userID, name, age, maritalStatus, password);
                            users.add(u);
                            break;
                        case "Officer":
                            List<HDBOfficer> officers = fileManager.loadOfficersFromFile("data/OfficerList.txt");
                            for (HDBOfficer officer : officers) {
                                if (!officer.getNRIC().equalsIgnoreCase(userID)) continue;

                                foundAny = true;
                                if (!officer.getPassword().equals(password)) {
                                    System.out.println("Incorrect password. Please try again.");
                                    break;
                                }

                                u = officer;
                                users.add(u);
                                break;
                            }
                            break;
                        default:  // Applicant
                            u = new Applicant(userID, name, age, maritalStatus, password);
                            users.add(u);
                    }

                    System.out.println("Login succeeded.");
                    System.out.printf("Welcome back, %s %s%n", u.getRole(), u.getName());
                    return u;
                }
                if (foundAny) break;  // we saw the NRIC but bad password, so stop checking lower roles
            }

            if (!foundAny) {
                System.out.println("Username not found. Please try again.");
            }
        }
    }
}
