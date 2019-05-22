// Written by Phil Dwyer
// Class to build the query for what issues were ready for release

package com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.query.Query;
import com.atlassian.query.order.SortOrder;
import com.orchardsoft.plugin.OrchardPlugin.Debug;

public class BuildQuery {
	
	private static Debug debugger = new Debug();
	private String className = this.getClass().getSimpleName();
	
	public BuildQuery() {
		
	}
	
	public Query JQLBuilderBuild(String projectName, String version, String componentItem, IssueType issuetype) {
		// If we don't have a component, mark it as empty

		final JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

		if(debugger.isTest) {
			// Test system query
			// Query the project and component only

			if(componentItem == "") {
				builder.where().project().eq().string(projectName).and().issueType().eq().string(issuetype.getName());
			} else {
				builder.where().project().eq().string(projectName).and().component().eq().string(componentItem);
			}
		} else {
			// THE QUERY
			// The component is rotated
			// project = HARVEST AND resolution = Verified AND fixVersion = 11.180328 ORDER BY key ASC
			// project = Copia AND resolution  = Verified AND fixVersion = 7.180227 ORDER BY key ASC
            // Harvest:
            // Crash, Feature Request, Optimization, Defect, Runtime Error, OT Error, UI Change, Question, Script Request, Task
            // Copia:
            // Feature Request, Defect, Crash, UI Change, Script Request, Task, Documentation Change, Question


            if(componentItem == "") {
                builder.where().project().eq().string(projectName).and().fixVersion().eq().string(version).and().issueType().eq().string(issuetype.getName());
				//builder.where().project().eq().string(projectName).and().resolution().eq().string("verified").and().fixVersion().eq().string(version).and().issueType().eq().string(issuetype.getName());
			} else {
				//builder.where().project().eq().string(projectName).and().resolution().eq().string("verified").and().fixVersion().eq().string(version).and().component().eq().string(componentItem);
			}
		}
		builder.orderBy().issueKey(SortOrder.ASC);
		
		Query query = builder.buildQuery();
		
		return query;
	}

}
