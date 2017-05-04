package org.ucombinator.jaam.visualizer.main;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.Iterator;

public class JAAMUtils {

	public static String RED(String string) {
		return "\033[31m"+string;
	}
	
	public static String YELLOW(String string) {
		return "\033[33m"+string;
	}
	
	public static String GREEN(String string) {
		return "\033[32m"+string;
	}
	
	public static String BLUE(String string) {
		return "\033[34m"+string;
	}
	
	public static String WHITE(String string) {
		return "\033[0m"+string;
	}

}
