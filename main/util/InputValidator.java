package main.util;

import java.util.regex.Pattern;

/**
 * Provides utility functions to validate input formats (NRIC, password, age).
 */
public class InputValidator {
    // Condition for a valid NRIC (starts with S or T, followed by 7-digit number
    // and ends with another letter)
    private static final Pattern NRIC_PATTERN = Pattern.compile("^[ST]\\d{7}[A-Z]$");

    /**
     * Checks if the NRIC format is valid.
     *
     * @param nric the NRIC string to validate
     * @return true if valid format
     */
    public static boolean isValidNRIC(String nric) {
        return nric != null && NRIC_PATTERN.matcher(nric).matches();
    }

     /**
     * Checks if a password string is non-empty and not null.
     *
     * @param password the password string
     * @return true if valid
     */
    public static boolean isValidPassword(String password) {
        return password != null && !password.trim().isEmpty();
    }

    /**
     * Checks if an age is positive.
     *
     * @param age age to check
     * @return true if age > 0
     */
    public static boolean isValidAge(int age) {
        return age > 0;
    }

}
