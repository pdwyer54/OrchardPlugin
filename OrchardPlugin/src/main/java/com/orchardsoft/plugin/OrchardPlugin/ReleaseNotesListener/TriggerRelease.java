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
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.DownloadHelper;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class TriggerRelease implements InitializingBean, DisposableBean {

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

            DownloadHelper run = new DownloadHelper();
            String junk = run.versionRelease(event.getVersion(),"","");

    }

}
