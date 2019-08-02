// Written by Phil Dwyer
// Class that handles everything email

package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

//internal imports

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.Email;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.queue.MailQueueItem;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.mail.server.SMTPMailServer;
import com.orchardsoft.plugin.OrchardPlugin.Debug;

public class SendEmail {

    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();
    private static final UserManager userManager = ComponentAccessor.getUserManager();

    public SendEmail() {

    }

    public String gatherEmailList(Project project, String user) {
        // Builds a list of emails to send to, currently this doesn't do anything but build the list for who clicked release. This is so we can fix something if it goes wrong
        // so it doesn't go out to the entire company right away

        String emailList = "";
        String projectKey = project.getKey();

        if(debugger.isTest) {
            projectKey = "Self";
        }


        if (projectKey.contains("HARVEST")) {
           emailList = getUserEmail("orchard-all");

        } else if (projectKey.contains("COPIA")) {
            emailList = getUserEmail("orchard-all");

        } else if (projectKey.contains("IE")) {
            emailList = getUserEmail("orchard-all");

        } else if (projectKey.contains("MAPPER")) {
            emailList = emailList + getUserEmail("orchard-dev")+ ",";
            emailList = emailList + getUserEmail("jeickhoff");

        } else if (projectKey.contains("OC")) {
            emailList = getUserEmail("orchard-all");

        } else if (projectKey.contains("ODE")) {
            emailList = getUserEmail("orchard-all");

        } else if (projectKey.contains("OT")) {
            emailList = emailList + getUserEmail("orchard-dev") + ",";
            emailList = emailList + getUserEmail("documentation");

        } else if (projectKey.contains("PS")) {
            // No email list here
            // Should never happen, send it to me
            projectKey = getUserEmail("pdwyer");

        } else if (projectKey.contains("WinAPI")) {
            emailList = getUserEmail("Dev-Harvest");

        } else if (projectKey.contains("INSTTRK")){
            emailList = getUserEmail("orchard-all");

        } else if(projectKey.contains("TWAIN")){
            emailList = emailList + getUserEmail("orchard-dev")+ ",";
            emailList = emailList + getUserEmail("documentation");

        }else if (projectKey.contains("LABELER")){
            emailList = emailList + getUserEmail("orchard-dev");

        }else if (projectKey.contains("LICENSE")){
            emailList = emailList + getUserEmail("orchard-dev")+ ",";
            emailList = emailList + getUserEmail("rtindall")+",";
            emailList = emailList + getUserEmail("aLorraine");

        } else if (projectKey.contains("NLURW")){
            // Should never happen, send it to me
            emailList = getUserEmail("pdwyer");

        } else if (projectKey.contains("Self")){
            emailList = emailList + getUserEmail(user);
        } else { // Debugging
            emailList = emailList + getUserEmail("pdwyer") +",";
            emailList = emailList + getUserEmail("dwacker") + ",";
        }


        return emailList;

    }

    public String setSubject(String projectName, String versionNumber, Version version) {
        // Builds the subject, really just takes the version number and project and adds now available.

        String subject = "";
        ProjectHelper projectHelper = new ProjectHelper();

        String versionDescription = "";
        if(version.getDescription() != null){
            versionDescription = version.getDescription();
        }

        if (projectName.contains("Copia")) { // If Copia we have to add Trellis because it's the same thing
            String date = projectHelper.buildDate(version);
            //String trellisVersion = Integer.toString(projectHelper.currentProjectVersion("TRELLIS",version));
            String trellisVersion = projectHelper.getDescriptionCustomField("Trellis",version);
            String sequioiaVersion = projectHelper.getDescriptionCustomField("Sequoia",version);
            trellisVersion = trellisVersion + "." + date;
            sequioiaVersion = sequioiaVersion + "." + date;

            subject = projectName + " " + versionNumber + "/Trellis " + trellisVersion + "/Sequoia " + sequioiaVersion + projectHelper.checkHotfix(versionNumber) + " is now available";
        } else if (projectName.contains("Product Security")) {
            // No release here
        } else if(projectName.contains("Harvest LIS")) {
            if(versionDescription.contains("Remote Workstation Only")){
                subject = "Remote Workstation "+ versionNumber + projectHelper.checkHotfix(versionNumber) +" is now available";
            } else {
                subject = projectName + " " + versionNumber + projectHelper.checkHotfix(versionNumber) + " is now available";
            }
        }else if (projectName.contains("Win32API")){
            subject = "New version of Win32API plug-in released";
        } else if (projectName.contains("License Generator")){
            subject = versionNumber + " is now available";
        }
        else {
            // All of these don't need the hotfix part as hotfixes aren't really part of the release cycles
            if(projectName.contains("Interface Engine") || (projectName.contains("Orchard TWAIN")) || (projectName.contains("Mapper")) || (projectName.contains("Labeler"))){
                subject = projectName + " " + versionNumber + " is now available";
            }else {
                subject = projectName + " " + versionNumber + projectHelper.checkHotfix(versionNumber) + " is now available";
            }
        }

        if (projectName.contains("Labeler")) {
            projectHelper.handleLabelerRelease(versionNumber);
        }

        return subject;
    }

    public void sendTheEmail(String recipients, String subject, String body) {
        // Does all the email building and puts the email in the mail queue

        debugger.logdebug("Attempting to send the email",className);

        final SMTPMailServer mailServer = MailFactory.getServerManager().getDefaultSMTPMailServer();
        String prefix = mailServer.getPrefix();

        //mailServer.setPrefix("");

        // Build the email class
        Email finalEmail = new Email(recipients);
        finalEmail.setSubject(subject);
        finalEmail.setBody(body);
        finalEmail.setMimeType("text/html");
        finalEmail.setFrom(mailServer.getDefaultFrom());

        // Put it in the mail queue
        final SingleMailQueueItem item = new SingleMailQueueItem(finalEmail);
        ComponentAccessor.getMailQueue().addItem((MailQueueItem) item);


        //ComponentAccessor.getMailQueue().getSendingStarted();
        //try {
        //    TimeUnit.SECONDS.sleep(5);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        //while(ComponentAccessor.getMailQueue().isSending()){
        //   try {
        //      TimeUnit.SECONDS.sleep(2);
        //  } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        //}

        //mailServer.setPrefix(prefix);


    }

     public void downloadTheHTML(String data,String version){



     }

    // If email is sent to this address no matter what the project is, it should go
    // here
    private String addDefault() {
        String emailList = "";

        emailList = emailList + getUserEmail("orchard-operations") + ",";
        emailList = emailList + getUserEmail("documentation") + ",";

        return emailList;
    }

    public String getUserEmail(String userName) {
        // Gets emails of people, if it's a person we just get the email from jira, otherwise just attach the domain to the passed in piece

        String email = "";
        ApplicationUser user = userManager.getUserByNameEvenWhenUnknown(userName);

        if (userManager.isUserExisting(user)) { // Get the email if the user exists
            email = user.getEmailAddress();

        } else { // If we don't know the email (most likely a group like orchard-dev), we should
            // just pass in what the username is in the email
            email = userName + "@orchardsoft.com";
        }

        return email;
    }



}