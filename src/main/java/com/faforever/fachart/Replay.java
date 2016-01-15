/**
 * @author Aaron Elligsen
 */
package com.faforever.fachart;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The Replay class holds pretty much all the information that is useful when 
 * describing a replay. 
 */
public class Replay {
    public final int ONEBYTE = 1; //8 bits
    public final int TWOBYTES = 2; //16 bits
    public final int THREEBYTES = 3; //24 bits
    public final int FOURBYTES = 4; //32 bits
    public String ReplayPatchFileId;
    public String ReplayVersionId;
    public long GameModsSize;
    public Hashtable<Object, Object> GameMods;
    public long LuaScenarioInfoSize;
    public Hashtable<Object, Object> LuaScenarioInfo;
    public long NumSources;
    public String[][] CommandSource;
    public long CheatsEnabled;
    public long NumArmies;
    public ArrayList<Hashtable<Object, Object>> Armies;
    public long RandomSeed;
    public long GameTime;
    public float[] PlayerGameTimes;
    public ArrayList<Vector<Point>> APMS;
    public ArrayList<Vector<Point>> MicroAPM;
    //public Hashtable EngineerBuildOrder;
    public Link buildorder;
    public int[][] ActionsList;
    public int[] ActionsTotal;
    public long[] PlayerLastTurn;
    private int index;
    private int array_index;
    private int csl;
    private int eof;
    private Hashtable<String, String> unitTable;

    public Replay(Hashtable<String, String> uTable) {
        unitTable = uTable;

        ReplayPatchFileId = null;
        ReplayVersionId = null;

        GameModsSize = 0;
        GameMods = null;
        LuaScenarioInfoSize = 0;
        LuaScenarioInfo = null;
        NumSources = 0;
        CommandSource = null;

        CheatsEnabled = 0;
        NumArmies = 0;
        Armies = null;

        RandomSeed = 0;
        GameTime = 0;
        APMS = null;
        MicroAPM = null;
        buildorder = null;
        ActionsList = null;
        ActionsTotal = null;
        PlayerGameTimes = null;
        index = 0;
        array_index = 0;

    }


    /**
     * Reads directly from replay file when a string is expected and returns
     * all characters until null character is found as a string.
     *
     * @param areplay The replay file is passed as a byte[]
     * @return String
     *
     */
    private String returnNextString(byte[] areplay) {

        String current_option = "";
        int i = 0;
        while (areplay[index + i] != 0) {
            current_option += (char) areplay[index + i];
            i++;
        }
        index += i + 1;
        return current_option;
    }

    /**
     * The header is one linear portion of the file. There is only one way to
     * read it. Therefore instead of offering the various functions individually
     * which won't work unless in the right location I have made them private
     * functions of the class and created this public class to handle the
     * header section.
     *
     * IMPORTANT: this function MUST be used before any of the other public Replay functions
     * as it sets csl and eof for the use by the other functions.
     *
     * @param thereplay Takes the replay as byte array
     * @param file_size Takes the size of the file which can represent the length of the array
     */

    public void analyzeHeader(byte[] thereplay, int file_size) {
        this.setReplayPatchFileId(thereplay);
        this.setReplayVersionId(thereplay);
        this.setGameModsSize(thereplay);
        this.setGameMods(this.GameModsSize, thereplay);
        this.setLuaScenarioInfoSize(thereplay);
        this.setLuaScenarioInfo(this.LuaScenarioInfoSize, thereplay);
        this.setNumSources(thereplay);
        this.setCommandSource(this.NumSources, thereplay);
        this.setCheatsEnabled(thereplay);
        this.setNumArmies(thereplay);
        this.setArmies(this.NumArmies, thereplay);
        this.setRandomSeed(thereplay);

        this.eof = file_size;
        csl = index;
    }

    /**
     * parseLuaTable is a short recursive descent parser for serialized LUA tables
     * which are similar to things like Hashmaps (Java), or other data structures with
     * key value pairs. It rebuilds and returns the key value pair relationship as a Hashmap.
     * LUA is a weakly typed language.
     *
     * @param input Passed an array of bytes which is a preloaded section of
     * LUA tables from the replay.
     * @return Hashmap containing rebuilt key-value pair data
     *
     */
    private Object parseLuaTable(byte[] input) {

        Object result = null;

        int type = (int) input[array_index];
        array_index++;
        switch (type) {
            case 0: //LUA_TYPE_NUMBER
                Long number = new Long(ReplayReader.unsignedInt(input, array_index, 4));
                array_index += FOURBYTES;
                result = number;
                break;
            case 1: //LUA_TYPE_STRING
                String current_string = "";
                while (input[array_index] != 0) {
                    current_string += (char) input[array_index];
                    array_index++;

                }
                array_index++;

                result = current_string;
                break;
            case 2: //LUA_TYPE_NIL
                array_index++;

                break;
            case 3: //LUA_TYPE_BOOL
                Boolean value;
                if (input[array_index] == 0) {
                    value = false;
                } else {
                    value = true;
                }
                result = value;
                array_index++;

                break;
            case 4: //LUA_TYPE_TABLE_BEGIN
                Hashtable<Object, Object> tableLevel = new Hashtable<Object, Object>();
                while (input[array_index] != 5) {
                    Object key = parseLuaTable(input);
                    Object v = parseLuaTable(input);
                    tableLevel.put(key, v);
                }
                array_index++;

                result = tableLevel;
                break;

            case 5: //LUA_TYPE_TABLE_END
                break;
            case 0xff: //LUA_TYPE_MAX
                break;
        }
        return result;
    }

    /**
     * Reads the replay game patch version and skips past formating characters
     *
     * @param thereplay Takes the replay as a byte[]
     * @return String containing the game patch version
     *
     */
    private void setReplayPatchFileId(byte[] thereplay) {
        String replayFilePatchId = returnNextString(thereplay);
        index += THREEBYTES;
        ReplayPatchFileId = replayFilePatchId;
    }

    /**
     * Reads the replay version id and skips past formating characters.
     * Specifically, this is the version of the replay file format
     *
     * @param thereplay Takes the replay as a byte[]
     * @return String containing the replay version
     *
     */
    private void setReplayVersionId(byte[] thereplay) {
        String replayVersionId = returnNextString(thereplay);
        index += FOURBYTES;
        ReplayVersionId = replayVersionId;
    }

    /**
     * Part of the replay data includes the game mods used in the replay and
     * before it lists the mod data it declares the size of that section
     * *INCLUDING* the bytes used to declare the size.
     *
     * @param thereplay Takes the replay as a byte[]
     * @return Long containing the size of the game mod serialized table
     *
     */
    private void setGameModsSize(byte[] thereplay) {
        byte[] inputWord = new byte[FOURBYTES];
        for (int i = 0; i < FOURBYTES; i++)
            inputWord[i] = thereplay[index + i];
        index += FOURBYTES;
        long gameModsSize = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
        GameModsSize = gameModsSize;
    }

    /**
     * Reads and rebuilds the Game mod table describing the mods used in the game.
     * Returns the table as a Hashtable with the proper relationships.
     *
     * @param long Takes the size of the Game Mod section as set by setGameModsSize
     * @param thereplay Takes the replay as a byte[]
     * @return Hashtable containing Game Mod data
     *
     */
    private void setGameMods(long size, byte[] thereplay) {
        byte[] inputWord = new byte[(int) size];
        for (int i = 0; i < size; i++)
            inputWord[i] = thereplay[index + i];
        index += size;
        Hashtable<Object, Object> gameMods = (Hashtable<Object, Object>) parseLuaTable(inputWord);
        GameMods = gameMods;
    }

    /**
     * Part of the replay data includes the match settings used in the replay and
     * before it lists the data it declares the size of that section
     * *INCLUDING* the bytes used to declare the size.
     *
     * @param thereplay Takes the replay as a byte[]
     * @return Long containing the size of the match setting serialized table
     *
     */
    private void setLuaScenarioInfoSize(byte[] thereplay) {
        byte[] inputWord = new byte[FOURBYTES];
        for (int i = 0; i < FOURBYTES; i++)
            inputWord[i] = thereplay[index + i];
        index += FOURBYTES;
        long LSIS = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
        LuaScenarioInfoSize = LSIS;
    }

    /**
     * Reads and rebuilds the LUA Scenario info table describing the settings
     * used for the match.
     * Returns the table as a Hashtable with the proper relationships.
     *
     * @param long Takes the size of the LuaScenarioInfo section as set by setLuaScenarioInfo
     * @param thereplay Takes the replay as a byte[]
     * @return Hashtable containing Game Mod data
     *
     */
    private void setLuaScenarioInfo(long size, byte[] thereplay) {
        byte[] inputWord = new byte[(int) size];
        for (int i = 0; i < size; i++)
            inputWord[i] = thereplay[index + i];
        index += size;
        Hashtable<Object, Object> LSI = (Hashtable<Object, Object>) parseLuaTable(inputWord);
        array_index = 0;
        this.LuaScenarioInfo = LSI;
    }

    /**
     * Sources are the basic descriptors of the players involved in the match
     * The replay format has a place where it describes the number of Source matching
     * pairs to follow
     *
     * @param thereplay Takes the replay as a byte[]
     * @return The number of Sources involved in the replay
     *
     */
    private void setNumSources(byte[] thereplay) {
        byte[] inputWord = new byte[ONEBYTE];
        inputWord[0] = thereplay[index];
        index += ONEBYTE;
        long numSources = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
        NumSources = numSources;
    }

    /**
     * The Command Sources or Sources are listed in a simple serialized multidimensional
     * array. This function reads through and stores them in a 2D String array.
     * In particular the Source arrays contain the Player command id and Player name.
     *
     * @param numSources the number of Sources as set by setNumSources
     * @param thereplay Takes the replay as a byte[]
     * @return 2D String Array with Source command id and player name
     *
     */
    private void setCommandSource(long numSources, byte[] thereplay) {
        String[][] players = new String[(int) numSources][2];
        byte[] inputWord = new byte[FOURBYTES];

        for (int i = 0; i < numSources; i++) {
            players[i][0] = returnNextString(thereplay);
            for (int j = 0; j < FOURBYTES; j++)
                inputWord[j] = thereplay[index + j];
            index += FOURBYTES;
            players[i][1] = String.valueOf(ReplayReader.unsignedInt(inputWord, 0, FOURBYTES));
        }
        CommandSource = players;

    }

    /**
     * The replay stores info related to allowing cheats in two places.
     * One is in the LuaScenarioInfo as a key-value pair and as a true/false byte.
     * This reads that true/false byte.
     *
     * @param thereplay Takes the replay as a byte[]
     * @return 2D String Array with Source command id and player name
     *
     */
    private void setCheatsEnabled(byte[] thereplay) {
        byte[] inputWord = new byte[ONEBYTE];
        inputWord[0] = thereplay[index];
        index += ONEBYTE;
        long cheats = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
        CheatsEnabled = cheats;
    }

    /**
     * Armies are serialized LUA tables containing a variety of player data
     * like race, starting position, color, and so on. This function reads and
     * returns the number of armies.This includes AI players, nuetral, and hostile
     * bystanders.
     *
     * @param thereplay Takes the replay as a byte[]
     * @return long containing the number of armies
     *
     */
    private void setNumArmies(byte[] thereplay) {
        byte[] inputWord = new byte[ONEBYTE];
        inputWord[0] = thereplay[index];
        index += ONEBYTE;
        long numArmies = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
        this.NumArmies = numArmies;
    }

    /**
     * Armies are serialized LUA tables containing a variety of player data
     * like race, starting position, color, and so on. This function reads and
     * returns the number of armies.This includes AI players, nuetral, and hostile
     * bystanders.
     *
     * @param numArmies The number of Armies set by setNumArmies
     * @param thereplay Takes the replay as a byte[]
     * @return long containing the number of armies
     *
     */
    private void setArmies(long numArmies, byte[] thereplay) {
        ArrayList<Hashtable<Object, Object>> player = new ArrayList<Hashtable<Object, Object>>();
        for (int i = 0; i < (int) numArmies; i++) {
            player.add(new Hashtable<Object, Object>());
        }
        byte[] inputWord;
        for (int i = 0; i < numArmies; i++) {
            inputWord = new byte[FOURBYTES];
            for (int j = 0; j < FOURBYTES; j++)
                inputWord[j] = thereplay[index + j];
            index += FOURBYTES;
            long playerDataSize = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
            inputWord = new byte[(int) playerDataSize];
            for (int j = 0; j < playerDataSize; j++)
                inputWord[j] = thereplay[index + j];
            index += (int) playerDataSize;
            array_index = 0;
            player.set(i, (Hashtable<Object, Object>) parseLuaTable(inputWord));
            array_index = 0;
            inputWord = new byte[ONEBYTE];
            inputWord[0] = thereplay[index];
            index += ONEBYTE;
            long playerCommandSource = ReplayReader.unsignedInt(inputWord, 0, ONEBYTE);
            player.get(i).put("CommandSource", playerCommandSource);
            if (playerCommandSource != 0xff)
                index += ONEBYTE;

        }
        Armies = player;
    }

    /**
     * At the end of the header file just before the command stream begins
     * is a random integer which is the seed for the random number generator
     * used for the games engine.
     *
     * @param thereplay Takes the replay as a byte[]
     * @return long int containing the random seed
     *
     */
    private void setRandomSeed(byte[] thereplay) {
        byte[] inputWord = new byte[FOURBYTES];
        for (int i = 0; i < FOURBYTES; i++)
            inputWord[i] = thereplay[index + i];
        index += FOURBYTES;
        long randomSeed = ReplayReader.unsignedInt(inputWord, 0, FOURBYTES);
        RandomSeed = randomSeed;
    }


    /**
     * Unfortunately, the running game time is not stored in the header. It
     * isn't stored as a single value anywhere. The command stream documents
     * time with ticks and every tick is 1/10 of a second. It records them all
     * game long you need to count all of them to determine the game time.
     * This means going through the command stream a second time to determine it.
     *
     * @param thereplay Takes the replay as a byte[]
     * @return The number of ticks counted. Apply a conversion of ticks/10/60 to get game time in minutes
     *
     */
    public void setGameTime(byte[] thereplay) {
        byte[] inputWord = new byte[1];
        if (csl != 0) {
            long tick = 0;
            while (index != eof) {
                inputWord[0] = thereplay[index];
                int message_op = (int) ReplayReader.unsignedInt(inputWord, 0, 1);
                index += ONEBYTE;
                int message_length = 0;
                inputWord = new byte[2];
                inputWord[0] = thereplay[index];
                inputWord[1] = thereplay[index + 1];
                index += TWOBYTES;
                message_length = (int) ReplayReader.unsignedInt(inputWord, 0, 2);
                if (message_op == 0) {
                    tick++;
                }
                index += message_length - 3; //skip all the data we don't need to look at
                //we're just looking for the time in this function.
                inputWord = new byte[1];
            }
            this.GameTime = tick;
            index = csl;
            this.setPlayerLastTurn(thereplay);
            this.setPlayerGameTimes();
        } else {
            this.GameTime = 0;
        }
    }

    /**
     * After other variables have been set by the function setGameTime and
     * setPlayerLastTurn this function sets the PlayerGameTimes which essentialy
     * is the measure in minutes before each player died or the game ended.
     */
    private void setPlayerGameTimes() {
        float[] gametime = new float[(int) NumSources];
        for (int i = 0; i < NumSources; i++) {
            if (PlayerLastTurn[i] > 0) {
                int tickseconds = (int) PlayerLastTurn[i] / 10;
                gametime[i] = tickseconds / 60;
            }
        }
        this.PlayerGameTimes = gametime;

    }

    /**
     * This sets the data structure APMS and the int array ActionTotal.
     * APMS is used for creating both the CPM graph and the Macro/Micro graph
     * ActionTotal is used primarily for providing the CPM average.
     *
     * @param thereplay Takes the replay as a byte array
     */

    public void setAPMS_actionTotal(byte[] thereplay) {

        int currentPlayer = 0;
        long currentAction = 0;

        int playerturn = 0;

        int[] actions = new int[(int) NumSources];
        int[] difActions = new int[(int) NumSources];

        ArrayList<Vector<Integer>> actions2 = new ArrayList<Vector<Integer>>();
        for (int i = 0; i < (int) NumSources; i++) {
            actions2.add(new Vector<Integer>());
        }

        int[] localTotal = new int[(int) NumSources];
        ArrayList<Vector<Point>> APM = new ArrayList<Vector<Point>>();
        for (int i = 0; i < (int) NumSources; i++) {
            APM.add(new Vector<Point>());
        }
        long lastTick = 0;
        long tick = 0;

        int cpmTimeIntervalSeconds = 60;
        int cpmTimeIntervalTicks = cpmTimeIntervalSeconds * 10;
        int cpmMultiplier = 60 / cpmTimeIntervalSeconds;

        int pointSampleIntervalTicks = 30;

        byte[] inputWord = new byte[1];
        if (csl != 0) {
            while (index != eof) {
                if (tick >= (600 - (cpmTimeIntervalTicks / 2)) && lastTick != tick) //don't begin calculating APM until 1 minute or 600 ticks
                {
                    for (int i = 0; i < NumSources; i++) {
                        int deltaActions = actions[i] - difActions[i];
                        difActions[i] = actions[i];
                        actions2.get(i).add(new Integer(deltaActions));
                        localTotal[i] += deltaActions;
                        if (tick > (600 + (cpmTimeIntervalTicks / 2))) {
                            localTotal[i] -= (Integer) actions2.get(i).remove(0);
                            if (tick % pointSampleIntervalTicks == 0) {
                                int floatError = (cpmTimeIntervalTicks / 2);
                                Point apm = new Point(tick - floatError, localTotal[i] * cpmMultiplier);
                                APM.get(i).add(apm);
                            }
                        }
                    }
                    lastTick = tick;
                }

                inputWord[0] = thereplay[index];
                int message_op = (int) ReplayReader.unsignedInt(inputWord, 0, 1);
                index += ONEBYTE;
                int message_length = 0;
                inputWord = new byte[2];
                inputWord[0] = thereplay[index];
                inputWord[1] = thereplay[index + 1];
                index += TWOBYTES;
                message_length = (int) ReplayReader.unsignedInt(inputWord, 0, 2);

                switch (message_op) {
                    case 0: /*theoutput.println("CMDST_Advance");*/
                        tick++;
                        break;
                    case 1: /*theoutput.println("CMDST_SetCommandSource");*/
                        playerturn = (int) thereplay[index];
                        break;
                    case 11: /*theoutput.println("CMDST_ProcessInfoPair "); */
                        if (currentAction != tick || currentPlayer != playerturn) {
                            actions[playerturn]++;
                            currentAction = tick;
                            currentPlayer = playerturn;
                        }
                        break;
                    case 12:
                        if (currentAction != tick || currentPlayer != playerturn) {
                            actions[playerturn]++;

                        }
                        break;
                    case 13: /*theoutput.println("CMDST_IssueFactoryCommand"); */
                        actions[playerturn]++;
                        break;
                    case 19: /*theoutput.println("CMDST_RemoveCommandFromQueue ");*/
                        if (currentAction != tick || currentPlayer != playerturn) {
                            actions[playerturn]++;
                            currentAction = tick;
                            currentPlayer = playerturn;
                        }
                        break;
                    case 22: /*theoutput.println("CMDST_LuaSimCallback"); */
                        if (currentAction != tick || currentPlayer != playerturn) {
                            actions[playerturn]++;
                            currentAction = tick;
                            currentPlayer = playerturn;
                        }
                        break;

                }
                index += message_length - 3;

            }

            this.APMS = APM;
            this.ActionsTotal = actions;
            index = csl;
        } else {
            this.APMS = null;
            this.ActionsTotal = null;
        }
    }

    /**
     * This sets MicroAPM which is a collection used to produce the Micro/Macro
     * graph section.
     *
     * @param thereplay Takes the replay as a byte[]
     */
    public void setMicroAPM(byte[] thereplay) {
        int currentPlayer = 0;
        long currentAction = 0;

        int playerturn = 0;
        ArrayList<Vector<Point>> MAPMS = new ArrayList<Vector<Point>>();
        for (int i = 0; i < (int) NumSources; i++) {
            MAPMS.add(new Vector<Point>());
        }

        int[] microActions = new int[(int) NumSources];
        int[] microDifActions = new int[(int) NumSources];
        ArrayList<Vector<Integer>> actions2micro = new ArrayList<Vector<Integer>>();
        for (int i = 0; i < (int) NumSources; i++) {
            actions2micro.add(new Vector<Integer>());
        }

        int[] localTotalMicro = new int[(int) NumSources];

        long lastTick = 0;
        long tick = 0;

        int cpmTimeIntervalSeconds = 60;
        int cpmTimeIntervalTicks = cpmTimeIntervalSeconds * 10;
        int cpmMultiplier = 60 / cpmTimeIntervalSeconds;

        int pointSampleIntervalTicks = 30;

        byte[] inputWord = new byte[1];
        if (csl != 0) {
            while (index != eof) {
                if (tick >= (600 - (cpmTimeIntervalTicks / 2)) && lastTick != tick) //don't begin calculating APM until 1 minute or 600 ticks
                {
                    for (int i = 0; i < NumSources; i++) {
                        int microDeltaActions = microActions[i] - microDifActions[i];
                        microDifActions[i] = microActions[i];
                        actions2micro.get(i).add(new Integer(microDeltaActions));
                        localTotalMicro[i] += microDeltaActions;
                        if (tick > (600 + (cpmTimeIntervalTicks / 2))) {
                            localTotalMicro[i] -= (Integer) actions2micro.get(i).remove(0);
                            if (tick % pointSampleIntervalTicks == 0) {
                                int floatError = (cpmTimeIntervalTicks / 2);
                                Point mapm = new Point(tick - floatError, localTotalMicro[i] * cpmMultiplier);
                                MAPMS.get(i).add(mapm);
                            }
                        }
                    }
                    lastTick = tick;
                }


                inputWord[0] = thereplay[index];
                int message_op = (int) ReplayReader.unsignedInt(inputWord, 0, 1);
                index += ONEBYTE;
                int message_length = 0;
                inputWord = new byte[2];
                inputWord[0] = thereplay[index];
                inputWord[1] = thereplay[index + 1];
                index += TWOBYTES;
                message_length = (int) ReplayReader.unsignedInt(inputWord, 0, 2);
                int faux_index = 0;
                switch (message_op) {
                    case 0:
                        tick++;
                        break;
                    case 1:
                        playerturn = (int) thereplay[index];
                        break;
                    case 12:
                        int numUnits = (int) ReplayReader.unsignedInt(thereplay, index, 4);
                        faux_index += FOURBYTES;
                        long[] entIds = new long[numUnits];
                        for (int b = 0; b < numUnits; b++) {
                            entIds[b] = ReplayReader.unsignedInt(thereplay, index + faux_index, 4);
                            faux_index += FOURBYTES;
                        }
                        long commandId = ReplayReader.unsignedInt(thereplay, index + faux_index, 4);
                        faux_index += 8; //move past commandID, and a 32 bit -1
                        int commandType = (int) ReplayReader.unsignedInt(thereplay, index + faux_index, 1);

                        if (currentAction != tick || currentPlayer != playerturn) {
                            currentAction = tick;
                            currentPlayer = playerturn;
                            switch (commandType) {
                                //The actions listed here are not micro actions
                                case 5: /*"UNITCOMMAND_BuildSiloTactical "*/
                                    break;
                                case 6: /*"UNITCOMMAND_BuildSiloNuke "*/
                                    break;
                                case 7: /*"UNITCOMMAND_BuildFactory "*/
                                    break;
                                case 8: /*"UNITCOMMAND_BuildMobile "*/
                                    break;
                                case 9: /*"UNITCOMMAND_BuildAssist "*/
                                    break;
                                case 15: /*"UNITCOMMAND_Guard "*/
                                    break;
                                case 16: /*"UNITCOMMAND_Patrol "*/
                                    break;
                                case 19: /*"UNITCOMMAND_Reclaim " */
                                    break;
                                case 20: /*"UNITCOMMAND_Repair " */
                                    break;
                                case 21: /*"UNITCOMMAND_Capture "*/
                                    break;
                                case 27: /*"UNITCOMMAND_Upgrade "*/
                                    break;
                                case 28: /*"UNITCOMMAND_Script "*/
                                    break;
                                case 29: /*"UNITCOMMAND_AssistCommander "*/
                                    break;
                                case 32: /*"UNITCOMMAND_Sacrifice " */
                                    break;
                                case 33: /*"UNITCOMMAND_Pause " */
                                    break;
                                case 38: /*"UNITCOMMAND_SpecialAction "*/
                                    break;
                                default:
                                    microActions[playerturn]++; //Everything else is.
                            }
                        }
                }
                faux_index = 0;
                index += message_length - 3;
                //skip all the data we don't need to look at
                inputWord = new byte[1];
            }
            this.MicroAPM = MAPMS;
            index = csl;
        } else {
            this.MicroAPM = null;
        }

    }

    /**
     * Builder orders are an important topic in RTS games and this can extract
     * them from the replay for study.
     *
     * @param thereplay Takes the replay as a byte array
     */
    public void setBuildorder(byte[] thereplay) {
        ArrayList<Hashtable<Long, String>> engineerBo = new ArrayList<Hashtable<Long, String>>();

        for (int i = 0; i < (int) NumSources; i++) {
            engineerBo.add(new Hashtable<Long, String>());
        }
        Link buildOrder;
        if (csl != 0) {
            if (unitTable != null) {
                buildOrder = new Link(CommandSource[0][0] + "'s build order\n", 0, 0, Long.MAX_VALUE, null);
                for (int i = 1; i < (int) NumSources; i++) {
                    buildOrder.add(new Link(CommandSource[i][0] + "'s build order\n", i, 0, Long.MAX_VALUE, null));
                }
                int BOtime = 3600; //only record build order for the first 360 seconds = 3600 ticks

                int playerturn = 0;
                long tick = 0;

                byte[] inputWord = new byte[1];

                while (index != eof) {
                    inputWord[0] = thereplay[index];
                    int message_op = (int) ReplayReader.unsignedInt(inputWord, 0, 1);
                    index += ONEBYTE;
                    int message_length = 0;
                    inputWord = new byte[2];
                    inputWord[0] = thereplay[index];
                    inputWord[1] = thereplay[index + 1];
                    index += TWOBYTES;
                    message_length = (int) ReplayReader.unsignedInt(inputWord, 0, 2);
                    switch (message_op) {
                        case 0:
                            tick++;
                            break;
                        case 1:
                            playerturn = (int) thereplay[index];
                            break;
                        case 12:
                            int faux_index = 0;
                            int numUnits = (int) ReplayReader.unsignedInt(thereplay, index, 4);
                            faux_index += FOURBYTES;
                            long[] entIds = new long[numUnits];
                            for (int b = 0; b < numUnits; b++) {
                                entIds[b] = ReplayReader.unsignedInt(thereplay, index + faux_index, 4);
                                faux_index += FOURBYTES;
                            }
                            long commandId = ReplayReader.unsignedInt(thereplay, index + faux_index, 4);
                            faux_index += 8; //move past commandID, and a 32 bit -1

                            int commandType = (int) ReplayReader.unsignedInt(thereplay, index + faux_index, 1);
                            faux_index += 5; //move past commandType and a 32 bit -1

                            int STITarget = (int) ReplayReader.unsignedInt(thereplay, index + faux_index, 1);

                            if (commandType == 7 || commandType == 8 || commandType == 27) {
                                if (STITarget == 0) {
                                    faux_index += 6;
                                } else if (STITarget == 2) {
                                    faux_index += 1 + 3 * 4 + 1 + 4;//move past stitarget, 3 floats, a zero,a -1 32bit int

                                }

                                char[] unitBluePrint = new char[7];
                                for (int i = 0; i < 7; i++) {
                                    unitBluePrint[i] = (char) ReplayReader.unsignedInt(thereplay, index + faux_index + i, 1);
                                }
                                String unitBP = new String(unitBluePrint);
                                faux_index += 7; //skip the blueprint

                                String workers = "";
                                if (commandType == 8) {
                                    workers = " built by ";
                                    for (int i = 0; i < numUnits; i++) {
                                        if (engineerBo.get(playerturn).containsKey(entIds[i])) {
                                            workers += (String) engineerBo.get(playerturn).get(entIds[i]) + " ";
                                        } else {
                                            if (engineerBo.get(playerturn).size() == 0) {
                                                engineerBo.get(playerturn).put(entIds[i], CommandSource[playerturn][0] + "'s ACU");
                                                workers += (String) engineerBo.get(playerturn).get(entIds[i]) + " ";
                                            } else {
                                                engineerBo.get(playerturn).put(entIds[i], "Engineer " + engineerBo.get(playerturn).size());
                                                workers += (String) engineerBo.get(playerturn).get(entIds[i]) + " ";
                                            }
                                        }
                                    }
                                }
                                if (tick <= BOtime) {

                                    buildOrder.add(new Link(unitTable.get(unitBP) + workers, playerturn, tick, commandId, entIds));
                                }
                            }

                            if (commandType == 19) {
                                String workers = "";
                                for (int i = 0; i < numUnits; i++) {
                                    if (engineerBo.get(playerturn).containsKey(entIds[i])) {
                                        workers += (String) engineerBo.get(playerturn).get(entIds[i]) + " ";
                                    } else {
                                        if (engineerBo.get(playerturn).size() == 0) {
                                            engineerBo.get(playerturn).put(entIds[i], CommandSource[playerturn][0] + "'s ACU");
                                            workers += (String) engineerBo.get(playerturn).get(entIds[i]) + " ";
                                        } else {
                                            engineerBo.get(playerturn).put(entIds[i], "Engineer " + engineerBo.get(playerturn).size());
                                            workers += (String) engineerBo.get(playerturn).get(entIds[i]) + " ";
                                        }
                                    }
                                }
                                if (tick <= BOtime) {
                                    buildOrder.add(new Link("Reclaiming by " + workers, playerturn, tick, commandId, entIds));
                                }

                            }
                            break;
                    }
                    index += message_length - 3; //skip all the data we don't need to look at

                    inputWord = new byte[1];

                }
            } else {
                buildOrder = new Link("unitDB not found. This section will not function.", 0, 0, Long.MAX_VALUE, null);
                System.err.println("bluh");
            }
            index = csl;
            this.buildorder = buildOrder;
        } else {
            this.buildorder = null;
            System.err.println("arg");
        }


    }

    /**
     * This function sets the array ActionList which is an array representing
     * the class of actions used for all in game actions. This is used for the
     * action distribution section.
     *
     * @param thereplay
     */
    public void setActionList(byte[] thereplay) {
        int[][] actionsList = new int[(int) NumSources][40];
        int currentPlayer = 0;
        long currentAction = 0;

        int playerturn = 0;
        long tick = 0;

        byte[] inputWord = new byte[1];
        if (csl != 0) {
            while (index != eof) {
                inputWord[0] = thereplay[index];
                int message_op = (int) ReplayReader.unsignedInt(inputWord, 0, 1);
                index += ONEBYTE;
                int message_length = 0;
                inputWord = new byte[2];
                inputWord[0] = thereplay[index];
                inputWord[1] = thereplay[index + 1];
                index += TWOBYTES;
                message_length = (int) ReplayReader.unsignedInt(inputWord, 0, 2);
                int faux_index = 0;

                switch (message_op) {
                    case 0:
                        tick++;
                        break;
                    case 1:
                        playerturn = (int) thereplay[index];
                        break;
                    case 12:
                        int numUnits = (int) ReplayReader.unsignedInt(thereplay, index, 4);
                        faux_index += FOURBYTES;
                        long[] entIds = new long[numUnits];
                        for (int b = 0; b < numUnits; b++) {
                            entIds[b] = ReplayReader.unsignedInt(thereplay, index + faux_index, 4);
                            faux_index += FOURBYTES;
                        }
                        long commandId = ReplayReader.unsignedInt(thereplay, index + faux_index, 4);
                        faux_index += 8; //move past commandID, and a 32 bit -1
                        int commandType = (int) ReplayReader.unsignedInt(thereplay, index + faux_index, 1);
                        if (currentAction != tick || currentPlayer != playerturn) {
                            actionsList[playerturn][commandType]++;
                            currentAction = tick;
                            currentPlayer = playerturn;

                        }
                }

                faux_index = 0;
                index += message_length - 3; //skip all the data we don't need to look at
                inputWord = new byte[1];
            }
            this.ActionsList = actionsList;
            index = csl;

        } else {
            this.ActionsList = null;
        }

    }

    /**
     * This function provides the last turn a player gave an action. This is
     * an approximation for the time of their death or game end.
     *
     * @param thereplay Takes the replay as byte array
     */
    private void setPlayerLastTurn(byte[] thereplay) {
        long playerLastTurn[] = new long[(int) NumSources];

        int currentPlayer = 0;
        long currentAction = 0;

        int playerturn = 0;

        long tick = 0;

        byte[] inputWord = new byte[1];
        if (csl != 0) {
            while (index != eof) {
                inputWord[0] = thereplay[index];
                int message_op = (int) ReplayReader.unsignedInt(inputWord, 0, 1);
                index += ONEBYTE;
                int message_length = 0;
                inputWord = new byte[2];
                inputWord[0] = thereplay[index];
                inputWord[1] = thereplay[index + 1];
                index += TWOBYTES;
                message_length = (int) ReplayReader.unsignedInt(inputWord, 0, 2);
                switch (message_op) {
                    case 0:
                        tick++;
                        break;
                    case 1:
                        playerturn = (int) thereplay[index];
                        break;
                    case 11:
                        if (currentAction != tick || currentPlayer != playerturn) {
                            currentAction = tick;
                            currentPlayer = playerturn;
                            playerLastTurn[playerturn] = tick;
                        }
                        break;
                    case 12:
                        playerLastTurn[playerturn] = tick;
                        break;
                    case 13:
                        playerLastTurn[playerturn] = tick;
                        break;
                    case 19:
                        if (currentAction != tick || currentPlayer != playerturn) {
                            currentAction = tick;
                            currentPlayer = playerturn;
                            playerLastTurn[playerturn] = tick;
                        }
                        break;
                    case 22:
                        if (currentAction != tick || currentPlayer != playerturn) {
                            currentAction = tick;
                            currentPlayer = playerturn;
                            playerLastTurn[playerturn] = tick;
                        }
                }
                index += message_length - 3; //skip all the data we don't need to look at
                //we're just looking for the time in this function.
                inputWord = new byte[1];
            }
            this.PlayerLastTurn = playerLastTurn;
            index = csl;

        } else {
            this.GameTime = 0;
        }

    }
}
