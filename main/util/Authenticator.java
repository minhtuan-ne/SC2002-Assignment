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
            boolean passwordFound = false;

            // Validate NRIC Format
            if (!InputValidator.isValidNRIC(UserID)) {
                System.out.println("Incorrect format of NRIC. Please try again");
                continue;
            }

            // Validate username and password
            for (String i : UserData.keySet()) {
                if (i.equals("Project")) {
                    continue;
                } 
                else {
                    List<List<String>> UserList = UserData.get(i);
                    for (int j = 0; j < UserList.size(); j++) {
                        // Validate username        
                        if (UserList.get(j).get(1).equals(UserID)) {
                            userFound = true;
                            // Validate password
                            if (UserList.get(j).get(4).equals(password)) {
                                System.out.println("Log in succeeded.");
                                System.out.println("Welcome back, " + i + " " + UserList.get(j).get(0));
                                passwordFound = true;
                                sc.close();
                                return i;
                            }
                        }
                    }
                }
            }
           
            if (!userFound) {
                System.out.println("Username not found. Please try again.");
            }
            else {
                if (!passwordFound) {
                    System.out.println("Incorrect password. Please try again.");
                }
            }
        }
    }
}
