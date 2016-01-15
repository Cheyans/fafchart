/**
 * @author Aaron Elligsen
 */
package com.faforever.fachart;

import java.util.Hashtable;

/*release features *CPM/
        *MacroMicroCPM/
		*BuildOrder by time
		BuildOrder by engineer
		*CPM order type distribution
		unit distribution?
*/
public class ReplayReader {

    byte[] thereplay;
    int endOfFile;


    public ReplayReader(byte[] replaybytes, int file_length) {
        thereplay = replaybytes;
        endOfFile = file_length;
    }

    /**
     * As Java does not support unsigned integers it was necessary to create
     * a function to convert the bytes Java read in.
     *
     * @param words The java io for accessing raw bits returns in byte arrays
     * @param index This index is where an integer should start if dealing
     * with large arrays of various data.
     * @param numBytes The number of 8 bit bytes to restore the integer (1, 2, and 4 are most common)
     * @return long int containing an unsigned int
     */
    public static long unsignedInt(byte[] words, int index, int numBytes) //converts little endian unsigned int to int
    {
        long value = 0;
        int byteStep = 0;
        for (int i = 0; i < numBytes; i++) {
            int temp = (int) words[index + i] & 0xFF;

            value = value | (temp << byteStep);
            byteStep += 8;

        }
        return value;
    }


    /**
     * Analyze is the meat and potatoes of this program. It started out as a
     * very procedural function. I have begun breaking it apart and making it
     * more class oriented. However, the analysis portion of the program
     * is best handled by a rather large switch statement. Mining the data as
     * the
     *
     * @param aReplay Takes the replay as a byte array
     * @param parent The analyze function may need to pass message back to
     * the parent frame in case of errors/exceptions
     */
    public Replay Analyze(byte[] thereplay, Hashtable<String, String> unitTable) {

        Replay replayData = new Replay(unitTable);

        /////////////////Header
        replayData.analyzeHeader(thereplay, endOfFile);

        ////////////////Everything else
        replayData.setGameTime(thereplay);
        replayData.setMicroAPM(thereplay);

        replayData.setActionList(thereplay);
        replayData.setAPMS_actionTotal(thereplay);
        replayData.setBuildorder(thereplay);


        return replayData;


    }

}



