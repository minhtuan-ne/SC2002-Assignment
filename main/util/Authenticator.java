package main.util;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Authenticator {
        public String LogIn() {
        // Get the data
        FileManager fm = new FileManager();
        HashMap<String, List<List<String>>> UserData = fm.getDatabyRole();

        // Welcome
        Scanner sc = new Scanner(System.in);

        System.out.println("Welcome back. Please log in");

        while (true) {
            System.out.print("User ID: ");
            String UserID = sc.nextLine();
            System.out.print("Password: ");
            String password = sc.nextLine();

            boolean userFound = false;

            // Validate username and password
            for (String i : UserData.keySet()) {
                if (i.equals("Project")) {
                    continue;
                } else {
                    List<List<String>> UserList = UserData.get(i);
                    for (int j = 0; j < UserList.size(); j++) {
                        // Validate username
                        if (InputValidator.isValidNRIC(UserList.get(j).get(1))) {
                            System.err.println("Incorrect format of NRIC.");
                            continue;
                        }
                        else if (UserList.get(j).get(1).equals(UserID)) {
                            userFound = true;
                            // Validate password
                            if (UserList.get(j).get(4).equals(password)) {
                                System.out.println("Log in succeeded.");
                                System.out.println("Welcome back, " + UserList.get(j).get(0));
                                sc.close();
                                return i;
                            } else {
                                System.out.println("Incorrect password.");
                            }
                        }
                    }
                }
                if (!userFound) {
                    System.out.println("Username not found. Try again.");
                }
            }
        }
    }
}
