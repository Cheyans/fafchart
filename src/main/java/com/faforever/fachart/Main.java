/**
 * @author Aaron Elligsen
 */

package com.faforever.fachart;


import com.jcraft.jzlib.InflaterInputStream;
import sun.misc.BASE64Decoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Scanner;

/**
 * This program is a replay analyzer for Supreme Commander: Forged Alliance.
 * It provides quantitative data on how players performed by measuring things like
 * commands per minute (multitasking speed), micro/macro (economic versus strategic focus),
 * build orders and so on. It is a tool for competitive players looking to better
 * improve their skills. 
 *
 * It was written using the Java Swing library and the open source graphing library JFreeChart.
 * JFreeChart is licensed under the terms of the GNU Lesser General
 * Public Licence (LGPL).  A copy of the licence is included in the
 * distribution. 
 * http://www.jfree.org/jfreechart/
 */
public class Main {

    /**
     * Main method which creates the base frame for the Analyzer. Replays can be 
     * opened from this frame as well as a global close. It provides version info too
     *.
     * @param args the command line arguments containing nothing or full path to 
     * one or several replays with spaces escaped as '*'s
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            System.out.println("Unable to load Windows look and feel");
        }


        JFrame frame = new JFrame("FAchart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        UnitDBReader unitDBReader = new UnitDBReader();
        Hashtable<String, String> unitTable = unitDBReader.readUnitDb();


        JMenuBar menuBar;
        JMenu menu;

        menuBar = new JMenuBar();
        menu = new JMenu("File");
        menuBar.add(menu);

        JMenuItem menuItem;
        menuItem = new JMenuItem("Open Replays",
                KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Loads one or more replay");
        menuItem.addActionListener(new loadListener(frame, unitTable));
        menu.add(menuItem);
        menu.addSeparator();
        JMenuItem quitMenuItem = new JMenuItem("Quit FAChart",
                KeyEvent.VK_Q);
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        quitMenuItem.addActionListener(new exitListener());
        menu.add(quitMenuItem);

        frame.setJMenuBar(menuBar);
        JPanel centralPane = new JPanel();
        centralPane.setLayout(new BoxLayout(centralPane, BoxLayout.Y_AXIS));
        centralPane.setPreferredSize(new Dimension(400, 100));
        centralPane.setMaximumSize(new Dimension(400, 100));
        JLabel version = new JLabel("Version 1.6");
        JLabel useage = new JLabel("To analyze replay(s) click on File->Open Replays (Ctrl+O for short). ");
        JLabel author = new JLabel("Author: Aaron Elligsen");
        JLabel thing = new JLabel("\"Supreme Commander\" and \"Supreme Commander Forged Alliance\" are registered");
        JLabel thing2 = new JLabel("trademarks of Gas Power Games Corp all rights reserved.");
        centralPane.add(version);
        centralPane.add(useage);
        centralPane.add(author);
        centralPane.add(thing);
        centralPane.add(thing2);
        frame.getContentPane().add(centralPane);
        
        
        /*
         * This portion is for sending replays directly to the program through the command line
         */

        if (args.length > 0) {
            File[] replayFiles = new File[args.length];
            for (int i = 0; i < args.length; i++) {

                String fp = Main.unEscapeSpaceString(args[i]);
                replayFiles[i] = new File(fp);

                JDialog temp = new JDialog(frame, replayFiles[i].getName(), false);
                temp.setPreferredSize(new Dimension(700, 600));
                temp.setMaximumSize(new Dimension(700, 600));
                FileInputStream thereplay = null;
                try {
                    thereplay = new FileInputStream(replayFiles[i]);
                    try {
                        int fileSize = thereplay.available();
                        byte[] replaybytes = new byte[fileSize];
                        thereplay.read(replaybytes);
                        //splitting it by newlines
                        String[] rp = new String(replaybytes).split("\\n");
                        ;
                        if (rp.length == 2) {
                            //base64->binary (zlib compressed)
                            BASE64Decoder decoder = new BASE64Decoder();
                            replaybytes = decoder.decodeBuffer(rp[rp.length - 1]);

                            //qCompress uses the first 4 bytes to store the size; removing the first 4 bytes
                            replaybytes = Arrays.copyOfRange(replaybytes, 4, replaybytes.length);

                            //Unpack the data
                            InflaterInputStream zs = new InflaterInputStream(new ByteArrayInputStream(replaybytes));
                            ArrayList<Byte> result = new ArrayList<Byte>(1000000);

                            //reading the unpacked data
                            byte[] buff = new byte[1000];
                            int len = 0;
                            while ((len = zs.read(buff)) > 0) {
                                for (int j = 0; j != len; j++) {
                                    result.add(buff[j]);
                                }
                            }

                            //closing the inflation stream
                            zs.close();


                            replaybytes = new byte[result.size()];
                            for (int j = 0; j != result.size(); j++) {
                                replaybytes[j] = result.get(j);
                            }


                        } else {
                            throw new IOException("invalid format");
                        }
                        try {
                            temp.getContentPane().add(new FACTabbedPane(replaybytes, unitTable, replaybytes.length));
                        } catch (NoClassDefFoundError b) {
                            JDialog noLibrary = new JDialog(frame, "Missing library", true);
                            noLibrary.setPreferredSize(new Dimension(150, 50));
                            noLibrary.setResizable(false);
                            JLabel missing = new JLabel("Chart library is missing.");
                            noLibrary.getContentPane().add(missing);
                            noLibrary.pack();
                            noLibrary.setVisible(true);
                        }
                    } catch (IOException t) {
                        JDialog noData = new JDialog(frame, "Replay:" + replayFiles[i].getName() + " data inaccessible", true);
                        noData.setPreferredSize(new Dimension(150, 50));
                        noData.setResizable(false);
                        JLabel missing = new JLabel("Replay data could not be read.");
                        noData.getContentPane().add(missing);
                        noData.pack();
                        noData.setVisible(true);
                    }
                } catch (FileNotFoundException g) {
                    JDialog noFile = new JDialog(frame, "Replay:" + replayFiles[i].getName() + " not found.", true);
                    noFile.setPreferredSize(new Dimension(200, 50));
                    noFile.setResizable(false);
                    JLabel missing = new JLabel("Replay:" + replayFiles[i].getName() + " is not found");
                    noFile.getContentPane().add(missing);
                    noFile.pack();
                    noFile.setVisible(true);
                }
                temp.pack();
                temp.setVisible(true);
            }
        }

        frame.pack();
        frame.setVisible(true);


    }

    /**
     * Replaces any spaces in path names with *
     * @param String Full replay path
     * @return escaped path string
     */
    static public String unEscapeSpaceString(String fp) {
        StringBuffer theFP = new StringBuffer(fp);
        while (theFP.lastIndexOf("*") != -1) {
            theFP.setCharAt(theFP.lastIndexOf("*"), ' ');
        }

        return theFP.toString();
    }

}
