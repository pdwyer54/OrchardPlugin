// Written by Drew Waddell and Phil Dwyer
// But Drew did all the hard work


function getCurrentUser() {
	var user;
	AJS.$.ajax({
			url: AJS.params.baseURL + "/rest/gadget/1.0/currentUser",
			type: 'GET',
			dataType: 'json',
			async: false,
			success: function(data) { user = data.username; }
	});
	return user;
}

function getGroups(user){
	var groups;
	AJS.$.ajax({
		url: AJS.params.baseURL + "/rest/api/2/user?username="+user+"&expand=groups",
		type: 'GET',
		dataType: 'json',
		async: false,
		success: function(data) { groups = data.groups.items; }
	});
	return groups;
}

function UserinGroup(user, group){
	var groups = getGroups(user);
	for (i = 0; i < groups.length; i++){
		if (groups[i].name==group){ 
			return true; 
		}
	}
	return false;
	}


AJS.toInit(function(jQuery){
	
	var tUser = getCurrentUser();
	//if((UserinGroup(tUser,"JG_Development")) || (UserinGroup(tUser,"JG_Testing")) || (UserinGroup(tUser,"JG_Automation"))){
	
	    jQuery("#quickSearchInput").on("keyup", function (e) {
				var sVal = jQuery("#quickSearchInput").val();
	
				console.log("This is the current value: "+sVal);
	
				if((sVal.indexOf("H-")==0)||(sVal.indexOf("h-")==0)){
					sVal = sVal.replace("H-","HARVEST-").replace("h-","HARVEST-");
					jQuery("#quickSearchInput").val(sVal);
				} else if((sVal.indexOf("C-")==0)||(sVal.indexOf("c-")==0)){
					sVal = sVal.replace("C-","COPIA-").replace("c-","COPIA-");
					jQuery("#quickSearchInput").val(sVal);
				} else if ((sVal.indexOf("T-")==0)||(sVal.indexOf("t-")==0)){
					sVal = sVal.replace("T-","TRELLIS-").replace("t-","TRELLIS-");
					jQuery("#quickSearchInput").val(sVal);
				} else if ((sVal.indexOf("L-")==0)||(sVal.indexOf("l-")==0)){
					sVal = sVal.replace("L-","LABELER-").replace("l-","LABELER-");
					jQuery("#quickSearchInput").val(sVal);
				} else if ((sVal.indexOf("LG-")==0)||(sVal.indexOf("lg-")==0)){
					sVal = sVal.replace("LG-","LICENSE-").replace("l-","LICENSE-");
					jQuery("#quickSearchInput").val(sVal);
				} else if ((sVal.indexOf("M-")==0)||(sVal.indexOf("m-")==0)){
					sVal = sVal.replace("M-","MAPPER-").replace("m-","MAPPER-");
					jQuery("#quickSearchInput").val(sVal);
	    		} else if (sVal.indexOf("#")==0){
					if(sVal.length>1){
						if(jQuery.isNumeric(sVal[1])){
							sVal = sVal.replace("#","TT-");
							jQuery("#quickSearchInput").val(sVal);
						}
					}
				}
	
			});


//jQuery("#quickSearchInput").change(function(){
	//console.log("Change: "+$(this).val());
//});

	//}
});
