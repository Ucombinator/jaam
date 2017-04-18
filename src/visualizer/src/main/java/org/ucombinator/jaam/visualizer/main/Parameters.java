package org.ucombinator.jaam.visualizer.main;

import java.io.File;
import java.io.IOException;

//import javax.swing.JFileChooser;
import javafx.scene.control.TextArea;

import java.awt.Font;
import java.awt.Color;

import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.gui.SearchResults;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.gui.CodeArea;
import org.ucombinator.jaam.visualizer.gui.StacFrame;

// TODO: Remove stFrame variable and make StacFrame a singleton class
public class Parameters
{
	public static boolean debugMode = false;
	public static boolean edgeVisible = true;
	public static final int width = 1000, height = 600;
	public static double boxFactor = 3.0/4.0;
	public static String currDirectory = "./";
	public static javafx.scene.paint.Color fxColorFocus = javafx.scene.paint.Color.BLUE,
			fxColorSelection = javafx.scene.paint.Color.ALICEBLUE,
			fxColorHighlight = javafx.scene.paint.Color.YELLOW;
	public static Color colorFocus = new Color(Integer.parseInt("FFF7BC", 16)),
			colorSelection = new Color(Integer.parseInt("A6BDDB", 16)),
			colorHighlight = new Color(Integer.parseInt("2B8CBE", 16));
	public static int transparency = 160;
	public static Font font = new Font("Serif", Font.PLAIN, 14);
	public static javafx.scene.text.Font jfxFont = new javafx.scene.text.Font("Serif", 14);
	
	public static boolean debug = false;
	public static long interval = 5000;
	public static long mouseLastTime;
	public static boolean vertexHighlight = true;
    public static boolean fixCaret = false;
	
	public static int debug1, debug2;
	public static long limitV = Long.MAX_VALUE;

	public static String getHTMLVerbatim(String str)
	{
		int pos = -1;
		String suf = ""+str;

		String pre = "";
		while(true)
		{
			pos = suf.indexOf('&');
			if(pos<0)
				break;
			pre = pre + suf.substring(0,pos)+"&amp;";
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
