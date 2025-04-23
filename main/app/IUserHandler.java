package main.app;

import java.time.format.DateTimeFormatter;
import java.util.*;
import main.models.*;

public interface IUserHandler {
    DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    void run(User user, Scanner sc);
}