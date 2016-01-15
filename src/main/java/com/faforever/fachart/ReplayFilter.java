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
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = FileUtil.getExtension(f);
        return extension != null && extension.equals("fafreplay");

    }

    //The description of this filter
    public String getDescription() {
        return "FAFReplay";
    }
}
