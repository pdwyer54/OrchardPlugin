package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.VelocityParamFactory;
import com.orchardsoft.plugin.OrchardPlugin.Debug;
import com.atlassian.velocity.VelocityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BuildTemplate {
    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();

    @SuppressWarnings("InjectedReferences")
    public String getTemplate(ArrayList<Issue> IssueList, Project project, Version version, String template) {
        final Map<String, Object> body = new HashMap<String, Object>(); // Final map to put pass into the template renderer so we have the info when building the template
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        ArrayList<TemplateObject> templateObjectArrayList = new ArrayList<TemplateObject>();
        String versionDescription = "";
        if(version.getDescription() != null){
            versionDescription = version.getDescription();
        }

        // Gather the custom field objects
        debugger.logdebug("Number of issues: "+IssueList.size(),className);
        Collection<CustomField> ReleaseNotesCollection = customFieldManager.getCustomFieldObjectsByName("Release Notes");
        Collection<CustomField> FixedbyCollection = customFieldManager.getCustomFieldObjectsByName("Fixed By");
        Collection<CustomField> ClientNameCollection = customFieldManager.getCustomFieldObjectsByName("Client Name");
        Collection<CustomField> PageCollection = customFieldManager.getCustomFieldObjectsByName("Page/Feature");
        CustomField ReleaseNotes = null;
        CustomField Fixedby = null;
        CustomField ClientName = null;
        CustomField Page = null;

        // So theoretically this only loops once as we only have 1 custom field with this name. If that changes we will need to change this
        // We are just going to fill the custom fields and immediately break
        for (CustomField customLoop : ReleaseNotesCollection){
            ReleaseNotes = customLoop;
            break;
        }
        for (CustomField customLoop : FixedbyCollection){
            Fixedby = customLoop;
            break;
        }
        for (CustomField customLoop : ClientNameCollection){
            ClientName = customLoop;
            break;
        }

        for(CustomField customLoop : PageCollection){
            Page = customLoop;
            break;
        }

        String releaseNotesString;
        String fixedByString;
        String clientNameString;
        String pageString;

        RendererManager rendererManager = ComponentAccessor.getRendererManager();
        JiraRendererPlugin jiraRendererPlugin = rendererManager.getRendererForType("atlassian-wiki-renderer");

        for (Issue issue : IssueList) { // This should already be in a list sorted by issue type and key

            // This really isn't needed but it can't hurt to clear every time
            releaseNotesString = "";
            fixedByString = "";
            clientNameString = "";
            pageString = "";

            // If the field is null it can fire errors so always check
            if(Fixedby != null) {
                ApplicationUser FixedbyUser = (ApplicationUser) issue.getCustomFieldValue(Fixedby); // getCustomFieldValue returns a java object, cast it to a user object
                if(FixedbyUser != null) {
                    debugger.logdebug("User: "+FixedbyUser.getDisplayName(),className);
                    fixedByString = FixedbyUser.getDisplayName();
                }
            }
            if(ReleaseNotes != null) {
                Object releaseNotesObject = issue.getCustomFieldValue(ReleaseNotes); // We can just read these objects with the toString command
                if(releaseNotesObject != null) {
                    releaseNotesString = scrubDND(releaseNotesObject.toString());
                    if(releaseNotesString.replaceAll("\\s","").length() > 0) {
                        releaseNotesString = jiraRendererPlugin.render(releaseNotesString, issue.getIssueRenderContext());
                        //debugger.logdebug(releaseNotesString,className);
                    } else {
                        releaseNotesString = "";
                    }
                }
            }

            if(ClientName != null) {
                Object clientNameObject = issue.getCustomFieldValue(ClientName);
                if(clientNameObject != null) {
                    clientNameString = clientNameObject.toString();
                }
            }

            if(Page != null){
                Object PageObject = issue.getCustomFieldValue(Page);
                if(PageObject != null){
                    pageString = PageObject.toString();
                }
            }

            // Create the template object with everything in it
            if (!releaseNotesString.isEmpty()) { // Only create it if it's not empty
                TemplateObject templateObject = new TemplateObject(releaseNotesString, issue.getKey(), fixedByString, clientNameString, issue.getIssueType().getName(), pageString,issue);
                templateObjectArrayList.add(templateObject);
            }
        }

        Collection<IssueType> issueTypeList = project.getIssueTypes();
        ArrayList<Integer> issueTypeOrder = new ArrayList<Integer>();
        ArrayList<String> issueTypeName = new ArrayList<String>();

        int i = 0;

        // This sequence is to figure out when to create a new table in the template. We could probably do without this and just check when the issue type changes
        // But my proficiency in velocity is not great so I want to make it as easy as possible.
        for (IssueType issueType : issueTypeList) {
            for (TemplateObject templateObject : templateObjectArrayList) {
                if(templateObject.Type == issueType.getName()) {
                    issueTypeOrder.add(i);
                    if(issueType.getName().contains("Crash")){
                        issueTypeName.add(issueType.getName() + "es"); // Add the es for grammatical reasons
                    }else {
                        issueTypeName.add(issueType.getName() + "s"); // Add the s for the table headers
                    }
                    debugger.logdebug(Integer.toString(i),className);
                    break;
                }
            }
            i++;
        }



        ApplicationProperties ap = ComponentAccessor.getApplicationProperties();
        String baseUrl = ap.getString(APKeys.JIRA_BASEURL);
        String orchardLogo = baseUrl+"/download/resources/com.orchardsoft.plugin.OrchardPlugin:OrchardPlugin-resources/images/Main.png"; // Main logo for bottom of email
        debugger.logdebug("Adding Flavor Text",className);
        String flavorText = getFlavorText(project,version);

        debugger.logdebug(flavorText,className);

        debugger.logdebug("Constructing Body",className);
        // Fill the map to pass into the template
        body.put("issueInfoArray",templateObjectArrayList);
        body.put("project",project);
        body.put("Image",getProjectLogoURL(project.getKey(),baseUrl,version));
        body.put("issueTypeOrder",issueTypeOrder);
        body.put("IssueTypeName",issueTypeName);
        body.put("baseURL",baseUrl);
        body.put("MainLogo",orchardLogo);
        body.put("FlavorText",flavorText);
        //body.put("debug",debugger);
        //body.put("PageBuilderService",pageBuilderService);
        String renderedText = "";

        String webworkEncoding = ap.getString(APKeys.JIRA_WEBWORK_ENCODING);
        VelocityManager vm = ComponentAccessor.getVelocityManager();
        //VelocityParamFactory vp = ComponentAccessor.getVelocityParamFactory();

        //templateRenderer.render("templates/ReleaseNotesHTML.vm",body,);

        debugger.logdebug("rendering template",className);
        //pageBuilderService.assembler().resources().requireWebResource("OrchardPlugin:OrchardPlugin-bootstrap");

        // New way
        try {
            renderedText = vm.getEncodedBody("templates/", template, baseUrl, webworkEncoding, body);
            debugger.logdebug(renderedText,className);
        } catch(Exception e) {
            debugger.logdebug("Failed to render",className);
            debugger.logdebug(e.toString(),className);
        }

        return renderedText;



    }


    private String getProjectLogoURL(String projectKey, String baseURL, Version version) {
        // Method to get the correct icon for the project
        String path = baseURL+"/download/resources/com.orchardsoft.plugin.OrchardPlugin:OrchardPlugin-resources/images/";

        String versionDescription = "";
        if(version.getDescription() != null){
            versionDescription = version.getDescription();
        }

        if(projectKey.contains("HARVEST")) {
            if(versionDescription.contains("Remote Workstation Only")) {
                path = path + "RW.png";
            } else{
                path = path + "Harvest.png";
            }
        } else if(projectKey.contains("COPIA")) {
            path = path + "Copia.png";
        } else if(projectKey.contains("IE")) {
            path = path + "IE.png";
        } else if(projectKey.contains("MAPPER")) {
            path = path + "Mapper.png";
        } else if(projectKey.contains("OC")) {
            path = path + "Collect.png";
        } else if(projectKey.contains("ODE")) {
            path = path + "ODE.png";
        } else if(projectKey.contains("OT")) {
            path = path + "twain.ico";
        } else if (projectKey.contains("INSTTRK")){
            path = path + "InstrumentTracker.png";
        } else if (projectKey.contains("LABELER")){
            path = path + "Labeler.png";
        } else if(projectKey.contains("PS")) {
            path = "";
        } else if(projectKey.contains("WinAPI")) {
            path = "";
        } else if(projectKey.contains("NLURW")) {
            path = "";
        } else {
            path = "";
        }

        return path;
    }

    private String scrubDND(String releaseNotes){
        String finalReleaseNotes = "";
        int DNDPosition = 0;

        if (releaseNotes.contains("[DND]")){
            DNDPosition = releaseNotes.indexOf("[DND]");
            finalReleaseNotes = releaseNotes.substring(0,DNDPosition);
            debugger.logdebug(Integer.toString(DNDPosition),className);
        } else if (releaseNotes.contains("DND")){
            DNDPosition = releaseNotes.indexOf("DND");
            finalReleaseNotes = releaseNotes.substring(0,DNDPosition);
            debugger.logdebug(Integer.toString(DNDPosition),className);
        } else {
            finalReleaseNotes = releaseNotes;
        }
        debugger.logdebug(finalReleaseNotes,className);

        return finalReleaseNotes;
    }

    private String getFlavorText(Project project, Version version){

        debugger.logdebug("getFlavorText has been called.",className);

        String projectKey = project.getKey();
        String returnText = "";

        String extraText = "";
        String extraText2 = "";

        ProjectHelper projectHelper = new ProjectHelper();

        String versionDescription = "";
        if(version.getDescription() != null){
            versionDescription = version.getDescription();
        }

        if(projectKey.contains("HARVEST")) {
            if(versionDescription.contains("Remote Workstation Only")) {
                returnText = "Remote Workstation "+version.getName()+projectHelper.checkHotfix(version.getName())+" is available today on the ORC and OPS drive.";
            } else{
                extraText = projectHelper.getProjectCustomField("Interface Engine",project);
                returnText = "Harvest "+version.getName()+projectHelper.checkHotfix(version.getName())+" is available today on the ORC and OPS drive. It includes Interface Engine"+extraText+".";
            }
        } else if(projectKey.contains("COPIA")) {
            returnText = "Warning: Make sure you have a backup of the client database before you update their system.";

        } else if(projectKey.contains("IE")) {
            returnText = "Version "+version.getName()+" of the Harvest Interface Engine is now available on the ORC.";

        } else if(projectKey.contains("OC")) {
            //extraText = projectHelper.getProjectCustomField("Version",project);
            //extraText2 = projectHelper.getProjectCustomField("Harvest Version",project);
            //extraText3 = projectHelper.getProjectCustomField("Copia Version",project);
            returnText = "IMPORTANT: Version 2 should only be installed with a Harvest 11.180711.180727 build  or a Copia 7.180907/Trellis 1.180907 build or later.";

        }  else if (projectKey.contains("INSTTRK")){
            extraText2 = projectHelper.getProjectCustomField("Website",project);
            extraText = " <a href=\""+extraText2+"\">here</a>";
            debugger.logdebug(extraText,className);
            returnText = "Instrument Tracker "+version.getName()+" is now available"+extraText;
            debugger.logdebug(returnText,className);

        } else if(projectKey.contains("MAPPER")) {
            returnText = "Hope it works!!!";

        } else if (projectKey.contains("LICENSE")){
            returnText = version.getName()+" is now available at \\\\OIT-File-01\\\\Harvest9Lic";
        } else {
            returnText = "";
        }

        return returnText;
    }

}
