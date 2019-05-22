// Written by Phil Dwyer
// PRD 11/26/18 DEV-542


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

function checkProject() {
    var isCorrectProject = false;

    if ((window.location.href.indexOf("administer-versions") > -1)) {
        if (getProject()!=""){
            isCorrectProject = true;
        }
    }
    return isCorrectProject;
}

function getVersionID(version,projectKey) {

    var resultData;
    var versionID = "";
    jQuery.ajax({
        type : 'GET',
        dataType : 'json',
        contentType : "application/json;",
        async : false,
        url : AJS.params.baseURL+"/rest/api/2/project/"+projectKey+"/version/?maxResults=100&orderBy=-releaseDate",
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


function Release(oldVersion,newVersion,project) {
    var versionID = getVersionID(oldVersion, project);

    if (versionID != "") {
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

            // Standard sizes are 400, 600, 800 and 960 pixels wide
            var dialog = new AJS.Dialog({
                width: 215,
                height: 250,
                id: "release-dialog",
                closeOnOutsideClick: true
            });

            dialog.addHeader("Trigger Release");

            dialog.addButton("Release", function (dialog) {
                Release(document.getElementById('releaseName').value,document.getElementById('newReleaseName').value,getProject());
                dialog.hide();
                location.reload();
            });

            dialog.addCancel("Cancel", function (dialog) {
                dialog.hide();
            }, "#");

            dialog.addPanel("SinglePanel", "<p>Version to Release: <input type='text' name='releaseName' id='releaseName'/><br/><br/>New Release Name: <input type='text' name='newReleaseName' id='newReleaseName'/></p>", "panelbody");

            e.preventDefault();
            dialog.show();
        }

    })
});
