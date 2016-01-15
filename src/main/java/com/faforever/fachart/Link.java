/**
 * @author Aaron Elligsen
 */
package com.faforever.fachart;

/**
 * This class is an ordered linked list which is sorted by 2 different parameters(player, and time).
 * The class also allows removal of links based on another parameter (commandId)
 * In addition it also prints out the array in a useful fashion.
 */
public class Link {
    public Link next;
    public Link previous;
    public String theObject;
    public long timestamp;
    public int player;
    public long commandId;
    public long[] entityIds;

    int occurrence;
    String newLine = System.getProperty("line.separator");

    /**
     * This creates Links containing new build order events
     *
     * @param obj This is the String which says what the order actually was.
     * @param pn This is the integer representing which player gave the command
     * @param ts This is the timestamp in ticks when the command was given
     * @param ci This is the id of the specific command given
     * @param eis This is an array of the entity Ids of the units involved in the action
     */
    public Link(String obj, int pn, long ts, long ci, long[] eis) {
        next = null;
        previous = null;
        theObject = obj;
        player = pn;
        timestamp = ts;
        commandId = ci;
        entityIds = eis;

        occurrence = 1;
    }

    /**
     * This adds events to the Build order and inserts them in the correct fashion
     * into a sort list based on Player and time. It also reduces repetetive actions
     * by providing a count
     *
     * @param cevent Takes a new Link to insert in the list
     */
    public void add(Link cevent) {
        Link temp = this;
        while (cevent.player > temp.player && temp.next != null) {
            temp = temp.next;
        }
        while (cevent.timestamp > temp.player && temp.next != null && temp.next.player == cevent.player) {
            temp = temp.next;
        }
        if (temp.next != null) {
            cevent.next = temp.next;
            cevent.next.previous = cevent;
        }
        if (temp.theObject.equals(cevent.theObject)) {
            temp.occurrence++;
        } else {

            cevent.previous = temp;
            temp.next = cevent;
        }
    }

    /**
     * Removes commands from the list which were cancelled by the player.
     *
     * @param ci commandID for the command that was cancelled.
     */
    public void remove(long ci) {
        Link temp = this;
        while (temp.commandId != ci && temp.next != null) {
            temp = temp.next;
        }
        if (temp.commandId == ci) {
            if (temp.next != null) {
                temp.next.previous = temp.previous;
                temp.previous.next = temp.next;

            } else {
                temp.previous.next = null;
            }
        }

    }

    /**
     * This creates a string which represents the build orders in a readable fashion.
     *
     */
    public String writeBO() {
        String theBo = "Timestamp/Count/Action\n";
        Link temp = this;
        int timeMin;
        int timeSec;
        if (!(this.next != null)) {
            timeMin = (int) Math.floor(this.timestamp / 600);
            timeSec = (int) Math.floor((this.timestamp / 10) - timeMin * 60);
            theBo += String.format("%2d:%02d  %3dx  %s%s", timeMin, timeSec, this.occurrence, this.theObject, newLine);
        }
        while (temp.next != null) {
            timeMin = (int) Math.floor(temp.timestamp / 600);
            timeSec = (int) Math.floor((temp.timestamp / 10) - timeMin * 60);
            if (temp.previous != null) {
                if (temp.previous.player != temp.player) {
                    theBo += newLine;
                }
            }
            theBo += String.format("%2d:%02d  %3dx  %s%s", timeMin, timeSec, temp.occurrence, temp.theObject, newLine);
            temp = temp.next;
        }
        return theBo;
    }

}