/**
 * @author Aaron Elligsen
 */

package com.faforever.fachart;

import com.jcraft.jzlib.InflaterInputStream;
import sun.misc.BASE64Decoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Scanner;


/**
 * This class is a listner for the GUI when a user choosed to open a replay for
 * analysis
 */
public class loadListener implements ActionListener {
    JFrame Parent;
    Hashtable<String, String> uTable;


    public loadListener(JFrame p, Hashtable<String, String> unitTable) {
        Parent = p;
        uTable = unitTable;
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new ReplayFilter());

        try {
            Scanner scan = new Scanner(new File("configSave"));
            String previousFileDir = scan.nextLine();
            fc.setCurrentDirectory(new File(previousFileDir));
        } catch (FileNotFoundException d) {
            System.err.println("Error: Could not open previous file location");

            fc.setCurrentDirectory(new File("C:\\ProgramData\\FAForever\\replays"));
        }


        int returnVal = fc.showOpenDialog(Parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file[] = fc.getSelectedFiles();

            try {
                PrintWriter print = new PrintWriter("configSave");
                String fileDir = file[0].getAbsolutePath();
                print.println(fileDir.substring(0, fileDir.lastIndexOf('\\') + 1));
                print.close();
            } catch (FileNotFoundException b) {
                System.err.println("Error: Could not save file location");
            }

            for (int i = 0; i < file.length; i++) {
                JDialog temp = new JDialog(Parent, file[i].getName(), false);
                temp.setMaximumSize(new Dimension(700, 600));
                temp.setPreferredSize(new Dimension(700, 600));
                FileInputStream thereplay = null;
                try {
                    thereplay = new FileInputStream(file[i]);
                    try {
                        int fileSize = thereplay.available();
                        byte[] replaybytes = new byte[fileSize];
                        //reading the replay
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
                            OutputStream os = new FileOutputStream(new File("bin.blob"));
                            while ((len = zs.read(buff)) > 0) {
                                os.write(buff, 0, len);
                                for (int j = 0; j != len; j++) {
                                    result.add(buff[j]);
                                }
                            }

                            replaybytes = new byte[result.size()];
                            for (int j = 0; j != result.size(); j++) {
                                replaybytes[j] = result.get(j);
                            }


                        } else {
                            throw new IOException("invalid format");
                        }

                        try {
                            temp.getContentPane().add(new FACTabbedPane(replaybytes, uTable, replaybytes.length));
                        } catch (NoClassDefFoundError b) {
                            JDialog noLibrary = new JDialog(Parent, "Missing library", true);
                            noLibrary.setPreferredSize(new Dimension(150, 50));
                            noLibrary.setResizable(false);
                            JLabel missing = new JLabel("Chart library is missing.");
                            noLibrary.getContentPane().add(missing);
                            noLibrary.pack();
                            noLibrary.setVisible(true);
                        }
                    } catch (IOException t) {
                        JDialog noData = new JDialog(Parent, "Replay:" + file[i].getName() + " data inaccessible", true);
                        noData.setPreferredSize(new Dimension(400, 50));
                        noData.setResizable(false);
                        JLabel missing = new JLabel("Replay data could not be read." + t.getMessage());
                        noData.getContentPane().add(missing);
                        noData.pack();
                        noData.setVisible(true);
                    }
                } catch (FileNotFoundException g) {
                    JDialog noFile = new JDialog(Parent, "Replay:" + file[i].getName() + " not found.", true);
                    noFile.setPreferredSize(new Dimension(200, 50));
                    noFile.setResizable(false);
                    JLabel missing = new JLabel("Replay:" + file[i].getName() + " is not found");
                    noFile.getContentPane().add(missing);
                    noFile.pack();
                    noFile.setVisible(true);
                }
                temp.pack();
                temp.setVisible(true);
            }

        } else {
            //Open command canceled by user, hence do nothing.
        }
    }


}
