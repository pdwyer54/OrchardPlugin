package com.orchardsoft.plugin.OrchardPlugin.CreatedIssueListener;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.atlassian.jira.issue.IssueManager;

import java.lang.reflect.Array;
import java.util.regex.Pattern;

// This class will handle any line separation and validation of the description that was sent from a ticket that has values. Currently
// that is for only Instrument Form.

public class ParseTicket {

    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private static final UserManager userManager = ComponentAccessor.getUserManager();
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();

    public ParseTicket(){

    }

    // Method to actually parse the line, it is supposed to look like this:
    // Requester's Email * pdwyer
    // We split on the * and get the value
    public String parseLine(String line){
        debugger.logdebug("Parse Line: "+line,className);
        String parsedLine = "";
        String[] linesplit = line.split(Pattern.quote("*"));
        if (linesplit.length >= 2){
            parsedLine = linesplit[1];
            parsedLine = parsedLine.trim();
            debugger.logdebug("Parsed line: "+parsedLine,className);
        } else {
            debugger.logdebug("There was an issue with splitting this line: "+line,className);
        }

        return parsedLine;
    }

    // Method that will get an string that should correlate with a ticket number
    // and send back the issue object. Passes back null if we do not have a valid number
    public Issue getIssue(String issueString){
        Issue issue = null;

        try{
            issue = issueManager.getIssueByCurrentKey(issueString);
        } catch (DataAccessException x){
            debugger.logdebug("There was an issue with finding the issue: "+issueString,className);
            debugger.logdebug(x.getMessage(),className);
        }
        return issue;
    }

    // Method that will validate all the tickets that are related to the instrument request. These are strings
    // and we need to verify that they are valid ticket in the system
    public Boolean validateTickets(String specValue, String pmDevValue, String pmFiaValue){
        Boolean valid = false;
        debugger.logdebug("Validing the tickets: "+specValue+" "+pmDevValue+" "+pmFiaValue,className);

        valid = validateLine("specreview",specValue);
        if(valid) {
            valid = validateLine("pmdev", pmDevValue);
        }
        if (valid) {
            valid = validateLine("pmfia", pmFiaValue);
        }

        return valid;
    }

    // Method that will check the various line in the instrument request form and verify that it has the correct and valid info
    // We also pass in the option so we know what we should be checking.
    public Boolean validateLine(String option, String value){
        Boolean valid = false;
        debugger.logdebug("Validating the line: "+option+" "+value,className);
        if(option == "product"){
            if (value.contains("ODE") || value.contains("Harvest Interface Engine")){
                valid = true;
            }
        } else if(option == "username"){
            // If the username isn't valid we really don't know who to send the failure email to
            ApplicationUser user = userManager.getUserByNameEvenWhenUnknown(value);
            if (userManager.isUserExisting(user)) {
                valid = true;
            } else {
                debugger.logdebug("No user exists. Notify someone",className);
            }
        } else if(option == "specreview" || option == "pmdev" || option == "pmfia"){
            // Check if it's a ticket, if not set to false, we handle failure emails outside of this method
            Issue issue = issueManager.getIssueByCurrentKey(value);
            if(issue != null){
             valid = true;
            }
        }

        return valid;
    }

}