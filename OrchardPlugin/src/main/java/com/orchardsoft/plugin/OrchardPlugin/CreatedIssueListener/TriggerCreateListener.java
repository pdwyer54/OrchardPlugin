package com.orchardsoft.plugin.OrchardPlugin.CreatedIssueListener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ofbiz.GenericValueUtils;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.ProjectHelper;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.SendEmail;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.springframework.stereotype.Component;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Component
public class TriggerCreateListener implements InitializingBean, DisposableBean {

    private String className = this.getClass().getSimpleName();
    private static Debug debugger = new Debug();
    private static final UserManager userManager = ComponentAccessor.getUserManager();
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();

    @JiraImport
    private final EventPublisher eventPublisher;

    @Autowired
    public TriggerCreateListener(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Called when the plugin is being disabled or removed.
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    /**
     * Called when the plugin has been enabled.
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @EventListener
    public void onIssueEvent(final IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();
        Project project = issue.getProjectObject();
        ProjectHelper projectHelper = new ProjectHelper();
        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID))  {
            // Only fire on triggered events=
            //debugger.logdebug("Issue created in project: " + project.getKey(), className);
            if (project.getKey().contains("NRFM")) { // This is the project we will be monitoring
                debugger.logdebug("IssueEvent has been fired in NRFM", className);
                if (issue.getSummary().matches("^Test .*")) { // Has to match what we expect

                    // All the info we need is in the description seperated by carriage returns, split based on that
                    String fullText = issue.getDescription();
                    String[] lines = fullText.split("\\r?\\n");

                    ParseTicket parseTicket = new ParseTicket();

                    Integer size = lines.length;

                    if (size > 0) {

                        // Check to make sure we have 6 "fields"
                        if (size != 6) {
                            debugger.logdebug("There are " + size.toString() + " lines in the split array", className);
                        }

                        // Build the ticket info for each item, parsing them correctly.
                        TicketInfoObject ticketInfo = new TicketInfoObject(parseTicket.parseLine(lines[0]), parseTicket.parseLine(lines[1]), parseTicket.parseLine(lines[2]), parseTicket.parseLine(lines[3]), parseTicket.parseLine(lines[4]), parseTicket.parseLine(lines[5]));

                        // Just spits out what we got back, mainly for checking
                        ticketInfo.debugTicketInfo();
                        SendEmail emailhandler = new SendEmail();
                        CustomField ClientID = null;
                        String ClientIDString = "";
                        Object ClientIDObject = null;
                        // Validate the linked tickets, if they exist
                        if (parseTicket.validateTickets(ticketInfo.specReviewTicket, ticketInfo.PmDevTicket, ticketInfo.PmFIATicket)) {
                            // So we have validated that all the link tickets exist, we need to grab a custom field from the PMDevTicket, the ClientID to put in our ticket
                            Issue PmDevIssue = parseTicket.getIssue(ticketInfo.getPmDevTicket());
                            ClientID = projectHelper.getCustomFieldObject("Client ID");
                                if (ClientID != null){
                                    ClientIDObject = PmDevIssue.getCustomFieldValue(ClientID);
                                    ClientIDString = ClientIDObject.toString();
                                }

                            String summary = issue.getSummary();
                            if (parseTicket.validateLine(ticketInfo.getType(ticketInfo.getEmailUserName()), ticketInfo.getEmailUserName())) { // Validate the email
                                ApplicationUser user = userManager.getUserByNameEvenWhenUnknown(ticketInfo.getEmailUserName());
                                JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
                                if (user != null) {
                                    jiraAuthenticationContext.setLoggedInUser(user);
                                }
                                if (parseTicket.validateLine(ticketInfo.getType(ticketInfo.getProduct()), ticketInfo.getProduct())) { // Lastly validate the product, (this should never not validate)

                                    // We need to grab all the info for the new ticket we are creating
                                    Long projectID = ticketInfo.getProjectID();
                                    String newDescription = issue.getDescription().replace("*", ":"); // The description will just be the old description with the * replaced with : so it looks better
                                    String issueTypeID = "12201"; // New instrument interface
                                    String priorityTypeID = "3";

                                    // Create the issue
                                    Issue createdIssue = projectHelper.createIssue(user, projectID, summary, newDescription, issueTypeID, "ODE_Team", priorityTypeID);
                                    if (createdIssue != null) {

                                        // Add all the links, if any of them fail we will delete the new ticket and tell the user
                                        // There is no reason why it shouldn't link properly as we have already validated that the tickets exist
                                        Boolean success = true;
                                        success = projectHelper.addIssueLink(createdIssue, parseTicket.getIssue(ticketInfo.getSpecReviewTicket()), "relates", user);
                                        if (success) {
                                            success = projectHelper.addIssueLink(createdIssue, parseTicket.getIssue(ticketInfo.getPmDevTicket()), "relates", user);
                                        }
                                        if (success) {
                                            success = projectHelper.addIssueLink(createdIssue, parseTicket.getIssue(ticketInfo.getPmFIATicket()), "relates", user);
                                        }
                                        if (success) {
                                            debugger.logdebug("The ticket was successful in creation",className);
                                            // Send success email
                                            String subject = "Instrument Interface Development Request successfully received";
                                            String body = "Your request for an Instrument Interface has been successfully received. <br> "+ createdIssue.getKey() + " was created.";
                                            projectHelper.addComponent(createdIssue,"Release",ticketInfo.getProjectID()); // We have success, last thing we need to do is add this component to the ticket
                                            if (ClientIDString != ""){
                                                debugger.logdebug("Client ID: "+ClientIDString,className);
                                                projectHelper.addCustomFieldValue(createdIssue,ClientID,ClientIDObject);
                                            }

                                            emailhandler.sendTheEmail(ticketInfo.getEmailAddress(), subject, body);

                                            // Various ways this ticket can fail
                                        } else {
                                            // The ticket was created but then there was an issue in linking so we need to delete it
                                            try {
                                                issueManager.deleteIssueNoEvent(createdIssue);
                                            } catch (RemoveException e) {
                                                debugger.logdebug("This is thrown when something, and I quote, \"goes horribly wrong.\"", className);
                                            }

                                            String subject = "Instrument Interface Development Request failed";
                                            String body = "The ticket failed to create the links, it was not created, please review your form input and try again: <br>" +
                                                    ticketInfo.getStringTicketInfo();
                                            emailhandler.sendTheEmail(ticketInfo.getEmailAddress(), subject, body);
                                        }

                                    } else {
                                        debugger.logdebug("There was an issue where the ticket was not created, we will not be adding links", className);
                                        String subject = "Instrument Interface Development Request failed";
                                        String body = "There was an unknown issue with creating the ticket, please review the form input and try again: <br>" +
                                                ticketInfo.getStringTicketInfo();

                                        emailhandler.sendTheEmail(ticketInfo.getEmailAddress(), subject, body);
                                    }

                                } else {
                                    debugger.logdebug("The product was not valid: " + ticketInfo.getProduct(), className);
                                }

                                if (user != null) {
                                    jiraAuthenticationContext.setLoggedInUser(null);
                                }
                            } else {
                                debugger.logdebug("The email address was not valid.", className);
                                String subject = "Instrument Interface Development Request failed";
                                String body = "The ticket failed because the email was invalid, it was not created, please review the input and forward it to who needs it: <br>" +
                                        ticketInfo.getStringTicketInfo();
                                emailhandler.sendTheEmail(emailhandler.getUserEmail("dhardwick"), subject, body);
                            }

                        } else {
                            // Send the email to the user saying the ticket numbers aren't valid
                            debugger.logdebug("There was an issue with the link ticket numbers not being valid, not creating the ticket", className);
                            String subject = "Instrument Interface Development Request failed";
                            String body = "One or more of the linked ticket numbers were not correct, please review the form input and try again: <br>" +
                                    ticketInfo.getStringTicketInfo();
                            if (parseTicket.validateLine(ticketInfo.getType(ticketInfo.getEmailUserName()), ticketInfo.getEmailUserName())) {
                                emailhandler.sendTheEmail(ticketInfo.getEmailAddress(), subject, body);
                            } else { // The email was also incorrect so we need to send it to the backup person
                                emailhandler.sendTheEmail(emailhandler.getUserEmail("dhardwick"), subject, body);
                            }

                        }

                    }

                    // Delete the intial issue. Please note this is gonna be weird because we are in the middle of the creation event
                    // So if will run this code and then "delete" the issue but because it's in the creation event it just never gets created, so JIRA may think this is an error
                    // It will still use up a key for the issue.
/*                    try {
                        issueManager.deleteIssueNoEvent(issue);
                    } catch (RemoveException e) {
                        debugger.logdebug("This is thrown when something, and I quote, \"goes horribly wrong.\"", className);
                    }*/
                } else {
                    debugger.logdebug("Issue summary does not start with Test: " + issue.getSummary(), className);
                }
            }

        } else if (eventTypeId.equals(EventType.ISSUE_ASSIGNED_ID)) {
            if (project.getKey().contains("NRFM")) {
                if (issue.getSummary().matches("^Test .*")) {
                    try {
                        issueManager.deleteIssueNoEvent(issue);
                    } catch (RemoveException e) {
                        debugger.logdebug("This is thrown when something, and I quote, \"goes horribly wrong.\"", className);
                    }
                }
            }
        } else if (eventTypeId.equals(EventType.ISSUE_WORKLOGGED_ID)) {
            StoryPointAllocation storyPointAllocation = new StoryPointAllocation();
            if (project.getKey().contains("COPIA")) {
                storyPointAllocation.CopiaPointAllocation(issue);
            } else if (project.getKey().contains("HARVEST")) {
                storyPointAllocation.HarvestPointAllocation(issue);
            } else if (project.getKey().contains("ODE")){
                storyPointAllocation.ODEPointAllocation(issue);
            }
        }
    }
}

