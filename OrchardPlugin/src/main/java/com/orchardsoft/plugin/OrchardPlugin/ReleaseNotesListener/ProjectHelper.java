// Written by Phil Dwyer
// Class that just does some helpful stuff related to what project we are releasing

package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.orchardsoft.plugin.OrchardPlugin.Debug;

import java.util.ArrayList;
import java.util.Collection;

public class ProjectHelper {
	
	private static Debug debugger = new Debug();
	private String className = this.getClass().getSimpleName();
	
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
		String description = version.getDescription();
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

		createIssue(user,HarvestPID,summary,description,IDType,"slerch");
		createIssue(user,CopiaPID,summary,description,IDType,"dwacker");
		createIssue(user,ODEPID,summary,description,IDType,"ODE_Team");
		createIssue(user,CollectPID,summary,description,IDType,"dwaddell");

		return true;
	}

	private void createIssue(ApplicationUser user, Long ProjectID, String summary, String description, String IDType, String assignee){
		IssueService issueService = ComponentAccessor.getIssueService();
		IssueInputParameters issueInput = issueService.newIssueInputParameters();

		issueInput.setProjectId(ProjectID);
		issueInput.setSummary(summary);
		issueInput.setDescription(description);
		issueInput.setIssueTypeId(IDType); // Task
		issueInput.setPriorityId(IDType); // Standard
		issueInput.setReporterId(user.getUsername());
		issueInput.setAssigneeId(assignee);

		IssueService.CreateValidationResult issue = issueService.validateCreate(user,issueInput);

		//ErrorCollection errorCollection = issue.getErrorCollection();
		//Map<String, String> errorMessages = errorCollection.getErrors();
		//for (Map.Entry<String,String> entry : errorMessages.entrySet()){
		//	debugger.logdebug("Key: "+entry.getKey(),className);
		//	debugger.logdebug("Value: "+entry.getValue(),className);
		//}

		//debugger.logdebug(errorMessages.toString(),className);
		IssueService.IssueResult iResult = issueService.create(user, issue);
	}
	
	public Collection<ProjectComponent> getComponentByProject(Project project) { // Gets all the components of a project
		return project.getComponents();
	}
	
	public Collection<IssueType> getIssueTypeByProject(Project project) {
		return project.getIssueTypes();
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