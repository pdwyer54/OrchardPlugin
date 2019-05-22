package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.project.VersionReleaseEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.springframework.stereotype.Component;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class TriggerRelease implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(TriggerRelease.class);
    private String className = this.getClass().getSimpleName();
    public static boolean isRW;
    private static Debug debugger = new Debug();

    @JiraImport
    private final EventPublisher eventPublisher;

    @Autowired
    public TriggerRelease(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @EventListener
    public void onVersionReleased(final VersionReleaseEvent event) {

            if (event.getVersion() != null) {
                ProjectHelper projectHelper = new ProjectHelper();
                BuildQuery queryBuilder = new BuildQuery();
                final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user
                Version version = event.getVersion(); // Get the version being released
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
                    BuildTemplate buildTemplate = new BuildTemplate();
                    buildTemplate.getTemplate(allIssues, project, version);
                }

            }

    }

    private boolean checkProject(String projectKey) { // Check the project to make sure we should even be doing this
        ProjectHelper projectHelper = new ProjectHelper();
        ArrayList<String> projectList=new ArrayList<String>();
        projectHelper.buildProjectList(projectList);
        return projectList.contains(projectKey);
    }

}
