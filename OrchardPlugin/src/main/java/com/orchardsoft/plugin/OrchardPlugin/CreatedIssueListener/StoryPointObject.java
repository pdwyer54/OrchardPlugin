package com.orchardsoft.plugin.OrchardPlugin.CreatedIssueListener;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.ProjectHelper;

import java.util.ArrayList;
import java.util.Collection;

public class StoryPointObject {

    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private static final ProjectHelper projectHelper = new ProjectHelper();

    public Issue epic;
    public Issue feature;
    public boolean isEpic;
    public Double StoryPointsTotal;
    public Double StoryPointsCompleted;
    public Double FeatureSPTotal;
    public Double FeatureSPComplete;
    public ArrayList<String> IssuesChecked;

    public StoryPointObject(Issue issue){


        CustomField epicLink = projectHelper.getCustomFieldObject("Epic Link");
        Object epicLinkValue;
        Issue epicLinkTicket = null;
        Issue featureTicket = null;
        boolean epicExists = false;
        epicLinkValue = issue.getCustomFieldValue(epicLink);
        if (epicLinkValue != null) {
            epicLinkTicket = (Issue) issue.getCustomFieldValue(epicLink);
            featureTicket = issue;
        } else if (issue.getIssueType().getName().contains("Epic")) {
            debugger.logdebug("This is an epic", className);
            epicLinkTicket = issue;
            epicExists = true;
        } else {
            // There is no epic link, check if it's a sub-task
            Issue parentIssue = projectHelper.getSubTaskParent(issue);
            if (parentIssue != null) {
                // Sub-task link exists, see if the parent has an epic link
                epicLinkValue = parentIssue.getCustomFieldValue(epicLink);
                if (epicLinkValue != null) {
                    epicLinkTicket = (Issue) issue.getCustomFieldValue(epicLink);
                }
            }
            if (epicLinkTicket == null) {
                debugger.logdebug("No epic could be found", className);
            }
        }

        epic = epicLinkTicket;
        feature = featureTicket;
        isEpic = epicExists;
        StoryPointsTotal = 0.0;
        StoryPointsCompleted = 0.0;
        FeatureSPTotal = 0.0;
        FeatureSPComplete = 0.0;
        IssuesChecked = new ArrayList<String>();
    }

    public StoryPointObject(Issue issue,String componentName){
        Issue componentTicket = null;
        Issue featureTicket = null;
        boolean componentExists = false;

        Collection<ProjectComponent> issueComponents= issue.getComponents();
        for (ProjectComponent component : issueComponents){
            if (component.getName().contains(componentName)){
                componentTicket = issue;
                componentExists = true;
                break;
            }
        }
        if (!componentExists){
            final ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user
            Collection<Issue> linkedIssues = projectHelper.getIssueLinks(issue, "blocks", currentUser);
            for(Issue linkedissue : linkedIssues){
                Collection<ProjectComponent> linkedIssueComponents= linkedissue.getComponents();
                for(ProjectComponent component : linkedIssueComponents){
                    if (component.getName().contains("Release")) {
                        componentTicket = linkedissue;
                        featureTicket = issue;
                        break;
                    }
                }

                if (componentTicket != null){
                    break;
                }
            }
        }

        epic = componentTicket;
        feature = featureTicket;
        isEpic = componentExists;
        StoryPointsTotal = 0.0;
        StoryPointsCompleted = 0.0;
        FeatureSPTotal = 0.0;
        FeatureSPComplete = 0.0;
        IssuesChecked = new ArrayList<String>();

    }


    public StoryPointObject(){
        epic = null;
        feature = null;
        isEpic = false;
        StoryPointsTotal = 0.0;
        StoryPointsCompleted = 0.0;
        FeatureSPTotal = 0.0;
        FeatureSPComplete = 0.0;
        IssuesChecked = new ArrayList<String>();
    }

    public void clearFeatureSP(){
        FeatureSPComplete = 0.0;
        FeatureSPTotal = 0.0;
    }

    public void clearOverallSP(){
        StoryPointsCompleted = 0.0;
        StoryPointsTotal = 0.0;
    }

    public String getStringFromDoubleFSPT(){
        return String.valueOf(FeatureSPTotal);
    }

    public String getStringFromDoubleSPT(){
        return String.valueOf(StoryPointsTotal);
    }

    public void addToIssuesChecked(Issue issue){
        IssuesChecked.add(issue.getKey());
    }

    public void clearIssuesChecked(){
        IssuesChecked.clear();
    }

    public boolean issueAlreadyChecked(Issue issue){
        if (IssuesChecked.indexOf(issue.getKey()) == -1){
            return false;
        } else {
            return true;
        }
    }

    public void applyStoryPointstoIssue(long type, Issue issue){
        CustomField CFStoryPointsTotal = projectHelper.getCustomFieldObject("Epic Story Points");
        CustomField CFStoryPointsCompleted = projectHelper.getCustomFieldObject("Epic Story Points Completed");
        CustomField CFStoryPointsPercent = projectHelper.getCustomFieldObject("Aha Story Points Percentage");

        Double total = 0.0;
        Double completed = 0.0;

        if(type == 1){ // Total
            total = StoryPointsTotal;
            completed = StoryPointsCompleted;
        } else if (type == 2){ // Feature
            total = FeatureSPTotal;
            completed = FeatureSPComplete;
        }

        projectHelper.addCustomFieldValue(issue, CFStoryPointsTotal, total);
        projectHelper.addCustomFieldValue(issue, CFStoryPointsCompleted, completed);
        projectHelper.addCustomFieldValue(issue, CFStoryPointsPercent, StoryPointPercentage(completed,total));
    }

    public Double StoryPointPercentage(Double completed,Double total){
        return (completed/total) * 100;
    }



    public void printOutInLog() {
        debugger.logdebug("", className);
        debugger.logdebug("", className);
        debugger.logdebug("", className);
    }
}
