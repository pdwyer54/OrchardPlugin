// Written by Phil Dwyer
// Debug class, this helps us easily turn this on and off depending on if we are testing or not
// All classes should have an instance of this class and the className variable set at the top

package com.orchardsoft.plugin.OrchardPlugin;

// For the debug logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Debug{

    private static final Logger log = LoggerFactory.getLogger(Debug.class);
    private static final boolean canLog = true; // Set this to false in prod systems
    public static boolean isTest = false; // Set this to false in prod systems as well

    public Debug() {

    }

    public void logdebug(String message,String className) { // Logs to atlassian-log in live system if Debug level is set.
        String string = "Class Name: ";
        string = string+className;
        string = string+" Message: ";
        string = string+message;
        if(canLog) {
            log.info(string);
        }
    }
    public void printDebugToFile(String message, String filepath) {
        //String fileName = String.format("%s\debug.txt",filepath);
        //message = String.format(message);
        if(canLog) {
            //PrintWriter out = new PrintWriter(fileName);
            //out.println(message);
            //out.close();
        }

    }

}