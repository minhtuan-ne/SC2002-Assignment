package main.models;

import java.io.IOException;
import java.util.regex.Pattern;
import main.util.FileManager;

public abstract class User {
    protected String nric;           // NRIC: S/T + 7 digits + 1 letter
    protected String name;
    protected int age;
    protected String maritalStatus;  // "Single" or "Married"
    protected String password;
    FileManager fileManager       = new FileManager();
    
    public User(String nric, String name, int age, String maritalStatus, String password) {
        if (!isValidNRIC(nric)) {
            throw new IllegalArgumentException("Invalid NRIC format.");
        }
        this.nric = nric.toUpperCase();
        this.name = name;
        this.age = age;
        this.maritalStatus = maritalStatus;
        this.password = password;
    }    

    public String getNRIC() { return nric; }

    public String getName() { return name; }

    public int getAge() { return age; }

    public String getMaritalStatus() { return maritalStatus; }

    public String getPassword() { return this.password; }

    public boolean checkPassword(String password) { return this.password.equals(password); }

    public boolean changePassword(String oldPassword, String newPassword) {
        try {
            if (!checkPassword(oldPassword)) {
                throw new IllegalArgumentException("Incorrect current password.");
            }
            this.password = newPassword;    
            fileManager.updatePassword(this.getRole(), this.nric, newPassword);
            System.out.println("Password changed successfully.");
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        } catch (IOException ioe) {
            System.out.println("Failed to save new password: " + ioe.getMessage());
            return false;
        }
    }

    public static boolean isValidNRIC(String nric) { return Pattern.matches("^[ST]\\d{7}[A-Z]$", nric.toUpperCase()); }

    // Abstract method to be implemented by subclasses
    public abstract String getRole();

    @Override
    public String toString() {
        return String.format("Name: %s | NRIC: %s | Age: %d | Marital Status: %s | Role: %s",
                             name, nric, age, maritalStatus, getRole());
    }
}
