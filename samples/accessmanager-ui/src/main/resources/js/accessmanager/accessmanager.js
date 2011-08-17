/*!
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Holds some common functions used by other access management objects 
 */
AccessManager = {
    /**
     * Holds the servlet context path of the web application
     */
    contextPath: "",

    /**
     * Resource strings for usermgmt that can be localized for other languages
     */
    messages: {
        "error.dlg.title": "Error",
        "confirm.yes": "Yes",
        "confirm.no": "No",
        "button.add": "Add",
        
        "tooltip.removeProperty": "Remove Property",
        "tooltip.removeMember": "Remove Member",
        
        "group.updated.msg": "Updated the group",
        "user.updated.msg": "Updated the user",
        "user.pwd.updated.msg": "Updated the password"
    }    
};

/**
 * For the navigation links in the left side of the page.
 */
AccessManager.SideBar = {
    /**
     * Initialize SideBar elements
     */
    init: function() {
        var i, navLinks, url, hash, nav;
        
        // highlight the link for the current page
        navLinks = $("ul#sidebar-nav li a");
        url = location.href;
        hash = location.hash;
        if (hash != null && hash.length > 0) {
            url = url.substring(0, url.length - hash.length);
        }
        for (i=0; i < navLinks.length; i++) {
            nav = navLinks[i];
            if (url == nav.href) {
                $(nav.parentNode).addClass("ui-corner-all ui-state-highlight");
            }
        }
    }
};

/**
 * For showing an error dialog when something goes wrong.
 */
AccessManager.ErrorDlg = {
    /**
     * Show a modal dialog with the supplied content.
     * @param {String} title The title text of the dialog
     * @param {String} msg The html markup of the dialog body
     */
    showError: function(title, msg) {
        // add an error_dialog div to the page if it doesn't already exist
        if ($('#error_dialog').length == 0) {
            $('body').append('<div id="error_dialog" style="display:none;"></div>');
        }
        //fill in the dialog body
        $('#error_dialog').html( msg );
        $('#error_dialog').dialog({
            title: title,
            bgiframe: true, 
            autoOpen: false, 
            width: 400,
            maxHeight: 500,
            modal: true
        });
        //show the modal dialog
        $('#error_dialog').dialog('open');
    },
    
    /**
     * Handles an error from an AJAX call by showing the error information
     * in a modal dialog.
     */
    errorHandler: function( xmlHttpRequest, textStatus, errorThrown ) {
        var title, obj;
        try {
            title = UserMgmt.messages["error.dlg.title"];
            //see if the response is a JSON resoponse with an error field
            obj = $.parseJSON(xmlHttpRequest.responseText);
            if (obj.error) {
                //found error field, so show the error message
                UserMgmt.ErrorDlg.showError(title, obj.error.message);                        
            } else {
                //no error field, so show the whole response text
                UserMgmt.ErrorDlg.showError(title, xmlHttpRequest.responseText);                        
            }
        } catch (e) {
            //Not JSON? Show the whole response text.
            UserMgmt.ErrorDlg.showError(title, xmlHttpRequest.responseText);                        
        }                        
    }
};

/**
 * For pages the that view/modify the Access Control List
 */
AccessManager.Acl = {
    /**
     * Initializes the elements for the ACL page
     */
    init: function() {
	    //apply jquery-ui styles to the buttons
    	$("#acl-list button").button();
    	
		$("#acl-list tbody tr:odd").addClass("odd");
		
        //attach a confirmation dialog to all the 'Remove' buttons
        $('.remove-ace').click( function(e) {
        	var form = this.form;
            //initialize the confirmation dialog
            $("#remove-ace-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: AccessManager.messages["confirm.yes"],
                        click: function() {
                    		form.submit();
                        } 
                    },
                    {
                        text: AccessManager.messages["confirm.no"],
                        click: function() {
                            $("#remove-ace-dialog").dialog("close");
                        }
                    }
                ]
            });

            //show the dialog
            $('#remove-ace-dialog').dialog('open');
            return false;
        });		
	}
};

/**
 * For pages the that view/modify an Access Control Entry
 */
AccessManager.Ace = {
    /**
     * Initializes the elements for the ACE page
     */
    init: function() {
		var m, form;
		m = AccessManager.Ace;
		
		$("#ace-list tbody tr:odd").addClass("odd");
		
	    //apply styles to the save button
        $("#addAceSaveButton").button();
			
		// validate form
		form = $("#update-ace-form");
		/*var validator = */form.validate({
			rules: {
				"principalId": "required"
			}/*,
			messages: {
			}*/
		});
		
		var allGranted = $("input[name='privilege@jcr:all'][value=granted]:radio:checked");
		var allDenied = $("input[name='privilege@jcr:all'][value=denied]:radio:checked");
		if (allGranted.length > 0 || allDenied.length > 0) {
			var value = allGranted.length > 0 ? "granted" : "denied";
			m.setAggregatePrivilege("jcr:read", value);
			m.setAggregatePrivilege("jcr:write", value);
			m.setAggregatePrivilege("jcr:modifyProperties", value);
			m.setAggregatePrivilege("jcr:removeNode", value);
			m.setAggregatePrivilege("jcr:addChildNodes", value);
			m.setAggregatePrivilege("jcr:removeChildNodes", value);

			m.setAggregatePrivilege("jcr:readAccessControl", value);
			m.setAggregatePrivilege("jcr:modifyAccessControl", value);

			m.setAggregatePrivilege("jcr:lockManagement", value);
			m.setAggregatePrivilege("jcr:versionManagement", value);
			m.setAggregatePrivilege("jcr:nodeTypeManagement", value);
			m.setAggregatePrivilege("jcr:retentionManagement", value);
			m.setAggregatePrivilege("jcr:lifecycleManagement", value);
		} else {
			var writeGranted = $("input[name='privilege@jcr:write'][value=granted]:radio:checked");
			var writeDenied = $("input[name='privilege@jcr:write'][value=denied]:radio:checked");
			if (writeGranted.length > 0 || writeDenied.length > 0) {
				var value = writeGranted.length > 0 ? "granted" : "denied";
				m.setAggregatePrivilege("jcr:modifyProperties", value);
				m.setAggregatePrivilege("jcr:removeNode", value);
				m.setAggregatePrivilege("jcr:addChildNodes", value);
				m.setAggregatePrivilege("jcr:removeChildNodes", value);
			}
		}

		$("input[name='privilege@jcr:all']:radio").bind("change", function(e) {
			m.setAggregatePrivilege("jcr:read", this.value);
			m.setAggregatePrivilege("jcr:write", this.value);
			m.setAggregatePrivilege("jcr:modifyProperties", this.value);
			m.setAggregatePrivilege("jcr:removeNode", this.value);
			m.setAggregatePrivilege("jcr:addChildNodes", this.value);
			m.setAggregatePrivilege("jcr:removeChildNodes", this.value);

			m.setAggregatePrivilege("jcr:readAccessControl", this.value);
			m.setAggregatePrivilege("jcr:modifyAccessControl", this.value);

			m.setAggregatePrivilege("jcr:lockManagement", this.value);
			m.setAggregatePrivilege("jcr:versionManagement", this.value);
			m.setAggregatePrivilege("jcr:nodeTypeManagement", this.value);
			m.setAggregatePrivilege("jcr:retentionManagement", this.value);
			m.setAggregatePrivilege("jcr:lifecycleManagement", this.value);
		});
		
		$("input[name='privilege@jcr:write']:radio").bind("change", function(e) {
			m.setAggregatePrivilege("jcr:modifyProperties", this.value);
			m.setAggregatePrivilege("jcr:removeNode", this.value);
			m.setAggregatePrivilege("jcr:addChildNodes", this.value);
			m.setAggregatePrivilege("jcr:removeChildNodes", this.value);
		});
		
        //attach an autocomplete handler to the name field
        $( "#principalId" ).autocomplete({
            source: AccessManager.contextPath + "/system/userManager.autocomplete.json",
            minLength: 1,
            select: function(event, ui) {
                var item = ui.item;
                $("#principalId")
                    .val(item.value)
                    .data("item", item);
            }
        })
        .data( "autocomplete" )._renderItem = function( ul, item ) {
            return $( "<li></li>" )
                .data( "item.autocomplete", item )
                .append( "<a>" + (item.label ? (item.label + " (" + item.value + ")") : item.value) + "</a>" )
                .appendTo( ul );
        };    
        
        //attach a confirmation dialog to the 'Remove' link
        $('#removeAceLink').click( function(e) {
            //initialize the confirmation dialog
            $("#remove-ace-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: AccessManager.messages["confirm.yes"],
                        click: function() {
                            $("#remove-ace-form").submit();
                        } 
                    },
                    {
                        text: AccessManager.messages["confirm.no"],
                        click: function() {
                            $("#remove-ace-dialog").dialog("close");
                        }
                    }
                ]
            });

            //show the dialog
            $('#remove-ace-dialog').dialog('open');
            return false;
        });
	},

	setAggregatePrivilege: function(privilegeName, value) {
	  	var btn = $("input[name='privilege@" + privilegeName + "'][value=granted]:radio");
	    btn.attr('checked', value == "granted");
	    btn.attr('disabled', value != "none");
	
	  	btn = $("input[name='privilege@" + privilegeName + "'][value=denied]:radio");
	    btn.attr('checked', value == "denied");
	    btn.attr('disabled', value != "none");
	
	  	btn = $("input[name='privilege@" + privilegeName + "'][value=none]:radio");
	    btn.attr('checked', value == "none");
	    btn.attr('disabled', value != "none");
	}
};

/**
 * Initialize any objects that are active on the current page
 */
$(function() {
    //make visible any elements that require scripting to be enabled 
    $(".noscript-hide").removeClass("noscript-hide");
    
    if ($("#sidebar-nav").length > 0) {
        AccessManager.SideBar.init();
    }
    
    if ($("#update-acl-block").length > 0) {
        AccessManager.Acl.init();
    }
    
    if ($("#update-ace-form").length > 0) {
        AccessManager.Ace.init();
    }
});
