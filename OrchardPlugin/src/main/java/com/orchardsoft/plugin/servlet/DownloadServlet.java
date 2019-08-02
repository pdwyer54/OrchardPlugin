// Written by Phil Dwyer
// PRD 11/26/18 DEV-557
//
// This method is called from javascript on the client and it basically runs the release notes so we can pass back the html text
//



package com.orchardsoft.plugin.servlet;

import com.orchardsoft.plugin.OrchardPlugin.Debug;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.orchardsoft.plugin.OrchardPlugin.ReleaseNotesListener.DownloadHelper;

public class DownloadServlet extends HttpServlet{
    private static Debug debugger = new Debug();
    private String className = this.getClass().getSimpleName();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {

        String version = req.getParameter("version");
        String projectKey = req.getParameter("project");
        String text = "";
        DownloadHelper helper = new DownloadHelper();
        debugger.logdebug("Running download helper",className);
        text = helper.versionRelease(null, version, projectKey);
        if(text == "") {
            debugger.logdebug("text was blank",className);
        }

        resp.setContentType("text/html");
        resp.getWriter().write(text);
    }

}