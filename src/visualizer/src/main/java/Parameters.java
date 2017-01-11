
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JTextArea;

import java.awt.Font;
import java.awt.Color;

// TODO: Remove stFrame variable and make StacFrame a singleton class
public class Parameters
{
	public static boolean debugMode = true;
	public static int width = 1200, height = 800;
	public static int transitionTime = 500;
	public static double zoomFactor = 3.0/4.0, boxFactor = 3.0/4.0;
	public static StacFrame stFrame;
	public static JTextArea rightArea;
	public static CodeArea bytecodeArea;
	public static CodeArea decompiledArea;
    public static SearchArea searchArea;
	public static String currDirectory = "./";
	public static javafx.scene.paint.Color fxColorFocus = javafx.scene.paint.Color.BLUE,
			fxColorSelection = javafx.scene.paint.Color.ALICEBLUE,
			fxColorHighlight = javafx.scene.paint.Color.YELLOW;
	public static Color colorFocus = new Color(Integer.parseInt("FFF7BC", 16)),
			colorSelection = new Color(Integer.parseInt("A6BDDB", 16)),
			colorHighlight = new Color(Integer.parseInt("2B8CBE", 16));
	public static int transparency = 160;
	public static Font font = new Font("Serif", Font.PLAIN, 14);
	
	public static boolean debug = false;
	public static long interval = 5000;
	public static long mouseLastTime;
	public static boolean highlightIncoming = false, highlightOutgoing = false, vertexHighlight = true;
    public static boolean fixCaret = false;
	
	public static int debug1, debug2;
	public static long limitV = Long.MAX_VALUE;
	
	public static void setRightText()
	{
		StringBuilder text = new StringBuilder();
		for(Vertex v : Main.graph.vertices)
		{
            if(v.isSelected)
				text.append(v.getRightPanelContent() + "\n\n");
		}
		
		for(MethodVertex v : Main.graph.methodVertices)
		{
            if(v.isSelected)
				text.append(v.getRightPanelContent() + "\n\n");
		}
		
		rightArea.setText(text.toString());
		rightArea.setCaretPosition(0);
	}

	public static File openFile(boolean includeDirectories)
	{
		try
		{
			JFileChooser choose = new javax.swing.JFileChooser(new File(currDirectory).getCanonicalPath());
			if(includeDirectories)
				choose.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

			int ret = choose.showOpenDialog(null);
			if(ret == JFileChooser.APPROVE_OPTION)
			{
				currDirectory = Parameters.folderFromPath(choose.getSelectedFile().getAbsolutePath());
				return choose.getSelectedFile();
			}
			else return null;
		}
		catch(IOException ex)
		{
			return null;
		}
	}

	public static String folderFromPath (String path)
	{
		int lastSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
		if(lastSlash == -1)
			return "/";
		
		String folder = path.substring(0,lastSlash);
				
		return folder;
	}

	public static void repaintAll()
	{
		System.out.println("Repainting...");
		if (!Parameters.debugMode)
		{
			bytecodeArea.setDescription();
			setRightText();
			searchArea.writeText();
		}

		stFrame.repaint();

        /*if(Parameters.fixCaret)
        {
            Parameters.fixCaret = false;
            Parameters.fixCaretPositions();
        }*/
	}
    
    public static void fixCaretPositions()
    {
        bytecodeArea.fixCaretPosition();
        searchArea.fixCaretPosition();
    }

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
