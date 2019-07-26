// Written by Phil Dwyer
// PRD 11/26/18 DEV-542


// This function will check the url for the project key and return said key
function getProject() {
    var name = "";

    if (window.location.href.indexOf("HARVEST") > -1) {
        name = "HARVEST";
    } else if (window.location.href.indexOf("COPIA") > -1) {
        name = "COPIA";
    } else if (window.location.href.indexOf("INSTTRK") > -1) {
        name = "INSTTRK";
    } else if (window.location.href.indexOf("IE") > -1) {
        name = "IE";
    } else if (window.location.href.indexOf("LABELER") > -1) {
        name = "LABELER";
    } else if (window.location.href.indexOf("MAPPER") > -1) {
        name = "MAPPER";
    } else if (window.location.href.indexOf("LICENSE") > -1) {
        name = "LICENSE";
    } else if (window.location.href.indexOf("OC") > -1) {
        name = "OC";
    } else if (window.location.href.indexOf("ODE") > -1) {
        name = "ODE";
    } else if (window.location.href.indexOf("OT") > -1) {
        name = "OT";
    } else if (window.location.href.indexOf("WINAPI") > -1) {
        name = "WINAPI";
    } else if (window.location.href.indexOf("TEST") > -1) {
        name = "TEST";
    }

    return name;
}

// This function will check if we are on the right screen or if we don't want to allow it
function checkProject() {
    var isCorrectProject = false;
    var bContinue = false;

    if (window.location.href.indexOf("selectedItem=com.atlassian.jira.jira-projects-plugin") > -1) {
        bContinue = (window.location.href.indexOf("release-page") > -1);
    }

    if ((window.location.href.indexOf("administer-versions") > -1) || (bContinue)) {
        if (getProject()!=""){
            isCorrectProject = true;
        }
    }
    return isCorrectProject;
}

// This takes the version name and project to get the ID. The ID is needed to determine what version we are changing. This is only needed
// if we don't have a drop down box or if we
function getVersionID(version,projectKey) {

    var resultData;
    var versionID = "";
    jQuery.ajax({
        type : 'GET',
        dataType : 'json',
        contentType : "application/json;",
        async : false,
        url : AJS.params.baseURL+"/rest/api/2/project/"+projectKey+"/version/?maxResults=1000&orderBy=-releaseDate",
        context : document.body,
        success : function(data) {
            resultData = data;
        },
        error: function(XMLHttpRequest) {
            console.log(XMLHttpRequest.responseText);
        }
    });

    if (resultData != null) {
        var results = [];
        var searchVal = version;
        var searchField = "name";

        for (var i = 0; i < resultData.values.length; i++) {
            if (resultData.values[i][searchField] == searchVal) {
                results.push(resultData.values[i]);
                break;
            }
        }

        versionID = results[0]['id'];
    }
    return versionID;
}


function Release(oldVersion,newVersion,project,versionID) {

    if (versionID == "") {
        versionID = getVersionID(oldVersion, project);
    }

    if (versionID != "") {
        var hotfixCheckbox = document.getElementById("isHotfix").checked;

    jQuery.ajax({
        type: "PUT",
        dataType: "json",
        contentType: "application/json;",
        async: false,
        url: AJS.params.baseURL + "/rest/api/2/version/" + versionID,
        data: JSON.stringify({
            "id": versionID,
            "name": newVersion,
            "released": true,
            "userReleaseDate": getDate(true)
        }),
        context: document.body,
        success: function (data) {
            returnData = data;
        },
        error: function(XMLHttpRequest) {
            console.log(XMLHttpRequest.responseText);
        }
    });

        if (hotfixCheckbox){

            jQuery.ajax({
                type: "POST",
                dataType: "json",
                contentType: "application/json;",
                async: false,
                url: AJS.params.baseURL + "/rest/api/2/version",
                data: JSON.stringify({
                    "name": oldVersion,
                    "released": false,
                    "archived": false,
                    "project": project

                }),
                context: document.body,
                success: function (data) {
                    returnData = data;
                },
                error: function(XMLHttpRequest) {
                    console.log(XMLHttpRequest.responseText);
                }
            });
        }

}
}

function getDate(userDate){
    var today = new Date();
    var dd = String(today.getDate()).padStart(2, '0');
    if (userDate){
        var mm = String(getMonth(today.getMonth())); //January is 0!
    } else {
        var mm = String(today.getMonth() + 1).padStart(2, '0'); //January is 0!
    }
    var yyyy = today.getFullYear();

    if (userDate) {
        today = dd + '/' + mm + '/' + yyyy;
    } else {
        today = yyyy + '-' + mm + '-' + dd;
    }
    return today;
}

function getMonth(month){
    var monthNames = new Array();
    monthNames[0] = "Jan";
    monthNames[1] = "Feb";
    monthNames[2] = "Mar";
    monthNames[3] = "Apr";
    monthNames[4] = "May";
    monthNames[5] = "Jun";
    monthNames[6] = "Jul";
    monthNames[7] = "Aug";
    monthNames[8] = "Sep";
    monthNames[9] = "Oct";
    monthNames[10] = "Nov";
    monthNames[11] = "Dec";
    var name = monthNames[month];
    return name;
}


AJS.toInit(function(jQuery){

    var isCtrl = false;
    var isShift = false;

    $(document).keydown(function (e) {



        if (e.which == 17){
            isCtrl = true;
        }
        if (e.which == 16){
            isShift = true;
        }

        if (isCtrl && isShift && e.which == 69 && checkProject()){
            console.log("Triggering ctrl-shift-e");
            isCtrl = false;
            isShift = false;

            var projectName = getProject();
            var resultData;
            var versionID = "";
            var availableArray = [];
            jQuery.ajax({
                type : 'GET',
                dataType : 'json',
                contentType : "application/json;",
                async : false,
                url : AJS.params.baseURL+"/rest/api/2/project/"+projectName+"/version/?maxResults=1000&orderBy=-releaseDate",
                context : document.body,
                success : function(data) {
                    resultData = data;
                },
                error: function(XMLHttpRequest) {
                    console.log(XMLHttpRequest.responseText);
                }
            });

            if (resultData != null) {

                var searchField = "released";

                for (var i = 0; i < resultData.values.length; i++) {
                    if (resultData.values[i][searchField] == false) {
                        var item = [resultData.values[i]["name"],resultData.values[i]["id"]];
                        availableArray.push(item);
                    }
                }
            }
            availableArray.sort();


            // Standard sizes are 400, 600, 800 and 960 pixels wide
            var dialog = new AJS.Dialog({
                width: 250,
                height: 300,
                id: "release-dialog",
                closeOnOutsideClick: true
            });

            dialog.addHeader("Trigger Release");

            dialog.addButton("Release", function (dialog) {
                var oldversionValue = "";
                var oldversionID = "";
                var e = document.getElementById('nameList');
                if (e != null){
                    oldversionValue = availableArray[e.selectedIndex][0];
                    oldversionID = availableArray[e.selectedIndex][1];
                } else{
                    oldversionValue = document.getElementById('releaseName').value;
                }

                Release(oldversionValue,document.getElementById('newReleaseName').value,projectName,oldversionID);
                dialog.hide();
                location.reload();
            });

            dialog.addCancel("Cancel", function (dialog) {
                dialog.hide();
            }, "#");


            if(availableArray.length > 0) {
                var nameEl = document.getElementById("nameList");
                if (nameEl != null) {
                    nameEl.remove();
                }
                var newNameEl = document.getElementById('newReleaseName');
                if (newNameEl != null){
                    newNameEl.remove();
                }

                var hotfixEl = document.getElementById("isHotfix");
                if (hotfixEl != null){
                    hotfixEl.remove();
                }

                dialog.addPanel("SinglePanel", "<p>Version to Release:" +
                    " <select id='nameList'></select><br/><br/>" +
                    "New Release Name: <input type='text' name='newReleaseName' id='newReleaseName'/><br/><br/>" +
                    "<input type='checkbox' id='isHotfix' name='isHotfix' value='Hotfix'> Create new hotfix?</p>", "panelbody");

                var select = document.getElementById("nameList");
                for (var i=0; i < availableArray.length;i++){
                    var opt = availableArray[i][0];
                    var el = document.createElement("option");
                    el.textContent = opt;
                    el.value = opt;
                    select.appendChild(el);
                }
            } else {
                dialog.addPanel("SinglePanel", "<p>Version to Release:" +
                    " <input type='text' name='releaseName' id='releaseName'/><br/><br/>" +
                    "New Release Name: <input type='text' name='newReleaseName' id='newReleaseName'/><br/><br/>" +
                    "<input type='checkbox' name='isHotfix' value='Hotfix'> Hotfix?</p>", "panelbody");
            }

            e.preventDefault();
            dialog.show();
        }

    })
});
