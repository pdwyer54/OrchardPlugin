package com.orchardsoft.plugin.OrchardPlugin.Reporting;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.sal.api.user.UserManager;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.BuildQuery;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.ProjectHelper;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.SendEmail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.text.DecimalFormat;

public class ReportingMain {

    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private ProjectHelper projectHelper = new ProjectHelper();
    final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user
    private static DecimalFormat df = new DecimalFormat("0.00");

    public String doReport(String filter) throws JqlParseException {

        String finalText = "";
        Query query;
        SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
        SearchResults searchResults;
        List<Issue> issuesInList = new ArrayList<>();
        ArrayList<Issue> allIssues = new ArrayList<>();
        BuildQuery queryBuilder = new BuildQuery();

        List<String> issueTypeFinalList = new ArrayList<>();
        List<Long> timeTrack = new ArrayList<>();
        List<Long> timeTrackPersonal = new ArrayList<>();


        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);
        query = parser.parseQuery(filter);

        MessageSet errorMessages = searchService.validateQuery(user, query);
        if (errorMessages.hasAnyErrors()) {
            // Add error logging
            debugger.logdebug("Failure in Query", className);
        } else{
            try {
                searchResults = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                if(!(searchResults.getTotal()>300)){
                    issuesInList = searchResults.getResults();
                    // debugger.logdebug(component.getName(), className);
                    debugger.logdebug(Integer.toString(issuesInList.size()), className);
                }
            } catch (Exception e) {
                debugger.logdebug("Line 64", className);
                debugger.logdebug(e.getMessage(), className);
            }
        }

        if (issuesInList.size() > 0){
            debugger.logdebug("Full list number: "+Integer.toString(issuesInList.size()), className);
            for (Issue issue : issuesInList){
                if(issueTypeFinalList.indexOf(issue.getIssueType().getName()) == -1){
                    issueTypeFinalList.add(issue.getIssueType().getName());
                    timeTrack.add(Long.valueOf(0));
                    timeTrackPersonal.add(Long.valueOf(0));

                }
            }
            List<Issue> checkedIssues = new ArrayList<>();
            for (Issue issue : issuesInList){
                findTimeIssue(issue,issueTypeFinalList,timeTrack,checkedIssues, timeTrackPersonal);
            }

        } else {
            debugger.logdebug("No issues in list",className);
        }

        if (issuesInList.size() > 0) {
            finalText = "Total time spent:\r\n";
        } else{
            finalText = "Result set too large";
        }

        double hours;
        for(int i = 0; i < issueTypeFinalList.size(); i++){
            debugger.logdebug("Total time: "+String.valueOf(timeTrack.get(i)),className);
            hours = (timeTrack.get(i).doubleValue() / 3600);
            debugger.logdebug("Hours: "+String.valueOf(hours),className);
            String time = String.valueOf(df.format(hours));
            finalText = finalText + issueTypeFinalList.get(i)+": "+ time +"h \r\n";
        }
        if (issuesInList.size() > 0) {
            finalText = finalText + "\r\n\r\n";
            finalText = finalText + "Personal time spent for: " + user.getDisplayName() + "\r\n";
        }

        for(int i = 0; i < issueTypeFinalList.size(); i++){
            debugger.logdebug("Total personal time: "+String.valueOf(timeTrackPersonal.get(i)),className);
            hours = (timeTrackPersonal.get(i).doubleValue() / 3600);
            debugger.logdebug("Hours: "+String.valueOf(hours),className);
            String time = String.valueOf(df.format(hours));
            finalText = finalText + issueTypeFinalList.get(i)+": "+ time +"h \r\n";
        }

        return finalText;
    }

    private void findTimeIssue(Issue issue, List<String> issueTypeFinalList,List<Long> timeTrack,List<Issue> checkedIssues, List<Long> timeTrackPersonal){
        if(issue.getIssueType().getName().contains("Task")){
        Collection<Issue> linkedIssues = projectHelper.getIssueLinks(issue, "sub-task", user);
        for (Issue parentIssue : linkedIssues){
            if(parentIssue.getIssueType().getName().contains("Task")){
                findTimeIssue(parentIssue,issueTypeFinalList,timeTrack,checkedIssues,timeTrackPersonal);
            }
            if(!checkedIssues.contains(parentIssue)) {
                Long totalTime = parentIssue.getTimeSpent();
                if (totalTime != null) {
                    getPersonalTime(parentIssue,issueTypeFinalList,timeTrackPersonal);
                    debugger.logdebug("Issue time spent: " + String.valueOf(totalTime), className);
                    int index = issueTypeFinalList.indexOf(parentIssue.getIssueType().getName());
                    if (index > -1) {
                        timeTrack.set(index, Long.sum(timeTrack.get(index), totalTime));
                    } else {
                        debugger.logdebug("index was -1", className);
                    }
                }
                checkedIssues.add(parentIssue);
            }
        }}
        // Add the actual issue
        if(!checkedIssues.contains(issue)) {
            Long totalTime = issue.getTimeSpent();
            if (totalTime != null) {
                getPersonalTime(issue,issueTypeFinalList,timeTrackPersonal);
                debugger.logdebug("Issue time spent: " + String.valueOf(totalTime), className);
                int index = issueTypeFinalList.indexOf(issue.getIssueType().getName());
                if (index > -1) {
                    timeTrack.set(index, Long.sum(timeTrack.get(index), totalTime));
                } else {
                    debugger.logdebug("index was -1", className);
                }
            }
            checkedIssues.add(issue);
        }
    }

    private void getPersonalTime(Issue issue, List<String> issueTypeFinalList, List<Long> timeTrackPersonal){
        WorklogManager worklogManager = ComponentAccessor.getWorklogManager();
        List<Worklog> worklogs = worklogManager.getByIssue(issue);
        int index = issueTypeFinalList.indexOf(issue.getIssueType().getName());
        SendEmail classHelper = new SendEmail();
        ApplicationUser newuser;
        if(user.getUsername().contains("pdwyer")) {
            newuser = user;
        } else {
            newuser = user;
        }
        if (index > -1){
            for (Worklog worklog : worklogs){
                if((worklog.getAuthorObject() != null) & (newuser != null)) {
                    if (worklog.getAuthorObject().equals(newuser)) {
                        Long time = worklog.getTimeSpent();
                        debugger.logdebug("Personal time spent: " + String.valueOf(time), className);
                        timeTrackPersonal.set(index, Long.sum(timeTrackPersonal.get(index), time));
                    } else {
                        debugger.logdebug("The user isn't the logged in user", className);
                    }
                }
            }
        }
    }

}
