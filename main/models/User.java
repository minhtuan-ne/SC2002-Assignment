package main.models;

import java.io.IOException;
import java.util.regex.Pattern;
import main.util.FileManager;

/**
 * Abstract class representing a generic user in the system.
 * Stores user identity, age, marital status, and authentication logic.
 */
public abstract class User {
    protected String nric;
    protected String name;
    protected int age;
    protected String maritalStatus;
    protected String password;
    FileManager fileManager = new FileManager();

    /**
     * Constructs a User.
     *
     * @param nric          NRIC in format S/TxxxxxxxX
     * @param name          user's name
     * @param age           user's age
     * @param maritalStatus \"Single\" or \"Married\"
     * @param password      login password
     */
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

    /**
     * Returns this user’s NRIC.
     *
     * @return the NRIC, in the format S/TxxxxxxxX
     */
    public String getNRIC() {
        return nric;
    }

    /**
     * Returns this user’s full name.
     *
     * @return the user’s name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns this user’s age.
     *
     * @return the user’s age in years
     */
    public int getAge() {
        return age;
    }

    /**
     * Returns this user’s marital status.
     *
     * @return either "Single" or "Married"
     */
    public String getMaritalStatus() {
        return maritalStatus;
    }

    /**
     * Returns this user’s password.
     * <p>
     * <em>Note:</em> exposing raw passwords is not recommended in production.
     *
     * @return the current password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Checks whether the provided password matches this user’s password.
     *
     * @param password the password to verify
     * @return true if it matches; false otherwise
     */
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }


    /**
     * Changes the password if the old one matches.
     *
     * @param oldPassword current password
     * @param newPassword new password
     * @return true if changed successfully
     */
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

    /**
     * Validates NRIC format (S/T followed by 7 digits and 1 letter).
     *
     * @param nric input string
     * @return true if format is valid
     */
    public static boolean isValidNRIC(String nric) {
        return Pattern.matches("^[ST]\\d{7}[A-Z]$", nric.toUpperCase());
    }

    /**
     * Returns the user role (to be implemented by subclass).
     *
     * @return role string
     */
    public abstract String getRole();

    @Override
    public String toString() {
        return String.format("Name: %s | NRIC: %s | Age: %d | Marital Status: %s | Role: %s",
                             name, nric, age, maritalStatus, getRole());
    }
}
