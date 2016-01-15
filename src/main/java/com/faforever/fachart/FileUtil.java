package com.faforever.fachart;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class FileUtil {
    public Hashtable<String, String> readUnitDb() {
        final Hashtable<String, String> unitTable = new Hashtable<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/unitDB.txt")));
        bufferedReader.lines().forEach(line -> {
            String[] unitInfo = line.split("\\s");
            unitTable.put(unitInfo[0], unitInfo[1]);
        });
        return unitTable;
    }


    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
