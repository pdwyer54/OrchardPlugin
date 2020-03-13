package com.orchardsoft.plugin.OrchardPlugin.CreatedIssueListener;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.util.UserManager;
import com.orchardsoft.plugin.OrchardPlugin.Debug;

public class StoryPointAllocation {

    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private static final UserManager userManager = ComponentAccessor.getUserManager();
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();

    public StoryPointAllocation(){

    }

    public void CopiaPointAllocation(){

    }

    public void HarvestPointAllocation(){


    }

    public void ODEPointAllocation(){

    }

}
