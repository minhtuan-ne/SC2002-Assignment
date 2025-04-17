package main.models;

import java.util.Collection;
import java.util.HashMap;

import main.models.User;

public class UserRepository {
    private static HashMap<String, User> userDatabase = new HashMap<>();
    
    public static void addUser(User user) {
        userDatabase.put(user.getNRIC(), user);
    }
    
    public static User getUser(String nric) {
        return userDatabase.get(nric);
    }
    
    public static Collection<User> getAllUsers() {
        return userDatabase.values();
    }
    
    public static void removeUser(User user) {
        userDatabase.remove(user.getNRIC());
    }
    
    public static boolean containsUser(String nric) {
        return userDatabase.containsKey(nric);
    }
    
    public static void setUserDatabase(HashMap<String, User> database) {
        userDatabase = database;
    }
    
    public static HashMap<String, User> getUserDatabase() {
        return userDatabase;
    }
}