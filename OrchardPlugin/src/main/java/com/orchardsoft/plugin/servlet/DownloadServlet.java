// Written by Phil Dwyer
// PRD 11/26/18 DEV-557
//
// This method is called from javascript on the client and it basically runs the release notes so we can pass back the html text
//



package com.orchardsoft.plugin.servlet;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.BuildQuery;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.DownloadHelper;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.ProjectHelper;
import com.orchardsoft.plugin.OrchardPlugin.Reporting.ReportingMain;

public class DownloadServlet extends HttpServlet{
    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private static final ProjectHelper projectHelper = new ProjectHelper();
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();
    private static final VersionManager versionManager = ComponentAccessor.getVersionManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean isDownload = true;
        isDownload = Boolean.parseBoolean(req.getParameter("isDownload"));
        boolean createReport = true;
        createReport = Boolean.parseBoolean(req.getParameter("createReport"));
        if (isDownload) {
            String version = req.getParameter("version");
            String projectKey = req.getParameter("project");
            String text = "";
            DownloadHelper helper = new DownloadHelper();
            debugger.logdebug("Running download helper", className);
            text = helper.versionRelease(null, version, projectKey);
            if (text == "") {
                debugger.logdebug("text was blank", className);
            }

            resp.setContentType("text/html");
            resp.getWriter().write(text);
        }
        else if(createReport) { // This creates a report that will total time based upon what the release was given
            String filter = req.getParameter("filter");
            if (filter != ""){
                String text = "";
                ReportingMain report = new ReportingMain();
                try{
                    text = report.doReport(filter);
                } catch (JqlParseException e){
                    debugger.logdebug("Failure in JQL Parse Exception", className);
                    debugger.logdebug(e.getMessage(), className);
                }

                resp.setContentType("text");
                resp.getWriter().write(text);
            }

        } else { // Release download, create an epic here for the stuff
            String oldVersion = req.getParameter("oldVersion"); // Hotfix X
            String newVersion = req.getParameter("newVersion"); // 11.X.X
            String projectKey = req.getParameter("project");
            debugger.logdebug("Servlet hit", className);
            debugger.logdebug("Old version: " + oldVersion, className);
            debugger.logdebug("New version: " + newVersion, className);
            debugger.logdebug("Project key: " + projectKey, className);

            boolean majorVersion = false;
            if (newVersion.contains("Hotfix Next")) {
                majorVersion = true;
            }

            // The release has already occured by now so when we query the new version it's actually the released version
            Query query;
            BuildQuery queryBuilder = new BuildQuery();
            query = queryBuilder.JQLBuilderBuild(projectKey, newVersion, "", null, "Epic");
            SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
            SearchResults searchResults;
            List<Issue> issuesInList = new ArrayList<>();
            final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user

            MessageSet errorMessages = searchService.validateQuery(user, query);
            if (errorMessages.hasAnyErrors()) {
                // Add error logging
                debugger.logdebug("Failure in Query", className);
            } else {
                try {
                    searchResults = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                    issuesInList = searchResults.getResults();

                } catch (Exception e) {
                    debugger.logdebug(e.getMessage(), className);
                }
            }

            if (issuesInList.isEmpty()) {
                debugger.logdebug("List is empty", className);
            }

            // There realistically should be one epic issue
            Issue epicIssue = null;
            if (!(issuesInList.isEmpty())) {
                for (Issue issue : issuesInList) {
                    if (issue.getSummary().contains(oldVersion)) {
                        epicIssue = issue;
                        break;
                    }
                }
            }
            Version newEpicFixVersion = null;
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project project = projectManager.getProjectByCurrentKey(projectKey);
            String IDType = "10018"; // Epic
            Collection<Version> projectVersions = versionManager.getVersions(project);

            /**
            if (!(epicIssue.getFixVersions().isEmpty())) {
                Collection<Version> epicVersions = epicIssue.getFixVersions();
                for (Version ver : epicVersions) {
                    newEpicFixVersion = ver;
                    break;
                }
            }
             **/

            if (newEpicFixVersion == null) {

                for (Version ver : projectVersions) {
                    if (ver.getName().contains(oldVersion)) {
                        newEpicFixVersion = ver;
                        debugger.logdebug("Version found: " + newEpicFixVersion.getName(), className);
                        break;
                    }
                }
            }

            if ((epicIssue == null) & (!majorVersion)) {
                debugger.logdebug("Creating epicIssue", className);
                epicIssue = projectHelper.createIssue(user, project.getId(), oldVersion, "Hotfix Epic old", IDType, "Development_Team", "3");
            }

            if (epicIssue != null) {
                debugger.logdebug("Epic Issue: " + epicIssue.getKey(), className);
                issuesInList.clear();
                query = queryBuilder.JQLReleaseQueryBuilderNotEpic(projectKey, newVersion);
                errorMessages = searchService.validateQuery(user, query);

                if (errorMessages.hasAnyErrors()) {
                    // Add error logging
                    debugger.logdebug("Failure in Query", className);
                } else {
                    try {
                        searchResults = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                        issuesInList = searchResults.getResults();

                    } catch (Exception e) {
                        debugger.logdebug(e.getMessage(), className);
                    }
                }
                Collection<Version> listofVersions = new ArrayList<>();

                Issue newEpic = projectHelper.createIssue(user, project.getId(), oldVersion, "Hotfix Epic", IDType, "Development_Team", "3");
                projectHelper.setSummary(issueManager.getIssueObject(epicIssue.getKey()), newVersion);
                CustomField customField2 = projectHelper.getCustomFieldObject("Epic Name");
                if (customField2 != null) {
                    projectHelper.addCustomFieldValue(issueManager.getIssueObject(epicIssue.getKey()), customField2, newVersion);
                }

                if (!(issuesInList.isEmpty())) {
                    CustomField customField = null;
                    Collection<Issue> epicIssues = projectHelper.getIssuesInEpic(epicIssue);
                    customField = projectHelper.getCustomFieldObject("Epic Link");
                    boolean foundVersion = false;
                    if (customField != null) {
                        for (Issue issue : issuesInList) {
                            if (!epicIssues.contains(issue)) {
                                debugger.logdebug("Issue not in epic, adding: " + issue.getKey(), className);
                                projectHelper.addCustomFieldValue(issueManager.getIssueObject(issue.getKey()), customField, issueManager.getIssueObject(epicIssue.getKey()));
                            }
                            if (!(foundVersion)) {
                                debugger.logdebug("Found fix version for epic", className);
                                listofVersions = versionManager.getFixVersionsFor(issue);
                                foundVersion = true;
                            }
                        }
                    } else {
                        debugger.logdebug("Customfield was null", className);
                    }

                } else {
                    debugger.logdebug("Release is empty of non-epic issues", className);
                }

                if (epicIssue.getFixVersions().isEmpty()) {
                    if (!(listofVersions.isEmpty())) {
                        projectHelper.setFixVersion(issueManager.getIssueObject(epicIssue.getKey()), listofVersions);
                    }
                }

                if ((newEpicFixVersion != null) & (newEpic != null)) {
                    debugger.logdebug("Attempting to set version for: " + newEpic.getKey(), className);
                    Collection<Version> versions = new ArrayList<>();
                    versions.add(newEpicFixVersion);
                    projectHelper.setFixVersion(issueManager.getIssueObject(newEpic.getKey()), versions);
                } else {
                    debugger.logdebug("No new version or epic", className);
                }

            } else {
                if (majorVersion) {
                    Issue newEpic = projectHelper.createIssue(user, project.getId(), newVersion, "Hotfix Epic", IDType, "Development_Team", "3");

                    Version hotfixVersion = null;
                    for (Version ver : projectVersions) {
                        if (ver.getName().contains(newVersion)) {
                            hotfixVersion = ver;
                            debugger.logdebug("Version found: " + hotfixVersion.getName(), className);
                            break;
                        }
                    }

                    if ((hotfixVersion != null) & (newEpic != null)) {
                        debugger.logdebug("Attempting to set version for: " + newEpic.getKey(), className);
                        Collection<Version> versions = new ArrayList<>();
                        versions.add(hotfixVersion);
                        projectHelper.setFixVersion(issueManager.getIssueObject(newEpic.getKey()), versions);

                    } else {
                        debugger.logdebug("No epic issue was found or could be created", className);
                    }
                }

            }
        }
    }

}
