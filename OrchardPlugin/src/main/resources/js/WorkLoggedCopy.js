// Written by Phil Dwyer

function PostComment(issueKey,tComment){
    AJS.$.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        url: AJS.params.baseURL + "/rest/api/2/issue/"+issueKey+"/comment",
        data: JSON.stringify({
            body: tComment
        }),
        success: function(data) {
        }
    });	
}


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
			//console.log(groups[i].name)
			return true; 
		}
	}
	return false;
}

function removeCopy(tText){
    var tFixedText = '';
    if (tText.includes('[COPY]')) {
        tFixedText = tText.replace('[COPY]', '');
    } else {
        tFixedText = tText;
    }

    return tFixedText;
}

AJS.$(document).ready(function() {
	AJS.$('body').on('click','#log-work-submit',function(){
		var tUser = getCurrentUser();
		if((UserinGroup(tUser,"JG_Development")) || (UserinGroup(tUser,"JG_Testing")) || (UserinGroup(tUser,"JG_Automation"))){
			var bContinue = true,
			self = this,
			tParent = AJS.$(self).closest('#log-work-dialog'),
			issueKey,
			tText;
			if(tParent.length<1){
				tParent = AJS.$(self).closest('#log-work');
			}

			issueKey = AJS.$(tParent).find('span.header-issue-key').text();
			if(!issueKey){
				issueKey = AJS.$(tParent).find('div[issue-key]').attr('issue-key');
			}
			
			AJS.$('span.aui-icon.icon-required').each(function () {
				if(AJS.$(this).parent().parent().find('input').val()===""){
					bContinue=false;	
				}	
			});
			if(bContinue){
				tText =	AJS.$(tParent).find('textarea[name="comment"]').val();
				if(!tText){
					tText=AJS.$(tParent).find('#comment-wiki-edit>textarea[name="comment"]').val();
                    if (tText.includes('[COPY]')){
                        tText = removeCopy(tText);
                        AJS.$(tParent).find('#comment-wiki-edit>textarea[name="comment"]').val(tText);
                    } else {
                        bContinue = false;
                    }
				} else {
                    if (tText.includes('[COPY]')){
                        tText = removeCopy(tText);
                        AJS.$(tParent).find('textarea[name="comment"]').val(tText);
                    } else {
                        bContinue = false;
                    }
                }

				if(bContinue) {
                    PostComment(issueKey, tText);
                }
			}
		}
	});
});