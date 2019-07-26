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

// This function will check if we are on the right screen or if we don't want to allow it
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

function getInfo(strVersion){
    var text = '';
    $.ajax({
        type : "GET",
        url : AJS.params.baseURL+"/plugins/servlet/downloadservlet",
        async : false,
        data : "version="+strVersion+"&project="+getProject(),
        success : function(data) {
            text = data;
        },
        error: function(XMLHttpRequest) {
            console.log(XMLHttpRequest.responseText);
        }
    });

    return text;

}

function sendDownload(filename, version) {

    var text = getInfo(version);

    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);

    element.style.display = 'none';
    document.body.appendChild(element);

    element.click();

    document.body.removeChild(element);
}

function getVersionName(versionID) {
    var version = '';

    var resultData;
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
        version = resultData['name'];
    }


    return version;
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
            // ctrl-shift-.
        if (isCtrl && isShift && e.which == 190 && checkProject()){
            console.log("Triggering ctrl-shift-.");
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
            var strVersion = getVersionName(pathArray[pathArray.length - 1]);

            sendDownload(getProject()+" "+strVersion+".txt", strVersion);


            dialog.hide();
        }

    })
});
