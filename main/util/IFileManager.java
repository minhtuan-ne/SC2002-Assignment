package main.util;

import java.util.List;
import java.util.Map;

public interface IFileManager {
    Map<String, List<List<String>>> getDatabyRole();
    List<List<String>> readFile(String fileName);
}
