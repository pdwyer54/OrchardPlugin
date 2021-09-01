package com.orchardsoft.plugin.OrchardPlugin.CreatedIssueListener;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.issue.customfields.option.*;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.ProjectHelper;


import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StoryPointAllocation {

    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private static final UserManager userManager = ComponentAccessor.getUserManager();
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();
    private static final ProjectHelper projectHelper = new ProjectHelper();
    private static final ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user

    public StoryPointAllocation(){

    }

    private Double GetStoryPointsFromIssue(Issue issue){
        Double storyPoints = 0.0;
        CustomField CFStoryPoints = projectHelper.getCustomFieldObject("Story Points");
        Object storyPointObj = null;

        storyPointObj = issue.getCustomFieldValue(CFStoryPoints);
        if (storyPointObj != null) {
            storyPoints = Double.valueOf(storyPointObj.toString());
        }
        return storyPoints;
    }

    private Boolean checkTestingTask(Issue issue){
        Boolean isTestingTask;
        IssueType issueType = issue.getIssueType();

        if(issueType.getName().contains("Testing Task")){
            isTestingTask = true;
        } else {
            isTestingTask = false;
        }
        return isTestingTask;
    }

    private Double GetTestStoryPointsFromComplexity(Issue issue){
        Double storyPoints = 0.0;
        CustomField CFStoryPoints = projectHelper.getCustomFieldObject("Testing Complexity");
        debugger.logdebug("Calling GetTestStoryPointsFromComplexity",className);

        if(CFStoryPoints == null){
            debugger.logdebug("Testing Complexity custom field was null?",className);
        } else {
            debugger.logdebug("Issue for testing complex: "+issue.getKey(),className);
        }
        Object storyPointObj = issue.getCustomFieldValue(CFStoryPoints);
        if (storyPointObj != null) {

            debugger.logdebug("Attempting to get Testing Complexity Val",className);
            debugger.logdebug(storyPointObj.toString(),className);
            storyPoints = Double.valueOf(storyPointObj.toString());
        } else {
            debugger.logdebug("storyPointObj was null",className);
        }



        return storyPoints;
    }


    private Double addStoryPoints(Double total, Double points){
      Double finalTotal = total + points;
      return finalTotal;
    }

    public void HarvestPointAllocation(Issue issue){
        debugger.logdebug("Starting Harvest Point Allocation",className);
        StoryPointObject storyPoints = new StoryPointObject(issue);
        if ((storyPoints.epic != null) & (storyPoints.isEpic)){
            debugger.logdebug("Epic Issue: " + storyPoints.epic.getKey(), className);
            Collection<Issue> IssuesinCollection = projectHelper.getIssuesInEpic(storyPoints.epic);
            Double storyPointsAmount = 0.0;
            Double testingStoryPointsAmount = 0.0;
            ArrayList<String> completedIssues = new ArrayList<String>();
            for (Issue subIssue : IssuesinCollection) {
                storyPoints.clearFeatureSP();
                debugger.logdebug("Epic Sub-Issue: " + subIssue.getKey(), className);
                storyPointsAmount = GetStoryPointsFromIssue(subIssue);
                testingStoryPointsAmount = GetTestStoryPointsFromComplexity(subIssue);
                storyPoints.FeatureSPTotal = storyPointsAmount;
                storyPoints.TestStoryPointsTotal = testingStoryPointsAmount;
                if (projectHelper.isResolved(subIssue)) {
                    storyPoints.FeatureSPComplete = storyPointsAmount;
                    storyPoints.TestStoryPointsCompleted = testingStoryPointsAmount;
                }


                if (storyPoints.feature != null) {
                    if (subIssue.getKey() == storyPoints.feature.getKey() & (completedIssues.indexOf(subIssue) < 0)) {
                        storyPoints.applyStoryPointstoIssue(2,subIssue,true);

                    }
                } else if (storyPoints.isEpic) {
                    debugger.logdebug("Index of " + String.valueOf(completedIssues.indexOf(subIssue.getKey())), className);
                    if ((completedIssues.indexOf(subIssue.getKey()) == -1)) {
                        if (storyPoints.FeatureSPTotal > 0.0) {
                            debugger.logdebug("Issue being updated: " + subIssue.getKey(), className);
                            debugger.logdebug("FeatureSPTotal: " + storyPoints.FeatureSPTotal.toString(), className);
                            debugger.logdebug("FeatureSPComplete: " + storyPoints.FeatureSPComplete.toString(), className);
                            storyPoints.applyStoryPointstoIssue(2,subIssue,true);
                        }
                    }
                }
                if (completedIssues.indexOf(subIssue.getKey()) == -1) {
                    //storyPoints.printOutInLog();
                    storyPoints.StoryPointsTotal = addStoryPoints(storyPoints.StoryPointsTotal,storyPoints.FeatureSPTotal);
                    storyPoints.StoryPointsCompleted = addStoryPoints(storyPoints.StoryPointsCompleted,storyPoints.FeatureSPComplete);
                    storyPoints.StoryPointsTotal = addStoryPoints(storyPoints.StoryPointsTotal,storyPoints.TestStoryPointsTotal);
                    storyPoints.StoryPointsCompleted = addStoryPoints(storyPoints.StoryPointsCompleted,storyPoints.TestStoryPointsCompleted);
                    storyPoints.StoryTestPoints = addStoryPoints(storyPoints.StoryTestPoints,storyPoints.TestStoryPointsTotal);
                    storyPoints.StoryTestPointsCompleted = addStoryPoints(storyPoints.StoryTestPointsCompleted,storyPoints.TestStoryPointsCompleted);
                    debugger.logdebug("CFStoryPointsTotal: " + String.valueOf(storyPoints.StoryPointsTotal), className);
                    debugger.logdebug("CFStoryPointsCompleted:" + String.valueOf(storyPoints.StoryPointsCompleted), className);

                }



                if (completedIssues.indexOf(subIssue.getKey()) == -1) {
                    completedIssues.add(subIssue.getKey());
                }
            }

            debugger.logdebug("Adding everything up and putting in the epic", className);
            debugger.logdebug("CFStoryPointsTotal: " + String.valueOf(storyPoints.StoryPointsTotal), className);
            debugger.logdebug("CFStoryPointsCompleted:" + String.valueOf(storyPoints.StoryPointsCompleted), className);
            storyPoints.applyStoryPointstoIssue(1,storyPoints.epic,true);


        }

    }

    public void CopiaPointAllocation(Issue issue) {
        StoryPointObject storyPoints = new StoryPointObject(issue);


        if ((storyPoints.epic != null) & (storyPoints.isEpic)){
            CustomField CFStoryPoints = projectHelper.getCustomFieldObject("Story Points");
            debugger.logdebug("Epic Issue: " + storyPoints.epic.getKey(), className);
            Collection<Issue> IssuesinCollection = projectHelper.getIssuesInEpic(storyPoints.epic);
            ArrayList<String> completedIssues = new ArrayList<String>();
            // Loop through all the sub-tasks to get how many story points are on each one
            for (Issue subIssue : IssuesinCollection) {
                storyPoints.clearFeatureSP();
                debugger.logdebug("Epic Sub-Issue: " + subIssue.getKey(), className);
                Collection<Issue> linkedIssues = projectHelper.getIssueLinks(subIssue, "sub-task", currentUser);
                Double storyPointsAmount = 0.0;
                for (Issue linkedSubTasks : linkedIssues) {
                    debugger.logdebug("Feature Sub-Task: " + linkedSubTasks.getKey(), className);
                        storyPointsAmount = GetStoryPointsFromIssue(linkedSubTasks);
                        if (storyPointsAmount > 0.0) {
                            debugger.logdebug("Issue " + linkedSubTasks.getKey() + " was added to list", className);
                            completedIssues.add(linkedSubTasks.getKey());
                            storyPoints.FeatureSPTotal = addStoryPoints(storyPoints.FeatureSPTotal, storyPointsAmount);
                            if(checkTestingTask(linkedSubTasks)){
                                storyPoints.TestStoryPointsTotal = addStoryPoints(storyPoints.TestStoryPointsTotal, storyPointsAmount);
                            }
                            debugger.logdebug("Story Points Total: " + storyPoints.FeatureSPTotal.toString(), className);
                            if (projectHelper.isResolved(linkedSubTasks)) {
                                storyPoints.FeatureSPComplete = addStoryPoints(storyPoints.FeatureSPComplete, storyPointsAmount);
                                if(checkTestingTask(linkedSubTasks)) {
                                    storyPoints.TestStoryPointsCompleted = addStoryPoints(storyPoints.TestStoryPointsCompleted, storyPointsAmount);
                                }
                                debugger.logdebug("Story Points Completed: " + storyPoints.FeatureSPComplete.toString(), className);
                            }
                        }

                }
                if (storyPoints.feature != null) {
                    if (subIssue.getKey() == storyPoints.feature.getKey() & (completedIssues.indexOf(subIssue) < 0)) {
                        storyPoints.applyStoryPointstoIssue(2,subIssue,true);
                    }
                } else if (storyPoints.isEpic) {
                    debugger.logdebug("Index of " + String.valueOf(completedIssues.indexOf(subIssue.getKey())), className);
                    if ((completedIssues.indexOf(subIssue.getKey()) == -1)) {
                        if (storyPoints.FeatureSPTotal > 0.0) {
                            debugger.logdebug("Issue being updated: " + subIssue.getKey(), className);
                            debugger.logdebug("FeatureSPTotal: " + storyPoints.FeatureSPTotal.toString(), className);
                            debugger.logdebug("FeatureSPComplete: " + storyPoints.FeatureSPComplete.toString(), className);
                            storyPoints.applyStoryPointstoIssue(2,subIssue,true);
                        }
                    }
                }

                if (completedIssues.indexOf(subIssue.getKey()) == -1) {
                    storyPoints.StoryPointsTotal = addStoryPoints(storyPoints.StoryPointsTotal,storyPoints.FeatureSPTotal);
                    storyPoints.StoryPointsCompleted = addStoryPoints(storyPoints.StoryPointsCompleted,storyPoints.FeatureSPComplete);
                    storyPoints.StoryPointsTotal = addStoryPoints(storyPoints.StoryPointsTotal,storyPoints.TestStoryPointsTotal);
                    storyPoints.StoryPointsCompleted = addStoryPoints(storyPoints.StoryPointsCompleted,storyPoints.TestStoryPointsCompleted);
                    storyPoints.StoryTestPoints = addStoryPoints(storyPoints.StoryTestPoints,storyPoints.TestStoryPointsTotal);
                    storyPoints.StoryTestPointsCompleted = addStoryPoints(storyPoints.StoryTestPointsCompleted,storyPoints.TestStoryPointsCompleted);
                    debugger.logdebug("CFStoryPointsTotal: " + String.valueOf(storyPoints.StoryPointsTotal), className);
                    debugger.logdebug("CFStoryPointsCompleted:" + String.valueOf(storyPoints.StoryPointsCompleted), className);
                }
                // We have gone through all the sub tasks now add the feature amount as well
                debugger.logdebug("Checking story points of the feature", className);

                if (completedIssues.indexOf(subIssue.getKey()) == -1) {
                    storyPointsAmount =GetStoryPointsFromIssue(subIssue);
                    if (storyPointsAmount > 0.0) {
                        storyPoints.StoryPointsTotal = addStoryPoints(storyPoints.StoryPointsTotal,storyPointsAmount);
                        if (projectHelper.isResolved(subIssue)) {
                            storyPoints.StoryPointsCompleted = addStoryPoints(storyPoints.StoryPointsCompleted,storyPointsAmount);
                        }
                        debugger.logdebug("CFStoryPointsTotal: " + String.valueOf(storyPoints.StoryPointsTotal), className);
                        debugger.logdebug("CFStoryPointsCompleted:" + String.valueOf(storyPoints.StoryPointsCompleted), className);
                    }
                }
                if (completedIssues.indexOf(subIssue.getKey()) == -1) {
                    completedIssues.add(subIssue.getKey());
                }
            }
            debugger.logdebug("Adding everything up and putting in the epic", className);
            debugger.logdebug("CFStoryPointsTotal: " + String.valueOf(storyPoints.StoryPointsTotal), className);
            debugger.logdebug("CFStoryPointsCompleted:" + String.valueOf(storyPoints.StoryPointsCompleted), className);
            storyPoints.applyStoryPointstoIssue(1,storyPoints.epic,true);
        }
    }

    public void ODEPointAllocation(Issue issue){
        StoryPointObject storyPoints = new StoryPointObject(issue,"Release");
        // storyPoints.isEpic is component here
        // storyPoints.epic is issue with Release component on it

        if ((storyPoints.epic != null) & (storyPoints.isEpic)){
            debugger.logdebug("Component Issue: " + storyPoints.epic.getKey(), className);
            Collection<Issue> linkedIssues = projectHelper.getIssueLinks(storyPoints.epic, "is blocked by", currentUser);
            Double storyPointsAmount = 0.0;

            if (linkedIssues.isEmpty()){
                debugger.logdebug("No linked issues",className);
            } else {
                debugger.logdebug(String.valueOf(linkedIssues.size())+" number of linked issues",className);
            }

            for (Issue linkedIssue : linkedIssues){
                if(linkedIssue.getProjectObject().getKey().contains("ODE")) {
                    debugger.logdebug("Linked blocked by issue: " + linkedIssue.getKey(), className);
                    StoryPointObject storyPointObject = new StoryPointObject();
                    storyPointObject.addToIssuesChecked(issue); // Always add the original issue
                    storyPointsAmount = GetStoryPointsFromIssue(linkedIssue);
                    storyPointObject = ODEsearchChildIssues(linkedIssue, storyPointObject);
                    if (!storyPointObject.issueAlreadyChecked(linkedIssue)) {
                        storyPointObject.FeatureSPTotal = addStoryPoints(storyPointObject.FeatureSPTotal, storyPointsAmount);
                        if (projectHelper.isResolved(linkedIssue)) {
                            storyPointObject.FeatureSPComplete = addStoryPoints(storyPointObject.FeatureSPComplete, storyPointsAmount);
                        }
                        storyPointObject.addToIssuesChecked(linkedIssue);
                    }

                    debugger.logdebug("Adding to SPTotal", className);
                    debugger.logdebug("StoryPointsTotal: " + storyPoints.getStringFromDoubleSPT(), className);
                    storyPoints.StoryPointsTotal = addStoryPoints(storyPoints.StoryPointsTotal, storyPointObject.FeatureSPTotal);
                    storyPoints.StoryPointsCompleted = addStoryPoints(storyPoints.StoryPointsCompleted, storyPointObject.FeatureSPComplete);
                    debugger.logdebug("StoryPointsTotal: " + storyPoints.getStringFromDoubleSPT(), className);
                }
            }

            debugger.logdebug("Adding everything up and putting in the epic", className);
            debugger.logdebug("CFStoryPointsTotal: " + String.valueOf(storyPoints.StoryPointsTotal), className);
            debugger.logdebug("CFStoryPointsCompleted:" + String.valueOf(storyPoints.StoryPointsCompleted), className);
            storyPoints.applyStoryPointstoIssue(1,storyPoints.epic,false);

        }
    }

    private StoryPointObject ODEsearchChildIssues(Issue issue,StoryPointObject storyPointObject){
        if(issue.getProjectObject().getKey().contains("ODE")) {
            debugger.logdebug("Calling ODEsearchChildIssues", className);
            Double storyPointsAmount = 0.0;
            if (!storyPointObject.issueAlreadyChecked(issue)) {
                debugger.logdebug("Adding the parent issue: " + issue.getKey(), className);
                storyPointsAmount = GetStoryPointsFromIssue(issue);
                debugger.logdebug("storyPointsAmount: " + String.valueOf(storyPointsAmount), className);
                storyPointObject.FeatureSPTotal = addStoryPoints(storyPointObject.FeatureSPTotal, storyPointsAmount);
                if (projectHelper.isResolved(issue)) {
                    storyPointObject.FeatureSPComplete = addStoryPoints(storyPointObject.FeatureSPComplete, storyPointsAmount);
                }
                storyPointObject.addToIssuesChecked(issue);
            }
            Collection<Issue> linkedIssues = projectHelper.getIssueLinks(issue, "is blocked by", currentUser);
            Collection<Issue> linkedIssue2 = projectHelper.getIssueLinks(issue, "blocks", currentUser);
            linkedIssues.addAll(linkedIssue2);
            for (Issue linkedIssue : linkedIssues) {
                if (!storyPointObject.issueAlreadyChecked(linkedIssue)) {
                    debugger.logdebug("Checking for linked issues in: " + linkedIssue.getKey(), className);
                    storyPointObject = ODEsearchChildIssues(linkedIssue, storyPointObject);
                }
            }

            debugger.logdebug("StorypointObject has: " + storyPointObject.getStringFromDoubleFSPT(), className);
        }
        return storyPointObject;
    }

}
