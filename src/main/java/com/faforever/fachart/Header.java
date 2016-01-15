/**
 * @author Aaron Elligsen
 */
package com.faforever.fachart;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;


public class Header {
    /*
     The program
    */
    public final static int ONEBYTE = 1; //8 bits
    public final static int TWOBYTES = 2; //16 bits
    public final static int THREEBYTES = 3; //24 bits
    public final static int FOURBYTES = 4; //32 bits
    static int index = 0;

    /**
     * Reads directly from replay file when a string is expected and returns
     * all characters until null character is found as a string.
     *
     * @param areplay The replay file is passed as a FileInputStream
     * @return String
     * @throws IOException
     */
    public static String returnNextString(FileInputStream areplay) throws IOException {
        byte[] inputWord = new byte[ONEBYTE];
        areplay.read(inputWord);
        String current_option = "";
        while (inputWord[0] != 0) {
            current_option += (char) inputWord[0];
            //System.out.println((char)inputWord[0]);
            areplay.read(inputWord);
        }
        return current_option;
    }

    /**
     * Read characters from an array of 'byte's until null character is reached
     *
     * @param inputString Passed an array of bytes which is a preloaded section of
     * replay data.
     * @return String of character data from replay section
     * @throws IOException
     */
    public static String returnNextString(byte[] inputString) throws IOException {
        int index = 0;
        String current_string = "";
        while (inputString[index] != 0) {
            current_string += (char) inputString[index];
            //System.out.println((char)inputWord[0]);
            //areplay.read(inputWord);
            index++;
        }
        return current_string;
    }

    /**
     * parseLuaTable is a short recursive descent parser for serialized LUA tables
     * which are similar to things like Hashmaps (Java), or other data structures with
     * key value pairs. It rebuilds and returns the key value pair relationship as a Hashmap..
     *
     * @param input Passed an array of bytes which is a preloaded section of
     * LUA tables from the replay.
     * @return Hashmap containing rebuilt key-value pair data
     * @throws IOException
     */
    static public Object parseLuaTable(byte[] input/*, Integer index, EScrLuaType inState*/) {

        Object result = null;

        int type = (int) input[index];
        index++;
        //System.out.println(index);
        switch (type) {
            case 0: //LUA_TYPE_NUMBER
                Long number = new Long(ReplayReader.unsignedInt(input, index, 4));
                index += FOURBYTES;
                result = number;
                //System.out.println("number");
                break;
            case 1: //LUA_TYPE_STRING
                String current_string = "";
                while (input[index] != 0) {
                    current_string += (char) input[index];
                    index++;
                }
                index++;
                result = current_string;
                //System.out.println("string");
                break;
            case 2: //LUA_TYPE_NIL
                index++;
                //System.out.println("nill");
                break;
            case 3: //LUA_TYPE_BOOL
                Boolean value;
                if (input[index] == 0) {
                    value = false;
                } else {
                    value = true;
                }
                result = value;
                index++;
                //System.out.println("Boolean");
                break;
            case 4: //LUA_TYPE_TABLE_BEGIN
                Hashtable tableLevel = new Hashtable();
                //System.out.println("table_begin");
                while (input[index] != 5) {
                    Object key = parseLuaTable(input);
                    Object v = parseLuaTable(input);
                    tableLevel.put(key, v);
                }
                index++;
                result = tableLevel;
                break;

            case 5: //LUA_TYPE_TABLE_END
                //System.out.println("Table_end");
                break;
            case 0xff: //LUA_TYPE_MAX
                //System.out.println("MAX");
                break;
        }
        return result;
    }

    /**
     * Reads the replay game patch version and skips past formating characters
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return String containing the game patch version
     * @throws IOException
     */
    static String setReplayPatchFileId(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[ONEBYTE];
        String replayFilePatchId = Header.returnNextString(thereplay);
        thereplay.skip(3);
        return replayFilePatchId;
    }

    /**
     * Reads the replay version id and skips past formating characters.
     * Specifically, this is the version of the replay file format
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return String containing the replay version
     * @throws IOException
     */
    static String setReplayVersionId(FileInputStream thereplay) throws IOException {
        String replayVersionId = Header.returnNextString(thereplay);
        thereplay.skip(FOURBYTES);
        return replayVersionId;
    }

    /**
     * Part of the replay data includes the game mods used in the replay and
     * before it lists the mod data it declares the size of that section
     * *INCLUDING* the bytes used to declare the size.
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return Long containing the size of the game mod serialized table
     * @throws IOException
     */
    static long setGameModsSize(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[FOURBYTES];
        thereplay.read(inputWord);
        long gameModsSize = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
        return gameModsSize;
    }

    /**
     * Reads and rebuilds the Game mod table describing the mods used in the game.
     * Returns the table as a Hashtable with the proper relationships.
     *
     * @param long Takes the size of the Game Mod section as set by setGameModsSize
     * @param thereplay Takes the replay as a FileInputStream
     * @return Hashtable containing Game Mod data
     * @throws IOException
     */
    static Hashtable setGameMods(long size, FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[(int) size];
        thereplay.read(inputWord);
        Hashtable gameMods = (Hashtable) Header.parseLuaTable(inputWord);
        Header.index = 0;
        return gameMods;
    }

    /**
     * Part of the replay data includes the match settings used in the replay and
     * before it lists the data it declares the size of that section
     * *INCLUDING* the bytes used to declare the size.
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return Long containing the size of the match setting serialized table
     * @throws IOException
     */
    static long setLuaScenarioInfoSize(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[FOURBYTES];
        thereplay.read(inputWord);
        long LSIS = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
        return LSIS;
    }

    /**
     * Reads and rebuilds the LUA Scenario info table describing the settings
     * used for the match.
     * Returns the table as a Hashtable with the proper relationships.
     *
     * @param long Takes the size of the LuaScenarioInfo section as set by setLuaScenarioInfo
     * @param thereplay Takes the replay as a FileInputStream
     * @return Hashtable containing Game Mod data
     * @throws IOException
     */
    static Hashtable setLuaScenarioInfo(long size, FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[(int) size];
        thereplay.read(inputWord);
        Hashtable LuaScenarioInfo = (Hashtable) Header.parseLuaTable(inputWord);
        Header.index = 0;
        return LuaScenarioInfo;
    }

    /**
     * Sources are the basic descriptors of the players involved in the match
     * The replay format has a place where it describes the number of Source matching
     * pairs to follow
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return The number of Sources involved in the replay
     * @throws IOException
     */
    static long setNumSources(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[ONEBYTE];
        thereplay.read(inputWord);
        long numSources = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
        return numSources;
    }

    /**
     * The Command Sources or Sources are listed in a simple serialized multidimensional
     * array. This function reads through and stores them in a 2D String array.
     * In particular the Source arrays contain the Player command id and Player name.
     *
     * @param numSources the number of Sources as set by setNumSources
     * @param thereplay Takes the replay as a FileInputStream
     * @return 2D String Array with Source command id and player name
     * @throws IOException
     */
    static String[][] setCommandSource(long numSources, FileInputStream thereplay) throws IOException {
        String[][] players = new String[(int) numSources][2];
        byte[] inputWord = new byte[FOURBYTES];

        for (int i = 0; i < numSources; i++) {
            players[i][0] = Header.returnNextString(thereplay);
            thereplay.read(inputWord);
            players[i][1] = String.valueOf(ReplayReader.unsignedInt(inputWord, 0, FOURBYTES));
        }
        return players;
    }

    /**
     * The replay stores info related to allowing cheats in two places.
     * One is in the LuaScenarioInfo as a key-value pair and as a true/false byte.
     * This reads that true/false byte.
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return 2D String Array with Source command id and player name
     * @throws IOException
     */
    static long setCheatsEnabled(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[ONEBYTE];
        thereplay.read(inputWord);
        long cheats = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
        return cheats;
    }

    /**
     * Armies are serialized LUA tables containing a variety of player data
     * like race, starting position, color, and so on. This function reads and
     * returns the number of armies.This includes AI players, nuetral, and hostile
     * bystanders.
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return long containing the number of armies
     * @throws IOException
     */
    static long setNumArmies(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[ONEBYTE];
        thereplay.read(inputWord);
        long NumArmies = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
        return NumArmies;
    }

    /**
     * Armies are serialized LUA tables containing a variety of player data
     * like race, starting position, color, and so on. This function reads and
     * returns the number of armies.This includes AI players, nuetral, and hostile
     * bystanders.
     *
     * @param numArmies The number of Armies set by setNumArmies
     * @param thereplay Takes the replay as a FileInputStream
     * @return long containing the number of armies
     * @throws IOException
     */
    static Hashtable[] setArmies(long numArmies, FileInputStream thereplay) throws IOException {
        Hashtable[] player = new Hashtable[(int) numArmies];
        byte[] inputWord;
        for (int i = 0; i < numArmies; i++) {
            inputWord = new byte[FOURBYTES];
            thereplay.read(inputWord);
            long playerDataSize = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
            inputWord = new byte[(int) playerDataSize];
            thereplay.read(inputWord);
            index = 0;
            player[i] = (Hashtable) Header.parseLuaTable(inputWord);
            Header.index = 0;
            inputWord = new byte[ONEBYTE];
            thereplay.read(inputWord);
            long playerCommandSource = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
            player[i].put("CommandSource", playerCommandSource);
            if (playerCommandSource != 0xff)
                thereplay.read(inputWord);//skips FF

        }
        return player;
    }

    /**
     * At the end of the header file just before the command stream begins
     * is a random integer which is the seed for the random number generator
     * used for the games engine.
     *
     * @param thereplay Takes the replay as a FileInputStream
     * @return long int containing the random seed
     * @throws IOException
     */
    static long setRandomSeed(FileInputStream thereplay) throws IOException {
        byte[] inputWord = new byte[FOURBYTES];
        thereplay.read(inputWord);
        long randomSeed = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
        return randomSeed;
    }

}

/*enum EScrLuaType 
{ 
    LUA_TYPE_NUMBER, 
    LUA_TYPE_STRING, 
    LUA_TYPE_NIL, 
    LUA_TYPE_BOOL, 
    LUA_TYPE_TABLE_BEGIN, 
    LUA_TYPE_TABLE_END, 
    LUA_TYPE_MAX//(0xff) 
} */
