// Written by Phil Dwyer
// PRD 04/23/19 DEV-557


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
    if (getProject()!=""){
        if ((window.location.href.indexOf("/versions/") > -1) && (window.location.href.indexOf("/projects/") > -1)) {
            isCorrectProject = true;
        } else if((window.location.href.indexOf("/fixforversion/") > -1)){
            isCorrectProject = true;
        }
    }
    return isCorrectProject;
}

function getDescription(versionID,projectKey) {

    var resultData;
    var oldDescription = "";
    jQuery.ajax({
        type : 'GET',
        dataType : 'json',
        contentType : "application/json;",
        async : false,
        url : AJS.params.baseURL+"/rest/api/2/version/"+versionID,
        context : document.body,
        success : function(data) {
            resultData = data;
        },
        error: function(XMLHttpRequest) {
            console.log(XMLHttpRequest.responseText);
        }
    });

    if (resultData != null) {
        oldDescription = resultData['description'];
    }
    return oldDescription;

}


function Release(VersionID,project) {
    var versionID = VersionID;
    var oldDescription = getDescription(versionID,project);

    if (versionID != "") {
        jQuery.ajax({
            type: "PUT",
            dataType: "json",
            contentType: "application/json;",
            async: false,
            url: AJS.params.baseURL + "/rest/api/2/version/" + versionID,
            data: JSON.stringify({
                "released": false,
                "description": "download"
            }),
            context: document.body,
            success: function (data) {
                returnData = data;
            },
            error: function(XMLHttpRequest) {
                console.log(XMLHttpRequest.responseText);
            }
        });



        jQuery.ajax({
            type: "PUT",
            dataType: "json",
            contentType: "application/json;",
            async: false,
            url: AJS.params.baseURL + "/rest/api/2/version/" + versionID,
            data: JSON.stringify({
                "released": true,
            }),
            context: document.body,
            success: function (data) {
                returnData = data;
            },
            error: function(XMLHttpRequest) {
                console.log(XMLHttpRequest.responseText);
            }
        });



        jQuery.ajax({
            type: "PUT",
            dataType: "json",
            contentType: "application/json;",
            async: false,
            url: AJS.params.baseURL + "/rest/api/2/version/" + versionID,
            data: JSON.stringify({
                "description": oldDescription
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

        if (isCtrl && isShift && e.which == 76 && checkProject()){
            console.log("Triggering ctrl-shift-e");
            isCtrl = false;
            isShift = false;

            var dialog = new AJS.Dialog({
                width: 300,
                height: 50,
                id: "dialog-download",
                closeOnOutsideClick: false
            });
            dialog.addHeader("Downloading Release Notes");

            dialog.show();
            var pathArray = window.location.pathname.split('/');
            Release(pathArray[pathArray.length - 1], getProject());
            dialog.hide();
            location.reload();
        }

    })
});
