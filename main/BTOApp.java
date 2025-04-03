package main;

import main.util.Authenticator;


public class BTOApp {
    public static void main(String[] args) {
        // Log in and get user type (Applicant, Officer, or Manager)
       Authenticator auth = new Authenticator();
       String userType = auth.LogIn();
        
       // Function for each usertype
       switch (userType) {
        case "Applicant":
            // Functions for Applicant
            
            break;

        case "Officer":
            // Functions for Applicant
            
            break;

        case "Manager":
            // Functions for Applicant
            
            break;

        default:
            break;
       }
    }
}
