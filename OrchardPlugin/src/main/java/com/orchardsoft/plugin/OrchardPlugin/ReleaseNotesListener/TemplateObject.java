// Written by Phil Dwyer
// Class that holds a lot off issue info, and we pass an array of these into the template

package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

//internal imports
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.*;
import com.orchardsoft.plugin.OrchardPlugin.*;

import com.atlassian.jira.issue.Issue; // Issue object, very important for using an issue's specific data

public class TemplateObject {
    public String releaseNotes = "";
    public String issueText = "";
    public Issue issue;
    public String fixedBy = "";
    public String clientName = "";
    public String Type = "";
    public String Page = "";
    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();

    public TemplateObject(String releasenotes, String issuetext, String fixedby, String clientname, String type, String page, Issue i) { // Fill all
        releaseNotes = releasenotes;
        issueText = issuetext;
        fixedBy = fixedby;
        clientName = clientname;
        Type = type;
        Page = page;
        issue = i;

    }

    public TemplateObject() { // Fill none
        releaseNotes = "";
        issueText = "";
        fixedBy = "";
        clientName = "";
        Type = "";
        issue = null;
        Page = "";
    }

    // These require getters because the version of velocity that we use doesn't let you just grab a variable like we can in java

    public String getIssueType() {
        return Type;
    }

    public String getIssueText() {
        return issueText;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public String getFixedBy() {
        return fixedBy;
    }

    public String getClientName() {
        return clientName;
    }

    public String getPage() { return Page; }

    // Print out everything in the object
    public void printOutInLog() {
        debugger.logdebug("Issue object:",className);
        debugger.logdebug("Release Notes: "+releaseNotes,className);
        debugger.logdebug("Issue Text: "+issueText,className);
        debugger.logdebug("Fixed By: "+fixedBy,className);
        debugger.logdebug("Client Name: "+clientName,className);
        debugger.logdebug("Type: "+Type,className);
        debugger.logdebug("Page: "+Page,className);

    }

}