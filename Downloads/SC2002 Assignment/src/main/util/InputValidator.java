package main.util;

import java.util.regex.Pattern;

public class InputValidator {
    // Condition for a valid NRIC (starts with S or T, followed by 7-digit number and ends with another letter)
    private static final Pattern NRIC_PATTERN = Pattern.compile("^[ST]\\d{7}[A-Z]$");

    // Validate NRIC format
    public static boolean isValidNRIC(String nric) {
        return nric != null && NRIC_PATTERN.matcher(nric).matches();
    }

    // Validate password (non-empty)
    public static boolean isValidPassword(String password) {
        return password != null && !password.trim().isEmpty();
    }

    // Validate age (larger than 0)
    public static boolean isValidAge(int age) {
        return age > 0;
    }

    // Validate

}
