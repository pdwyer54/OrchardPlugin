package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.orchardsoft.plugin.OrchardPlugin.Debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DownloadHelper {

    private String className = this.getClass().getSimpleName();
    private static Debug debugger = new Debug();

    public String versionRelease(Version version,String strVersion, String projectKey){

        debugger.logdebug("Running versionRelease", className);

        // What are we doing?
        Boolean bSendEmail = true;

        // If we have these things then we are downloading
        if (version == null && strVersion != ""){
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project project = projectManager.getProjectByCurrentKey(projectKey);
            Collection<Version> versions =project.getVersions();
            for (Version ver: versions){
                if(ver.getName().equals(strVersion)){
                    version = ver;
                    debugger.logdebug("We are downloading, version found: "+version.getName(), className);
                    bSendEmail = false;
                    break;


                }
            }
        }

        String renderedText = "";
        if (version != null) {
            ProjectHelper projectHelper = new ProjectHelper();
            BuildQuery queryBuilder = new BuildQuery();
            final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user
            VersionManager versionManager = ComponentAccessor.getVersionManager();
            Project project = version.getProject();

            // Check if we are in a test system, if we aren't we need to make sure this is a project we actually can run this on.
            // If it is a test system always run it
            boolean Continue = false;
            if (!(debugger.isTest)) {
                Continue = checkProject(version.getProject().getKey());
            } else {
                Continue = true;
            }

            if (Continue) {
                debugger.logdebug(version.getName(), className);


                // Getting all the items
                Query query;
                SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
                SearchResults searchResults;
                Collection<ProjectComponent> componentList = projectHelper.getComponentByProject(project); // Component stuff that will eventually be needed for Copia, Trellis and Sequoia
                Collection<IssueType> issueTypeList = projectHelper.getIssueTypeByProject(project); // This list is sorted in a way already, however it's sorted in the project scheme
                List<Issue> issuesInList = new ArrayList<>();
                ArrayList<Issue> allIssues = new ArrayList<>();
                //for (ProjectComponent component : componentList) { // Run a query for each component, this is for Copia eventually
                for (IssueType issueType : issueTypeList) {
                    query = queryBuilder.JQLBuilderBuild(project.getName(), version.getName(), "", issueType);

                    MessageSet errorMessages = searchService.validateQuery(user, query);
                    if (errorMessages.hasAnyErrors()) {
                        // Add error logging
                        debugger.logdebug("Failure in Query", className);
                    } else {
                        try {
                            searchResults = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                            issuesInList = searchResults.getIssues();
                            // debugger.logdebug(component.getName(), className);
                            debugger.logdebug(Integer.toString(issuesInList.size()), className);

                            if (!issuesInList.isEmpty()) {
                                int markedChange = allIssues.size();
                                allIssues.addAll(issuesInList);
                            }

                        } catch (Exception e) {
                            debugger.logdebug("Line 134", className);
                            debugger.logdebug(e.getMessage(), className);
                        }
                    }
                }


                //}
                // Run the blank component last
                //query = queryBuilder.JQLBuilderBuild(project.getName(),version.getName(),"",issueTypeList);
                //MessageSet errorMessages = searchService.validateQuery(user, query);
                //if (errorMessages.hasAnyErrors()) {
                // Add error logging
                //	debugger.logdebug("Failure in Query",className);
                //} else {
                //	try {
                //		searchResults = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                //		issuesInList = searchResults.getIssues();
                //		debugger.logdebug(Integer.toString(issuesInList.size()),className);

                //		if (!issuesInList.isEmpty()) {
                //			allIssues.addAll(issuesInList);
                //		}

                //	} catch (Exception e){
                //		debugger.logdebug("Line 159",className);
                //		debugger.logdebug(e.getMessage(),className);
                //	}
                //}

                // Build template
                debugger.logdebug("Attempting Build Template", className);
                String template = "";
                BuildTemplate buildTemplate = new BuildTemplate();
                if (bSendEmail){
                    template = "ReleaseNotesHTML.vm";
                }else{
                    template = "ReleaseNotesHTML3.vm";
                }

                renderedText = buildTemplate.getTemplate(allIssues, project, version,template);

                // For now we are going to send the email to whoever clicked release, this way we can somewhat screen the email before sending to to almost all the company
                JiraAuthenticationContext authContext = ComponentAccessor.getJiraAuthenticationContext();
                ApplicationUser loggedInUser = authContext.getLoggedInUser();


                SendEmail sendEmail = new SendEmail();
                String emailList = sendEmail.gatherEmailList(project, "pdwyer"); // Get the list, right now it's just returning the user who builds the version
                if (version.getName().contains("Next")) { // Error out if it has next in it
                    emailList = sendEmail.getUserEmail(loggedInUser.getUsername());
                }

                String versionDescription = "";
                if (version.getDescription() != null) {
                    versionDescription = version.getDescription();
                }

                if (versionDescription.contains("DNS")) { // If we added DNS to the description, don't send the email
                    bSendEmail = false;
                } else if (versionDescription.contains("download")) { // If we just want to download we will slap download in the description
                    bSendEmail = false;
                }
                debugger.logdebug(emailList, className);
                String subject = sendEmail.setSubject(project.getName(), version.getName(), version); // Build the subject title
                debugger.logdebug(subject, className);

                // Send the email
                if (bSendEmail) {
                    sendEmail.sendTheEmail(emailList, subject, renderedText);
                }
            }

        }

        return renderedText;
    }

    private boolean checkProject(String projectKey) { // Check the project to make sure we should even be doing this
        ProjectHelper projectHelper = new ProjectHelper();
        ArrayList<String> projectList=new ArrayList<String>();
        projectHelper.buildProjectList(projectList);
        return projectList.contains(projectKey);
    }

}
