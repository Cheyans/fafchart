/**
 * @author Aaron Elligsen
 */

package com.faforever.fachart;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * This class ensures only Supreme Commander Forged Alliance replays are loaded.
 *
 */
public class ReplayFilter extends FileFilter {

    public ReplayFilter() {


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

    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = ReplayFilter.getExtension(f);
        if (extension != null) {
            if (extension.equals("fafreplay")) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    //The description of this filter
    public String getDescription() {
        return "FAFReplay";
    }
}
