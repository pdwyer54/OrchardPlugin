package com.orchardsoft.plugin.OrchardPlugin.CreatedIssueListener;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.plugins.mail.internal.DefaultMailLoopDetectionService;
import com.atlassian.jira.user.ApplicationUser;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.SendEmail;

// Object that holds all the ticket info that we have already parsed correctly
public class TicketInfoObject {
        private static Debug debugger = new Debug();
        private String className = this.getClass().getSimpleName();
        public String product;
        public String emailUserName;
        public String specReviewTicket;
        public String PmDevTicket;
        public String PmFIATicket;
        public String instModelName;

        public TicketInfoObject(String Product, String EmailUserName, String SpecReviewTicket,String PMDevTicket,String PMFIATicket,String InstModelName){
            product = Product;
            emailUserName = EmailUserName;
            specReviewTicket = SpecReviewTicket;
            PmDevTicket = PMDevTicket;
            PmFIATicket = PMFIATicket;
            instModelName = InstModelName;
        }

        public TicketInfoObject(){
            product = "";
            emailUserName = "";
            specReviewTicket = "";
            PmDevTicket = "";
            PmFIATicket = "";
            instModelName = "";
        }

    public String getProduct() {
        return product;
    }

    public String getEmailUserName() {
            return emailUserName;
    }

    public String getSpecReviewTicket() {
            return specReviewTicket;
    }

    public String getPmDevTicket() {
            return PmDevTicket;
    }

    public String getPmFIATicket() {
        return PmFIATicket;
    }

    public  String getInstModelName() { return instModelName; }

    public void setProduct(String x) { product = x; }

    public void setEmailUserName(String x) { emailUserName = x; }

    public void setSpecReviewTicket(String x) { specReviewTicket = x; }

    public void setPmDevTicket(String x) {PmDevTicket = x; }

    public void setPmFIATicket(String x) { PmFIATicket = x; }

    public  void setInstModelName(String x) { instModelName= x; }


    // Checks the value and will pass back what the type we are checking
    public String getType(String value){
            String returnValue = "";

            if(value == product){
                returnValue = "product";
            } else if(value == emailUserName){
                returnValue = "username";
            } else if (value == specReviewTicket){
                returnValue = "specreview";
            } else if (value == PmDevTicket){
                returnValue = "pmdev";
            } else if (value == PmFIATicket){
                returnValue = "pmfia";
            } else if (value == instModelName){
                returnValue = "instmodel";
            }

            //debugger.logdebug("return value: "+returnValue,className);
            return returnValue;
    }

    // Get's the email address of the user we hold in the object
    public String getEmailAddress(){
        SendEmail emailHandler = new SendEmail();
        return emailHandler.getUserEmail(emailUserName);

    }

    // When we create a ticket it needs the project ID, these are hardcoded numbers
    // So we just se them here
    public Long getProjectID(){
        Long projectID = Long.valueOf(0);

        if(product.contains("ODE")){
            projectID = Long.valueOf(10600);
        } else if (product.contains("Harvest Interface Engine")){
            projectID = Long.valueOf(10504);
    }
        debugger.logdebug("Project ID: "+projectID,className);
        return projectID;
    }

    // Debug info to make sure everything is where it should be
    public void debugTicketInfo() {
        debugger.logdebug("Ticket Info Object:",className);
        debugger.logdebug("Product: "+product,className);
        debugger.logdebug("Email Username: "+emailUserName,className);
        debugger.logdebug("Spec Review Ticket: "+specReviewTicket,className);
        debugger.logdebug("PM Dev Ticket: "+PmDevTicket,className);
        debugger.logdebug("PM FIA Ticket: "+PmFIATicket,className);
        debugger.logdebug("Instrument Model Name: "+instModelName,className);
    }

    // Similar to debug info but this is formatted better so we can send it to the user
    public String getStringTicketInfo(){
            String returnValue = "Product: "+product+
                    "<br> Email: "+getEmailAddress()+
                    "<br> Spec Review JIRA Issue number: "+specReviewTicket+
                    "<br> PM Development JIRA Issue number: "+PmDevTicket+
                    "<br> PM FIA JIRA Issue number: "+PmFIATicket+
                    "<br> Full Instrument Model Name: "+instModelName;
            return returnValue;
    }


}
