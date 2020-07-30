// Written by Phil Dwyer
// PRD 04/23/19 DEV-557


function getProject2() {
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
function checkProject2() {
    var isCorrectProject = false;
    if (getProject2()!=""){
        if ((window.location.href.indexOf("/versions/") > -1) && (window.location.href.indexOf("/projects/") > -1)) {
            isCorrectProject = true;
        } else if((window.location.href.indexOf("/fixforversion/") > -1)){
            isCorrectProject = true;
        }
    }
    return isCorrectProject;
}

function isFilter(){
    var isCorrectFilter = false;
    if (window.location.href.indexOf("issues/?filter=") > -1){
        isCorrectFilter = true;
    } else if (window.location.href.indexOf("/issues/?jql=") > -1){
        isCorrectFilter = true;
    }

    return isCorrectFilter;
}

function getInfo(strVersion){
    // This calls DownloadServlet.java on the server
    // This only have a doGet statement right now so ajax can only use the get command

    var text = '';
    $.ajax({
        type : "GET",
        url : AJS.params.baseURL+"/plugins/servlet/downloadservlet",
        async : false,
        data : "version="+strVersion+"&project="+getProject2()+"&isDownload=true&createReport=true",
        success : function(data) {
            text = data;
        },
        error: function(XMLHttpRequest) {
            console.log(XMLHttpRequest.responseText);
        }
    });

    return text;

}

function createReport(filter){
    // This calls DownloadServlet.java on the server
    // This only have a doGet statement right now so ajax can only use the get command

    var text = '';
    $.ajax({
        type : "GET",
        url : AJS.params.baseURL+"/plugins/servlet/downloadservlet",
        async : false,
        data : "filter="+filter+"&isDownload=false&createReport=true",
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

    // This method creates an element that then is used to send a download to the user
    var text = getInfo(version);

    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);

    element.style.display = 'none';
    document.body.appendChild(element);

    element.click();

    document.body.removeChild(element);
}

function getReport(filename, filter) {

    // This method creates an element that then is used to send a download to the user
    var text = createReport(filter);

    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);

    element.style.display = 'none';
    document.body.appendChild(element);

    element.click();

    document.body.removeChild(element);
}

function getVersionName(versionID) {

    // We will have the version ID so we need to get the version data so we can have the name of the version itself
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
        if (isCtrl && isShift && e.which == 190 && checkProject2()){
            console.log("Triggering ctrl-shift-.");
            isCtrl = false;
            isShift = false;

            var pathArray = window.location.pathname.split('/'); // The last piece is the version Id number so
            var strVersion = getVersionName(pathArray[pathArray.length - 1]);

            sendDownload(getProject2()+" "+strVersion+".txt", strVersion);

        } else  if (isCtrl && isShift && e.which == 76) {
            console.log("Triggering ctrl-shift-l");
            isCtrl = false;
            isShift = false;

            if (isFilter()) {
                var filter = $("#advanced-search").val()
                console.log("Filter: "+filter);
                var today = new Date();
                getReport( "Time logged Report "+today.toDateString()+".txt",filter);
            }
        }
    })
});
