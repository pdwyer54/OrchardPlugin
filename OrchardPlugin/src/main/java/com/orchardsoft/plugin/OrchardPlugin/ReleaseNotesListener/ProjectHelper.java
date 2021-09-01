// Written by Phil Dwyer
// Class that just does some helpful stuff related to what project we are releasing

package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.label.LabelService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ErrorCollection;
import com.orchardsoft.plugin.OrchardPlugin.Debug;

import java.util.*;

public class ProjectHelper {
	
	private static Debug debugger = new Debug();
	private String className = this.getClass().getSimpleName();
	private static final IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
	private static final ProjectComponentManager componentManager = ComponentAccessor.getProjectComponentManager();
	private static final CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
	private static final IssueManager issueManager = ComponentAccessor.getIssueManager();

	public ProjectHelper() {
		
	}
	
	public String checkProject(Issue issue) { // Get the project name of the issue
		return issue.getProjectObject().getName();
	}
	
	public void buildProjectList(ArrayList<String> List){ // Build the list of project keys, if needed
		List.add("HARVEST");
		List.add("COPIA");
		List.add("IE");
		List.add("LABELER");
		List.add("LICENSE");
		List.add("MAPPER");
		List.add("OC");
		List.add("ODE");
		List.add("OT");
		List.add("PS");
		List.add("NLURW");
		List.add("WINAPI");
		List.add("INSTTRK");
	}
	
	public int currentProjectVersion(String project, Version version) {
		// Gets the version number for what project is. At some point we want to move this to outside hardcoding and to a field that is in the project

		int versionNumber=0;
		debugger.logdebug("Project Key: "+project,className);

		String versionDescription = "";
		if(version.getDescription() != null){
            versionDescription = version.getDescription();
        }

		// -1 we are not gonna act upon it (still using the date though)
		// 0 we are not gonna change the version
		if(project.contains("HARVEST")) {
		    if(version.getName().contains("RW") || versionDescription.contains("Remote Workstation Only")) {
                versionNumber = 4;
                //TriggerRelease.isRW = true;
            } else {
                versionNumber = 11;
                //TriggerRelease.isRW = false;
            }
		} else if(project.contains("COPIA")) {
			versionNumber = 7;
		} else if(project.contains("IE")) {
			versionNumber = 2;
		} else if(project.contains("MAPPER")) {
			versionNumber = 4;
		} else if(project.contains("OC")) {
			versionNumber = 2;
		} else if(project.contains("ODE")) {
			versionNumber = -1;
		} else if(project.contains("OT")) {
			versionNumber = 1;
		} else if(project.contains("PS")) {
			versionNumber = 0;
		} else if(project.contains("WINAPI")) {
			versionNumber = 8;
		} else if(project.contains("NLURW")) {
			versionNumber = 1;
		} else if (project.contains("TRELLIS")){
            versionNumber = 1;
        } else if (project.contains("INSTTRK")){
			versionNumber = 1;
        } else { // 99, way off so we know it's wrong
			versionNumber = 99;
		}

		return versionNumber;	
	}

	public String getDescriptionCustomField(String keyword,Version version){

		String fieldText = "";
		debugger.logdebug("getDescriptionCustomField called",className);
		String description = "";
		if(version != null) {
			if(version.getDescription() != null) {
				description = version.getDescription();
			}
		}
		String searchKeyword = keyword+":";

		debugger.logdebug(searchKeyword,className);
		debugger.logdebug(description,className);

		if(description.contains(searchKeyword)){
			fieldText = getCustomField(description,searchKeyword);
		}

		return fieldText;
	}

    public String getProjectCustomField(String keyword, Project project){

        debugger.logdebug("getProjectCustomField has been called",className);
        String fieldText = "";
        String description = project.getDescription();
        String searchKeyword = keyword+":";

        debugger.logdebug(searchKeyword,className);
        debugger.logdebug(description,className);

        if(description.contains(searchKeyword)){
			fieldText = getCustomField(description,searchKeyword);
        }

        return " "+fieldText;
    }

    public String getCustomField(String description,String searchKeyword){

		String fieldText = "";
		debugger.logdebug("keyword found",className);
		int Pos = description.indexOf(searchKeyword);
		int lastPos = Pos + searchKeyword.length();
		debugger.logdebug(Integer.toString(lastPos),className);
		if(lastPos>=0) {
			int endPos = description.indexOf(";", lastPos);
			debugger.logdebug(Integer.toString(endPos),className);
			if (endPos >= 0) {
				fieldText = description.substring(lastPos,endPos);
				debugger.logdebug(fieldText,className);
				if(fieldText.charAt(0) == ' '){
					debugger.logdebug("First char is a space",className);
					// Remove the space
					fieldText = fieldText.substring(1);
				}
			}
		}

		return fieldText;
	}

	public String checkHotfix(String versionNumber){
		String hotfixText = "";
		int firstPeriod = versionNumber.indexOf(".");
		int secondPeriod = -1;

		// Check if there are 2 periods
		if(firstPeriod > 0){
			secondPeriod = versionNumber.indexOf(".",firstPeriod+1);
			if(secondPeriod > firstPeriod){
				hotfixText = " Hotfix";
			}
		}


		return hotfixText;
	}

	public Boolean handleLabelerRelease(String versionNumber){
		// When labeler is released we need to create new tickets for Harvest, ODE, Collect, and Copia
		IssueService issueService = ComponentAccessor.getIssueService();
		JiraAuthenticationContext jAC = ComponentAccessor.getJiraAuthenticationContext();
		ApplicationUser user = jAC.getLoggedInUser();
		IssueInputParameters issueInput = issueService.newIssueInputParameters();
		Long HarvestPID = new Long(10201); // Harvest
		Long CopiaPID = new Long(10202); // Copia
		Long ODEPID = new Long(10600); // ODE
		Long CollectPID = new Long(10500); // Collect
		String summary = "Update Labeler";
		String description = "New Labeler build is being released "+versionNumber+"\r\n  Update the project to include application/installer as necessary. \r\n ORC: \\\\orc\\Downloads\\Labeler\\1";
		String IDType = "3";

		debugger.logdebug("Creating tickets for Labeler",className);



		createIssue(user,HarvestPID,summary,description,IDType,"slerch",IDType);
		createIssue(user,CopiaPID,summary,description,IDType,"dwacker",IDType);
		createIssue(user,ODEPID,summary,description,IDType,"ODE_Team",IDType);
		createIssue(user,CollectPID,summary,description,IDType,"dwaddell",IDType);

		return true;
	}

	public Issue createIssue(ApplicationUser user, Long ProjectID, String summary, String description, String issueIDType, String assignee, String priorityIDType){

		IssueService issueService = ComponentAccessor.getIssueService();
		IssueInputParameters issueInput = issueService.newIssueInputParameters();
		JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();

		if(issueIDType.contains("10018")){
			CustomField customField = getCustomFieldObject("Epic Name");
			issueInput.addCustomFieldValue(customField.getIdAsLong(),summary);
		}

		issueInput.setProjectId(ProjectID);
		issueInput.setSummary(summary);
		issueInput.setDescription(description);
		issueInput.setIssueTypeId(issueIDType); // Task
		issueInput.setPriorityId(priorityIDType); // Standard
		issueInput.setReporterId(user.getUsername());
		issueInput.setAssigneeId(assignee);

		if (user != null){
			debugger.logdebug(user.getDisplayName()+" is creating an issue", className);
			jiraAuthenticationContext.setLoggedInUser(user);
		} else{
			debugger.logdebug("The user is null",className);
		}

		IssueService.CreateValidationResult issue = issueService.validateCreate(user,issueInput);

		debugger.logdebug("Creating a ticket in project: "+ProjectID.toString(),className);

		if (!issue.isValid()) {
			ErrorCollection errorCollection = issue.getErrorCollection();
			Map<String, String> errorMessages = errorCollection.getErrors();
			for (Map.Entry<String, String> entry : errorMessages.entrySet()) {
				debugger.logdebug("Key: " + entry.getKey(), className);
				debugger.logdebug("Value: " + entry.getValue(), className);
			}
		}

		Issue returnIssue = null;
		if (issue.isValid()) {
			//debugger.logdebug(errorMessages.toString(),className);
			IssueService.IssueResult iResult = issueService.create(user, issue);
			returnIssue = iResult.getIssue();
		}

			jiraAuthenticationContext.setLoggedInUser(null);
			return returnIssue;

	}

	public Boolean addIssueLink(Issue sourceIssue, Issue destissue, String issueLinkType, ApplicationUser user){
		Boolean succeeded = false;
		Long IssueLinkID = Long.valueOf(0);
		if(issueLinkType == "relates"){
			IssueLinkID = Long.valueOf(10003);
		} else if (issueLinkType == "sub-task") {
			IssueLinkID = Long.valueOf(10700);
		}

		try{
			issueLinkManager.createIssueLink(sourceIssue.getId(),destissue.getId(),IssueLinkID,Long.valueOf(1),user);
			succeeded = true;
		}catch (CreateException x){
			debugger.logdebug("There was an error in creating the issue link: "+sourceIssue.getId()+" "+destissue.getId()+" "+user.getUsername(),className);
			debugger.logdebug(x.getMessage(),className);
		}


		return succeeded;
	}

	public Boolean checkIssueLink(Issue sourceIssue, String issueLinkType, ApplicationUser user){
		Boolean Exists = false;

		Long IssueLinkID = Long.valueOf(0);
		if(issueLinkType == "relates"){
			IssueLinkID = Long.valueOf(10003);
		} else if (issueLinkType == "sub-task") {
			IssueLinkID = Long.valueOf(10700);
		}

			Set<IssueLinkType> linkedIssues = issueLinkManager.getLinkCollection(sourceIssue,user).getLinkTypes();
			for (IssueLinkType linkType:linkedIssues) {
				if(linkType.getId() == IssueLinkID){
					Exists = true;
					break;
				}
			}

		return Exists;
	}

	public Collection<Issue> getIssueLinks(Issue sourceIssue, String issueLinkType, ApplicationUser user){

		final Collection<Issue> issueLinks = new ArrayList<Issue>();
		Long IssueLinkID = Long.valueOf(0);
		boolean sourceCheck = true; // If this is true the source issue is the outward description, if false the source issue is the inward description
		if(issueLinkType == "relates"){
			IssueLinkID = Long.valueOf(10003);
			debugger.logdebug("relates was selected",className);
		} else if (issueLinkType == "sub-task") {
			IssueLinkID = Long.valueOf(10700);
			debugger.logdebug("sub-task was selected",className);
		} else if (issueLinkType == "is blocked by"){
			IssueLinkID = Long.valueOf(10203);
			sourceCheck = false;
			debugger.logdebug("is blocked by was selected",className);
		} else if (issueLinkType == "blocks"){
			IssueLinkID = Long.valueOf(10203);
			sourceCheck = true;
			debugger.logdebug("blocks was selected",className);
		}

		if (IssueLinkID > 0) {
			Collection<Issue> allLinkedIssues = issueLinkManager.getLinkCollection(sourceIssue, user).getAllIssues();
			debugger.logdebug("There are " + String.valueOf(allLinkedIssues.size()) + " issues linked", className);
			for (Issue issue : allLinkedIssues) {
				debugger.logdebug(issue.getKey()+" is being checked",className);
				if (sourceCheck){
					if (issueLinkManager.getIssueLink(sourceIssue.getId(), issue.getId(), IssueLinkID) != null) {
						debugger.logdebug(issue.getKey()+" was added",className);
						issueLinks.add(issue);
					}
				} else {
					if ((issueLinkManager.getIssueLink(issue.getId(), sourceIssue.getId(), IssueLinkID) != null)) {
						debugger.logdebug(issue.getKey() + " was added", className);
						issueLinks.add(issue);
					}
				}
			}
			if (!issueLinks.isEmpty()) {
				debugger.logdebug("There are links in this issue: " + String.valueOf(issueLinks.size()), className);
			}
		}

		return issueLinks;
	}

	public Issue getSubTaskParent (Issue subTask){
		Issue returnIssue = null;

		List<IssueLink> allLinks = issueLinkManager.getInwardLinks(subTask.getId());
		for (IssueLink link : allLinks) {
			if(link.getIssueLinkType().getId() == 10700){
				// It's a sub-task return the parent
				returnIssue = link.getSourceObject();
			}
		}

		return returnIssue;
	}

	public boolean setSummary(Issue issue, String summary){
		MutableIssue mutableIssue = (MutableIssue) issue;
		boolean success = false;

		mutableIssue.setSummary(summary);
		try{
			issueManager.updateIssue(issue.getReporterUser(),mutableIssue, EventDispatchOption.ISSUE_UPDATED,false);
			success = true;
		} catch (Exception x){
			debugger.logdebug("There was an error with updating the issue and adding the component", className);
		}

		return success;

	}

	public boolean setFixVersion(Issue issue, Collection<Version> versions){
		MutableIssue mutableIssue = (MutableIssue) issue;
		boolean success = false;

		mutableIssue.setFixVersions(versions);
		try{
			issueManager.updateIssue(issue.getReporterUser(),mutableIssue, EventDispatchOption.ISSUE_UPDATED,false);
			success = true;
		} catch (Exception x){
			debugger.logdebug("There was an error with updating the issue and adding the component", className);
		}

		return success;

	}

	public boolean setReporter(Issue issue, ApplicationUser user){
		MutableIssue mutableIssue = (MutableIssue) issue;
		boolean success = false;

		mutableIssue.setReporter(user);
		try{
			issueManager.updateIssue(user,mutableIssue, EventDispatchOption.ISSUE_UPDATED,false);
			success = true;
		}catch (Exception x){
			debugger.logdebug("There was an error with updating the issue's reporter", className);
		}
		return success;
	}

	public boolean addComponent(Issue issue, String componentToAdd, Long projectID){
		boolean success = false;

		ProjectComponent component = componentManager.findByComponentName(projectID,componentToAdd);

		if(component != null) {
			debugger.logdebug("Found component: "+component.getName(),className);
			MutableIssue mutableIssue = (MutableIssue) issue;
			debugger.logdebug("Mutable Issue Components: "+mutableIssue.getKey(),className);
			Collection<ProjectComponent> issueComponents= issue.getComponents();
			issueComponents.add(component);
			mutableIssue.setComponent(issueComponents);
			debugger.logdebug("Mutable Issue Components: "+mutableIssue.getComponents().toString(),className);
			try{
				issueManager.updateIssue(issue.getReporterUser(),mutableIssue, EventDispatchOption.ISSUE_UPDATED,false);
				success = true;
			} catch (Exception x){
				debugger.logdebug("There was an error with updating the issue and adding the component", className);
			}

		} else {
			debugger.logdebug("Could not find component",className);
		}

		return success;
	}

	public void addCustomFieldValue(Issue issue, CustomField customField, Object customFieldVal){
		debugger.logdebug("Attempting to add custom field value",className);
		try {
			MutableIssue mutableIssue = (MutableIssue) issue;
			debugger.logdebug("Mutable Issue: "+mutableIssue.getKey(),className);
			mutableIssue.setCustomFieldValue(customField,customFieldVal);
			debugger.logdebug("Setting custom field: "+customField.getFieldName()+" to "+customFieldVal.toString(),className);
			try{
				issueManager.updateIssue(issue.getReporterUser(),mutableIssue, EventDispatchOption.ISSUE_UPDATED,false);
				debugger.logdebug("Issue updated",className);
			} catch (Exception x){
				debugger.logdebug("There was an error with updating the issue and adding the customfield", className);
			}
		} catch (Exception x){
			debugger.logdebug("Failed to cast to Mutable Issue",className);
		}

	}

	public CustomField getCustomFieldObject(String customFieldName){
		CustomField finalCustomField = null;

		Collection<CustomField> customFieldCollection = customFieldManager.getCustomFieldObjectsByName(customFieldName);
		if (!(customFieldCollection.isEmpty())) {

			// So this should only have 1 value in it so after the first loop just break
			for (CustomField customfield : customFieldCollection) {
				finalCustomField = customfield;
				break;
			}
		}
		return finalCustomField;
	}

	public boolean isResolved(Issue issue){
		debugger.logdebug("isResolved was called",className);
		boolean resolved = false;
		Resolution resolution = issue.getResolution();
		String sRes = "";
		if(resolution != null){
			sRes = resolution.getName();
		}
		debugger.logdebug("Resolution is "+sRes,className);

		if (sRes.equals("Documented")){
			resolved = true;
		} else if (sRes.equals("Done")){
			resolved = true;
		} else if (sRes.equals("Fixed")){
			resolved = true;
		} else if (sRes.equals("Verified")){
			resolved = true;
		} else if (sRes.equals("Resolved")){
			resolved = true;
		} else if (sRes.equals("Duplicate")){
			resolved = true;
		} else if (sRes.equals("No longer needed")){
			resolved = true;
		}

		if(resolved){
			debugger.logdebug("Issue is resolved",className);
		} else{
			debugger.logdebug("Issue is unresolved",className);
		}

		return resolved;
	}

	public Collection<ProjectComponent> getComponentByProject(Project project) { // Gets all the components of a project
		return project.getComponents();
	}
	
	public Collection<IssueType> getIssueTypeByProject(Project project) {
		return project.getIssueTypes();
	}

	public Collection<Issue> getIssuesInEpic(Issue epic) {
		final String sLinkTypeName = "Epic-Story Link";
		final Collection<IssueLink> links = ComponentAccessor.getIssueLinkManager().getOutwardLinks(epic.getId());
		final Collection<Issue> issuesInEpic = new ArrayList<Issue>();
		for (final IssueLink link : links) {
			final String name = link.getIssueLinkType().getName();
			final Issue destinationObject = link.getDestinationObject();
			if (sLinkTypeName.equals(name)) {
				issuesInEpic.add(destinationObject);
			}
		}

		if(!issuesInEpic.isEmpty()) {
			debugger.logdebug("There are issues in this epic: "+String.valueOf(issuesInEpic.size()),className);
		}

		return issuesInEpic;
	}

	public void addLabel(Long issueKey,String value){
		LabelService labelService = null;
		value=value.replace(" ","_");
		debugger.logdebug("Attempting to add label: "+value,className);


		final ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(); // get current user
		debugger.logdebug("User gotten: "+currentUser.getDisplayName(),className);
		debugger.logdebug("Attempting to validate label",className);
		try{
			debugger.logdebug("Attempting to add label: "+value+" to issue ID: "+issueKey.toString(),className);
			LabelService.AddLabelValidationResult validationResult = labelService.validateAddLabel(currentUser,issueKey,value);
			if (validationResult.isValid()){
				LabelService.LabelsResult result = labelService.addLabel(currentUser,validationResult,false);
				if (!result.isValid()){
					debugger.logdebug("Errors in adding label",className);
					debugger.logdebug(result.getErrorCollection().toString(),className);
				}
			}
		}catch (Exception x){
			debugger.logdebug("Errors in validating label",className);

		}


	}

	public String getIEVersion(Version version){

		String IEVersion = "";

		String projectName = "Interface Engine";
		String newIE = getDescriptionCustomField("IEExtraVersion1",version);
		String newIE2 = getDescriptionCustomField("IEExtraVersion2",version);
		String date = buildDate(version);
		String versionNumber = version.getName();

		if(!newIE.isEmpty()){
			newIE = newIE + "." + date;
		}
		if(!newIE2.isEmpty()) {
			newIE2 = newIE2 + "." + date;
		}
		if((newIE.isEmpty()) && (newIE2.isEmpty())) {
			IEVersion = projectName + " " + versionNumber;
		} else if (newIE2.isEmpty()) {
			IEVersion = projectName + " " + versionNumber + "/"+ newIE;
		} else if (newIE.isEmpty()) {
			IEVersion = projectName + " " + versionNumber + "/"+ newIE2;
		} else {
			IEVersion = projectName + " " + versionNumber + "/"+ newIE2 + "/"+ newIE;
		}

		return IEVersion;

	}


	
    public String buildDate(Version version) { // Build the date for releases, now looks like "YYMMDD" or "180329" for 03/29/2018 // Also handles hotfix if we pass in a version with Hotfix in it
    	
    	// Builds non-hotfix stuff
        /**
        LocalDate date = LocalDate.now();
    	String month = convertDayMonth(date.getMonthValue());
    	String day =  convertDayMonth(date.getDayOfMonth());
    	String year = convertYear(date.getYear());
    	String formatedDate = year+month+day;

    	// Builds hotfix stuff if needed
    	if(version.getName().contains("Hotfix")) {
    		Project project = version.getProject();
    		Collection<Version> versions = project.getVersions();
    			for (Version ver : versions) {
    				if(ver.isReleased()) {
    					String verName = ver.getName();
    					int lastindex = verName.lastIndexOf(".");
    					int firstindex = verName.indexOf(".");
    					if(lastindex > 0) {
    						String finalVerPrefix;
    						if(lastindex != firstindex) {
    							finalVerPrefix = verName.substring(0,lastindex-1);
    						} else {
    							finalVerPrefix = verName;
    						}
    						formatedDate = finalVerPrefix+"."+formatedDate;

    						break;
    					}
    					
    				}
    			}
    	}
		 **/
        String finalVer = "";
    	String verName = version.getName();
        int firstindex = verName.indexOf(".");
        if(firstindex > 0){
          finalVer = verName.substring(firstindex+1,verName.length());
        }

    	
    	debugger.logdebug(finalVer,className);
    	return finalVer;
    }
    
    private String convertDayMonth(int day) { // Convert the day if it's 1-9
    	String date;
    	
		if(day == 1) {
			date="01";
		} else if(day == 2) {
			date="02";
		} else if(day == 3) {
			date="03";
		} else if(day == 4) {
			date="04";
		} else if(day == 5) {
			date="05";
		} else if(day == 6) {
			date="06";
		} else if(day == 7) {
			date="07";
		} else if(day == 8) {
			date="08";
		} else if(day == 9) {
			date="09";
		} else {
			date= Integer.toString(day);
		}
    	
    	return date;
    }
    
    private String convertYear(int year) { // Convert the year to the last 2 digits, so 2018 -> 18
    	String date;
    	
    	date = Integer.toString(year);
    	date = date.substring(date.length()-2);
    			
    	return date;
    }

    private Boolean addHofixVersion(Project project){
        return true;
    }

}