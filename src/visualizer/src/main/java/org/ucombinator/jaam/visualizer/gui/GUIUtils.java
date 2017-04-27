package org.ucombinator.jaam.visualizer.gui;

import javafx.stage.FileChooser;
import javafx.scene.layout.Pane;

import java.io.File;

/**
 * Created by timothyjohnson on 4/17/17.
 */
public class GUIUtils {

    public static File openFile(Pane p, String title)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        return fileChooser.showOpenDialog(p.getScene().getWindow());
    }

    public static String folderFromPath (String path)
    {
        int lastSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        if(lastSlash == -1)
            return "/";

        String folder = path.substring(0,lastSlash);

        return folder;
    }
}
