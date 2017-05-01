package org.ucombinator.jaam.visualizer.main;

import java.io.File;
import java.io.IOException;

//import javax.swing.JFileChooser;
import javafx.scene.control.TextArea;

import javafx.scene.text.Font;
import javafx.scene.paint.Color;

import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.gui.SearchResults;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.gui.CodeArea;
import org.ucombinator.jaam.visualizer.gui.StacFrame;

// TODO: Remove stFrame variable and make StacFrame a singleton class
public class Parameters
{
	public static final boolean debugMode = false;
	public static final int width = 1000, height = 600;
	public static final int transparency = 160;
	public static Font jfxFont = new Font("Serif", 14);

	public static String getHTMLVerbatim(String str)
	{
		int pos = -1;
		String suf = ""+str;

		String pre = "";
		while(true)
		{
			pos = suf.indexOf('&');
			if(pos < 0)
				break;
			pre = pre + suf.substring(0,pos) + "&amp;";
			suf = suf.substring(pos+1);
		}
		suf = pre + suf;

		pre = "";
		while(true)
		{
			pos = suf.indexOf('<');
			if(pos<0)
				break;
			pre = pre + suf.substring(0,pos)+"&lt;";
			suf = suf.substring(pos+1);
		}
		suf = pre + suf;

		pre = "";
		while(true)
		{
			pos = suf.indexOf('>');
			if(pos<0)
				break;
			pre = pre + suf.substring(0,pos)+"&gt;";
			suf = suf.substring(pos+1);
		}
		suf = pre + suf;

		return "<code>"+suf+"</code>";
	}
}
