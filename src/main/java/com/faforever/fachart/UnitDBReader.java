package com.faforever.fachart;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class UnitDBReader {
    public Hashtable<String,String> readUnitDb() {
        final Hashtable<String, String> unitTable = new Hashtable<>();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/unitDB.txt")));
            bufferedReader.lines().forEach(line -> {
                String[] unitInfo = line.split("\\s");
                unitTable.put(unitInfo[0], unitInfo[1]);
            });
        return unitTable;
    }
}
