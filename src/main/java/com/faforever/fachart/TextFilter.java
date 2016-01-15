/**
 * @author Aaron Elligsen
 */

package com.faforever.fachart;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * This class ensures that build orders are exported in the .txt format
 *
 */
public class TextFilter extends FileFilter {

    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = FileUtil.getExtension(f);
        return extension != null && extension.equals("txt");

    }

    //The description of this filter
    public String getDescription() {
        return "Text";
    }
}
