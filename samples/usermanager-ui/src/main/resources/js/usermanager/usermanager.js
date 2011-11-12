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
 * Holds some common functions used by other user management objects 
 */
UserManager = {
    /**
     * Holds the servlet context path of the web application
     */
    contextPath: "",

    /**
     * Format a string by replacing tokens with the passed in arguments.
     * 
     * @param {String} msg The message pattern.  Use {n} for replacable arguments where n is the arg index.
     * @param {Array} args array of arguments to apply to the message pattern
     */
    formatMsg: function(msg, args) {
        var regex = /{(\d+)}/g;
        return msg.replace(regex, function() {
            var index = parseInt(arguments[1]);
            return args[index];
        });
    },
    
    /**
     * Resource strings for usermgmt that can be localized for other languages
     */
    messages: {
        "error.dlg.title": "Error",
        "confirm.yes": "Yes",
        "confirm.no": "No",
        "button.add": "Add",
        
        "searching.progress.msg": "Searching, please wait...",
        
        "tooltip.removeProperty": "Remove Property",
        "tooltip.removeMember": "Remove Member",
        
        "group.updated.msg": "Updated the group",
        "user.updated.msg": "Updated the user",
        "user.pwd.updated.msg": "Updated the password",
        
        "group.created.msg": "Created the group",
        "user.created.msg": "Created the user"
    }    
}

/**
 * For the navigation links in the left side of the page.
 */
UserManager.SideBar = {
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
}

/**
 * For showing an error dialog when something goes wrong.
 */
UserManager.ErrorDlg = {
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
            title = UserManager.messages["error.dlg.title"];
            //see if the response is a JSON resoponse with an error field
            obj = $.parseJSON(xmlHttpRequest.responseText);
            if (obj.error) {
                //found error field, so show the error message
                UserManager.ErrorDlg.showError(title, obj.error.message);                        
            } else {
                //no error field, so show the whole response text
                UserManager.ErrorDlg.showError(title, xmlHttpRequest.responseText);                        
            }
        } catch (e) {
            //Not JSON? Show the whole response text.
            UserManager.ErrorDlg.showError(title, xmlHttpRequest.responseText);                        
        }                        
    }
};

/**
 * For pages the that search for Users or Groups.
 */
UserManager.Authorizables = {
    /**
     * Holds the id of the searching progress function (if any).
     */
    progressFnId: null,
    
    /**
     * Initializes the elements for the User/Group search page
     */
    init: function() {
        //apply styles to the quick-nav radio buttons
        $('#find-authorizables-quick-nav').buttonset().show();

        //apply styles and handlers to the search result table 
        UserManager.Authorizables.applyResultStylesAndHandlers();
        
        /**
         * Attach event handlers to the quick-hav radio buttons
         */
        $('#find-authorizables-quick-nav input[type = "radio"]').change( function (e) {
            //reset the paging to the first page
            $('#searchOffset').val(0);

            //clear value from the search input
            $("#findAuthorizablesQuery").val("");
            
            //do the search
            UserManager.Authorizables.runSearchFn();
            return false;
        });
    
        //apply jquery-ui styles to the search button
        $('button#findAuthorizablesBtn').button();
    
        //attach event handler to the search button
        $('button#findAuthorizablesBtn').click( function(e) {
            if ($('#findAuthorizablesQuery').val() == '') {
                //empty query == 'All'
                $('#radioAll').attr('checked', true);
                $('#radioAll').button('refresh');
            } else {
                //uncheck all the quick-nav radio buttons
                $('#radioNone').attr('checked', true);
                $('#radioNone').button('refresh');
            }
            
            //do the search
            UserManager.Authorizables.runSearchFn(0);
            return false;
        });

        /**
         * Listen for changes to the location hash.  This is to
         * make the browser history work (back button).
         */
        $(window).bind('hashchange', UserManager.Authorizables.runSearchFromHashFn);
        
        //trigger the search now in case we got here from the back button
        $(window).trigger("hashchange");
    },
    
    /**
     * Displays a 'Loading...' progress animation if the search is taking
     * a while to return.
     */
    progressFn: function() {
        //inject a progress block
        $('#authorizables-results-body')
            .after('<div id="authorizables-results-progress" class="search-empty-msg ui-corner-all ui-state-highlight">' + UserManager.messages["searching.progress.msg"] + '</div>');
        
        //done with this.
        UserManager.Authorizables.progressFnId = null;
    },
    
    /**
     * Removes the progress animation if it is present.
     */
    clearProgressFn: function() {
        if (UserManager.Authorizables.progressFnId != null) { 
            //stop the progress animation if it hasn't started yet.
            clearTimeout(UserManager.Authorizables.progressFnId); 
            UserManager.Authorizables.progressFnId = null;
        }
        //remove the old progress element from the page (if there)
        $('#authorizables-results-progress').remove();
    },

    /**
     * Executes the search, showing results starting at the
     * specified offset
     * @param {Number} offset the index of the first row to render
     */
    runSearchFn: function(offset) {
        var form, formData;
        
        //update the offset field in the form
        $('#searchOffset').val(offset == undefined ? "0" : offset);

        form = $("#find-authorizables-form");
        //serialize the form data to set as the location hash
        formData = form.serialize();
        
        //changing the location hash triggers the ajax search call
        // using the location hash here to make the browser back button work.
        window.location.hash = formData;
    },

    /**
     * Parse the location hash and extract the name/value pairs
     */
    parseHashParams: function() {
        var hashParams = {},
            e,
            a = /\+/g,  // Regex for replacing addition symbol with a space
            r = /([^&;=]+)=?([^&;]*)/g,
            d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
            q = window.location.hash.substring(1);

        while (e = r.exec(q))
           hashParams[d(e[1])] = d(e[2]);

        return hashParams;
    },
    
    /**
     * Executes the search when a change to the location hash has
     * been detected.
     */
    runSearchFromHashFn: function() {
        var form, action, hashParams;

        form = $("#find-authorizables-form");
        action = form.attr('action');
        //change the target sling selector to the searchresult fragment
        action = action.substring(0, action.length - 4) + "searchresult.html";

        //get the map of parameters in the hash string
        hashParams = UserManager.Authorizables.parseHashParams()
        
        //fill the query text into the search input
        $("#findAuthorizablesQuery").val(hashParams.q == undefined ? "" : hashParams.q);
        //select the correct quick-nav radio button
        $('#find-authorizables-quick-nav input[value="' + (hashParams.sp == undefined ? "" : hashParams.sp) + '"]')
            .attr("checked", true)
            .button('refresh');
        
        //fill in the offset value
        $('#searchOffset').val(hashParams.o == undefined ? "0" : hashParams.o);

        if (hashParams.q == undefined && hashParams.sp == undefined) {
            //the hashCode doesn't have a query, so don't to a search 
            if ($("#search-result-ready-to-search").length == 0) {
                // not on the ready-to-search page so we need to refresh the
                // result panel to get the right UI.
                $.ajax({
                    url: action,
                    type: 'get',
                    success: UserManager.Authorizables.searchDoneFn,
                    error: UserManager.ErrorDlg.errorHandler
                });
            }
            return;
        }
        
        //the hashCode has a query, so proceed.. 

        //hide the old results table
        $('#authorizables-results-body').hide();

        //show a progress animation if the search takes longer than 200ms to return
        UserManager.Authorizables.clearProgressFn();
        UserManager.Authorizables.progressFnId = setTimeout(UserManager.Authorizables.progressFn, 200);

        //do the search request.
        $.ajax({
            url: action,
            type: 'get',
            data: form.serialize(),
            cache: false,
            success: UserManager.Authorizables.searchDoneFn,
            error: function(xmlHttpRequest, textStatus, errorThrown) {
                //clear the progress animation if it is there.
                UserManager.Authorizables.clearProgressFn();
                
                //delegate the rest to the generic error handler
                UserManager.ErrorDlg.errorHandler(xmlHttpRequest, textStatus, errorThrown);
            }
        });
    },

    /**
     * Callback function for a successful search request
     */
    searchDoneFn: function( data, textStatus, xmlHttpRequest ) {
        //clear the progress animation if it is there.
        UserManager.Authorizables.clearProgressFn();
        
        //inject the results table into the page
        $('#authorizables-results-body').replaceWith(data);

        //apply extra styles and event handlers to the results table
        UserManager.Authorizables.applyResultStylesAndHandlers();
    },

    /**
     * Apply styles and event handlers to the result table rows
     */
    applyResultStylesAndHandlers: function( data, textStatus, xmlHttpRequest ) {
        //highlight the table row onHover
        $("#search-result-body tr")
            .mouseover(function(){
                $(this).addClass("ui-state-highlight");
            })
            .mouseout(function(){
                $(this).removeClass("ui-state-highlight");
            })
            .click(function(){
                var href = $(this).find("td a")[0].href;
                window.location = href;
                return false;
            });
        
        //attach event handlers to the paging buttons
        $("#first_page").button()
            .click(function() {
                UserManager.Authorizables.runSearchFn(0);
                return false;
            });            
        $("#prev_page").button()
            .click(function() {
                UserManager.Authorizables.runSearchFn($("#prev_page").val());
                return false;
            });
        $("#next_page").button()
            .click(function() {
                UserManager.Authorizables.runSearchFn($("#next_page").val());
                return false;
            });
    }
};


/**
 * Holds some common functions for Group related pages 
 */
UserManager.Group = {
        
};

/**
 * For the Group create page.
 */
UserManager.Group.Create = {
    /**
     * Initializes the elements for the create Group page
     */
    init: function() {
        //apply jquery-ui styles to create button
        $('button#createGroupBtn').button();

        // validate form
        /*var validator = */$("#create-group-form").validate({
            rules: {
                ":name": "required"
            }/*,
            messages: {
            }*/
        });

        //apply event handler to the create button
        $('button#createGroupBtn').click( function(e) {
            var form, actionUrl, formData;
            
            form = $("#create-group-form");
            
            //client side validation
            if (!form.valid()) {
                return false;
            }
            
            actionUrl = form.attr('action');

            //switch to a json response
            actionUrl = actionUrl.substring(0, actionUrl.length - 4) + "json";

            //turn off redirect in this context
            $('#redirect').attr('disabled', true);
            formData = form.serialize();
            $('#redirect').removeAttr('disabled');

            //hide the info msg from a previous action
            $("#create-group-body div.info-msg-block").hide();
            
            //submit the create request
            $.ajax({
                url: actionUrl,
                type: 'POST',
                data: formData,
                success: function( data, textStatus, xmlHttpRequest ) {
                    //clear the inputs
                    $("#create-group-form input[type='text']").val('');

                    //inject a success message
                    $("#create-group-body span.info-msg-text").html(UserManager.messages["group.created.msg"]);
                    $("#create-group-body div.info-msg-block").show();
                },
                error: UserManager.ErrorDlg.errorHandler
            });
            
            return false;
        });
    }
};

/**
 * For the Group update page.
 */
UserManager.Group.Update = {
    /**
     * Initializes the elements for the create Group page
     */
    init: function() {
        //apply jquery-ui styles to the update button
        $('button#updateGroupBtn').button();

        // validate form
        /*var validator = */$("#update-group-form").validate({
            rules: {
            }/*,
            messages: {
            }*/
        });
        
        //hover states on the remove member icons
        $('.remove-member').hover(
            function() { $(this).addClass('ui-state-hover'); }, 
            function() { $(this).removeClass('ui-state-hover'); }
        ).click(
            function(e) {
                //mark the member for deletion when the update is saved
                var memberItem = $(e.currentTarget.parentNode);
                memberItem.hide('slow', function() {
                    if ($('ol#declaredMembers li:visible').length == 0) {
                        $("#declaredMembers__empty").show();
                    }
                });
                memberItem.find('input').attr("name", ":member@Delete");
                
                return false;
            }
        );

        //hover states on the remove property icons
        $('.remove-property').hover(
            function() { $(this).addClass('ui-state-hover'); }, 
            function() { $(this).removeClass('ui-state-hover'); }
        ).click(
            function(e) {
                var memberItem, input, key;
                
                //mark the property for deletion when the update is saved
                memberItem = $(e.currentTarget.parentNode);
                memberItem.hide('slow');
                input = memberItem.find('input');
                key = input.attr("name");
                input.attr("name", key + "@Delete");
                return false;
            }
        );
        
        //attach event handler to the update button
        $('button#updateGroupBtn').click( function(e) {
            var form, actionUrl, formData;
            
            form = $("#update-group-form");

            //client side validation
            if (!form.valid()) {
                return false;
            }
            
            actionUrl = form.attr('action');

            //switch to a json response
            actionUrl = actionUrl.substring(0, actionUrl.length - 4) + "json";

            //turn off redirect in this context
            $('#redirect').attr('disabled', true);
            formData = form.serialize();
            $('#redirect').removeAttr('disabled');

            //hide the info msg from a previous action
            $("#update-group-body div.info-msg-block").hide();
            
            //submit the update request
            $.ajax({
                url: actionUrl,
                type: 'POST',
                data: formData,
                success: function( data, textStatus, xmlHttpRequest ) {
            		$("#update-group-body").parent().load(UserManager.contextPath + data.path + ".update_body.html", function() {
                        $("#update-group-body span.info-msg-text").html(UserManager.messages["group.updated.msg"]);
                        $("#update-group-body div.info-msg-block").show();
                    });
                    
                    setTimeout(function() {
                        //re-init since we just replaced the body
                        UserManager.Group.Update.init();

                        //make visible any elements that require scripting to be enabled 
                        $(".noscript-hide").removeClass("noscript-hide");
                    }, 100);
                },
                error: UserManager.ErrorDlg.errorHandler
            });
            return false;
        });

        
        //attach a confirmation dialog to the 'Remove' link
        $('a#removeGroupLink').click( function(e) {
            //initialize the confirmation dialog
            $("#remove-group-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: UserManager.messages["confirm.yes"],
                        click: function() {
                            $("#remove-group-form").submit();
                        } 
                    },
                    {
                        text: UserManager.messages["confirm.no"],
                        click: function() {
                            $("#remove-group-dialog").dialog("close");
                        }
                    }
                ]
            });

            //show the dialog
            $('#remove-group-dialog').dialog('open');
            return false;
        });

        //attach a prompt dialog to the 'Add Property' link
        $('a#add_property').click( function(e) {
            //initialize the dialog
            $("#add-property-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: UserManager.messages["button.add"],
                        click: function() {
                            var form, name, label, newItem;
                            
                            form = $("#add-property-form");
                            //client side validation
                            if (!form.valid()) {
                                return false;
                            }
                        
                            //get new property name
                            name=$("#newPropName").val();
                            label = name;
                            
                            //inject property line for the new property
                            $("#updateSubmitBtns").before('<div class="prop-line ui-helper-clearfix"><label for="' + name + '">' + label + ':</label> <input id="' + name + '" type="text" name="' + name + '" /> <a href="#" class="remove-property" title="' + UserManager.messages["tooltip.removeProperty"] + '"><span class="ui-icon ui-icon-circle-close"></span></a></div>');
                            
                            newItem = $("#updateSubmitBtns").prev();
                            
                            //add hover states on the remove property icons
                            newItem.find('a.remove-property').hover(
                                function() { $(this).addClass('ui-state-hover'); }, 
                                function() { $(this).removeClass('ui-state-hover'); }
                            ).click(
                                function(e) {
                                    var propItem = $(e.currentTarget.parentNode);
                                    //haven't saved yet on server, so just remove this element from the page.
                                    propItem.hide('slow', function() {
                                        propItem.remove();
                                    });
                                    return false;
                                }
                            );
                            
                            //close the dialog
                            $("#add-property-dialog").dialog( "close" );

                            //give focus to the new property line
                            newItem.find('input').focus();
                        } 
                    }
                ]
            });
            
            //clear old value from the dialog (if there)
            $("#newPropName").val('');
            
            //open the dialog
            $('#add-property-dialog').dialog('open');
            return false;
        });

        //forward the form submit event to the button handler to handle 'Enter' key in the 
        // prompt field
        $("#add-property-form").submit(function (e) {
            var buttons = $("#add-property-dialog").dialog( "option", "buttons" );
            buttons[0].click();
            return false;
        })
        .validate({
            rules: {
                "name": "required"
            }/*,
            messages: {
            }*/
        });
        
        //attach a member selection dialog
        $('a#add_member').click( function(e) {
            //initialize the dialog
            $("#add-member-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: UserManager.messages["button.add"],
                        click: function() {
                            var form, name, label, itemHtml, newItem, item;
                            
                            form = $("#add-group-member-form");
                            //client side validation
                            if (!form.valid()) {
                                return false;
                            }
                        
                            //get the selected member info
                            name = $("#memberName").val();
                            label = name;
                            item = $("#memberName").data("item");
                            if (item) {
                                if (item.label && item.value == name) {
                                    label = item.label;
                                }
                            }
                            
                            //inject a new member item into the page
                            itemHtml = '<li><input type="hidden" name=":member" value="' + name + '" /><span>' + label + '</span> <a href="#" class="remove-member" title="' + UserManager.messages['tooltip.removeMember'] + '"><span class="ui-icon ui-icon-circle-close"></span></a></li>';
                            //not empty, so remove the empty message if it is there
                            $("#declaredMembers__empty").hide();
                            $("#declaredMembers").append(itemHtml);
                            
                            newItem = $("#declaredMembers").find('li').last();
                            
                            //add hover states on the remove member icons
                            newItem.find('a.remove-member').hover(
                                function() { $(this).addClass('ui-state-hover'); }, 
                                function() { $(this).removeClass('ui-state-hover'); }
                            ).click(
                                function(e) {
                                    var memberItem = $(e.currentTarget.parentNode);
                                    memberItem.hide('slow', function() {
                                        //haven't submittted to the server yet, so just remove the item from the page
                                        memberItem.remove();
                                        if ($('ol#declaredMembers li:visible').length == 0) {
                                            //after removing the member, the list is empty, so
                                            // show the empty message.
                                            $("#declaredMembers__empty").show();
                                        }
                                    });
                                    return false;
                                }
                            );
                            
                            //close the dialog
                            $("#add-member-dialog").dialog( "close" );
                            return false;
                        }
                    }
                ]
            });
            
            //clear old member value
            $("#memberName").val('');
            
            //open the dialog
            $('#add-member-dialog').dialog('open');
            return false;
        });

        //forward the form submit event to the button handler to handle 'Enter' key in the 
        // prompt field
        $("#add-group-member-form").submit(function (e) {
            var buttons = $("#add-member-dialog").dialog( "option", "buttons" );
            buttons[0].click();
            return false;
        })
        .validate({
            rules: {
                ":member": "required"
            }/*,
            messages: {
            }*/
        });

        //attach an autocomplete handler to the member name field in
        // the add member dialog
        if ($("#memberName").length > 0) {
            $( "#memberName" ).autocomplete({
                source: UserManager.contextPath + "/system/userManager.autocomplete.json",
                minLength: 1,
                select: function(event, ui) {
                    var item = ui.item;
                    $("#memberName")
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
        }
    }
};


/**
 * Holds some common functions for User related pages 
 */
UserManager.User = {
        
};

/**
 * For the User create page.
 */
UserManager.User.Create = {
    /**
     * Initializes the elements for the create User page
     */
    init: function() {
        //apply jquery-ui styles to the create button
        $('button#createUserBtn').button();

        // validate form
        /*var validator = */$("#create-user-form").validate({
            rules: {
                ":name": "required",
                "pwd": "required",
                "pwdConfirm": {
                      equalTo: "#pwd"
                }
            }/*,
            messages: {
            }*/
        });
        
        //attach event handler to the create button
        $('button#createUserBtn').click( function(e) {
            var form, actionUrl, formData;
            
            form = $("#create-user-form");
            
            //client side validation
            if (!form.valid()) {
                return false;
            }
            
            actionUrl = form.attr('action');

            //switch to a json response
            actionUrl = actionUrl.substring(0, actionUrl.length - 4) + "json";

            //turn off redirect in this context
            $('#redirect').attr('disabled', true);
            formData = form.serialize();
            $('#redirect').removeAttr('disabled');

            //hide the info msg from a previous action
            $("#create-user-body div.info-msg-block").hide();

            //submit the create request
            $.ajax({
                url: actionUrl,
                type: 'POST',
                data: formData,
                success: function( data, textStatus, xmlHttpRequest ) {
                    //clear the inputs
                    $("#create-user-form input[type='text']").val('');
                    $("#create-user-form input[type='password']").val('');
    
                    //inject a success message
                    $("#create-user-body span.info-msg-text").html(UserManager.messages["user.created.msg"]);
                    $("#create-user-body div.info-msg-block").show();
                },
                error: UserManager.ErrorDlg.errorHandler
            });
            return false;
        });
    }
};

/**
 * For the User update page.
 */
UserManager.User.Update = {
    /**
     * Initializes the elements for the User update page
     */
    init: function() {
        //apply jquery-ui styles to the update button
        $('button#updateUserBtn').button();
        
        // validate form
        /*var validator = */$("#update-user-form").validate({
            rules: {
            }/*,
            messages: {
            }*/
        });
        
        var disabledRadioFn = function (e) {
    	   var disabled = $('input:radio[name=":disabled"]:checked').val();
    	   if (disabled == "true") {
    		   $("#disabledReasonPanel").show();
    	   } else {
    		   $("#disabledReasonPanel").hide();
    	   }
           return false;
       };
       
       /*
        * Attach event handlers to the status radio buttons
        */
       $('input:radio[name=":disabled"]').change(disabledRadioFn);
       disabledRadioFn();
        
        //hover states on the remove member icons
        $('.remove-property').hover(
            function() { $(this).addClass('ui-state-hover'); }, 
            function() { $(this).removeClass('ui-state-hover'); }
        ).click(
            function(e) {
                var memberItem, input, key;
                
                //mark the member for removal
                memberItem = $(e.currentTarget.parentNode);
                memberItem.hide('slow');
                input = memberItem.find('input');
                key = input.attr("name");
                input.attr("name", key + "@Delete");
                return false;
            }
        );
        
        //attach the event handler to the update button
        $('button#updateUserBtn').click( function(e) {
            var form, actionUrl, formData;
            
            form = $("#update-user-form");

            //client side validation
            if (!form.valid()) {
                return false;
            }

            actionUrl = form.attr('action');

            //switch to a json response
            actionUrl = actionUrl.substring(0, actionUrl.length - 4) + "json";

            //turn off redirect in this context
            $('#redirect').attr('disabled', true);
            formData = form.serialize();
            $('#redirect').removeAttr('disabled');

            //hide the info msg from a previous action
            $("#update-user-body div.info-msg-block").hide();

            //submit the update request
            $.ajax({
                url: actionUrl,
                type: 'POST',
                data: formData,
                success: function( data, textStatus, xmlHttpRequest ) {
                    //reload the update body content
                    $("#update-user-body").parent().load(UserManager.contextPath + data.path + ".update_body.html", function() {
                        //inject a success message
                        $("#update-user-body span.info-msg-text").html(UserManager.messages["user.updated.msg"]);
                        $("#update-user-body div.info-msg-block").show();
                    });

                    setTimeout(function() {
                        //re-init the page since we just replaced the body
                        UserManager.User.Update.init();

                        if ($("#update-password-form").length > 0) {
                            UserManager.User.UpdatePassword.init();
                        }

                        //make visible any elements that require scripting to be enabled 
                        $(".noscript-hide").removeClass("noscript-hide");
                    }, 100);
                },
                error: UserManager.ErrorDlg.errorHandler
            });
            return false;
        });
        
        //attach a confirmation dialog to the 'Remove' link
        $('a#removeUserLink').click( function(e) {
            //prepare the confirmation dialog
            $("#remove-user-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: UserManager.messages["confirm.yes"],
                        click: function() {
                            $("#remove-user-form").submit();
                        } 
                    },
                    {
                        text: UserManager.messages["confirm.no"],
                        click: function() {
                            $("#remove-user-dialog").dialog("close");
                        } 
                    }
                ]
            });

            //open the dialog
            $('#remove-user-dialog').dialog('open');
            return false;
        });

        //attach a prompt dialog to the 'Add Property' link
        $('a#add_property').click( function(e) {
            //prepare the dialog
            $("#add-property-dialog").dialog({
                autoOpen: false,
                height: 'auto',
                width: 350,
                modal: true,
                resizable: false,
                buttons: [
                    {
                        text: UserManager.messages["button.add"],
                        click: function() {
                            var form, name, label, newItem;
                            
                            form = $("#add-property-form");
                            //client side validation
                            if (!form.valid()) {
                                return false;
                            }
                            
                            name=$("#newPropName").val();
                            label = name;
                            $("#updateSubmitBtns").before('<div class="prop-line ui-helper-clearfix"><label for="' + name + '">' + label + ':</label> <input id="' + name + '" type="text" name="' + name + '" /> <a href="#" class="remove-property" title="' + UserManager.messages["tooltip.removeProperty"] + '"><span class="ui-icon ui-icon-circle-close"></span></a></div>');

                            newItem = $("#updateSubmitBtns").prev();
                            
                            //add hover states on the remove property icons
                            newItem.find('a.remove-property').hover(
                                function() { $(this).addClass('ui-state-hover'); }, 
                                function() { $(this).removeClass('ui-state-hover'); }
                            ).click(
                                function(e) {
                                    var propItem = $(e.currentTarget.parentNode);
                                    propItem.hide('slow', function() {
                                        propItem.remove();
                                    });
                                    return false;
                                }
                            );
                            
                            $("#add-property-dialog").dialog( "close" );
                            
                            newItem.find('input').focus();
                        } 
                    }
                ]
            });
            
            
            //clear the old value (if any)
            $("#newPropName").val('');
            
            //open the dialog
            $('#add-property-dialog').dialog('open');
            return false;
        });

        //forward the form submit event to the button handler to handle 'Enter' key in the 
        // prompt field
        $("#add-property-form").submit(function (e) {
            var buttons = $("#add-property-dialog").dialog( "option", "buttons" );
            buttons[0].click();
            return false;
        })
        .validate({    
            rules: {
                "name": "required"
            }/*,
            messages: {
            }*/
        });
    }
};

/**
 * For the User update password page.
 */
UserManager.User.UpdatePassword = {
    /**
     * Initializes the elements for the User update password page
     */
    init: function() {
        //apply jquery-ui styles to the update button
        $('button#updatePasswordBtn').button();

        // validate form
        /*var validator = */$("#update-password-form").validate({
            rules: {
                "oldPwd": "required",
                "newPwd": "required",
                "newPwdConfirm": {
                      equalTo: "#newPwd"
                }
            }/*,
            messages: {
            }*/
        });
        
        //clear input values if the browser auto-filled them.
        $("#update-password-form input[type='password']").val("");
        
        //attach event handler to the update button
        $('button#updatePasswordBtn').click( function(e) {
            var form, actionUrl, formData;
            
            form = $("#update-password-form");

            //client side validation
            if (!form.valid()) {
                return false;
            }
            
            actionUrl = form.attr('action');

            //switch to a json response
            actionUrl = actionUrl.substring(0, actionUrl.length - 4) + "json";

            //turn off redirect in this context
            $('#pwdRedirect').attr('disabled', true);
            formData = form.serialize();
            $('#pwdRedirect').removeAttr('disabled');

            //hide the info msg from a previous action
            $("#update-password-body div.info-msg-block").hide();

            //submit the update request
            $.ajax({
                url: actionUrl,
                type: 'POST',
                data: formData,
                success: function( data, textStatus, xmlHttpRequest ) {
                    //clear the inputs
                    $("#update-password-form input[type='password']").val('');
    
                    //inject a success message
                    $("#update-password-body span.info-msg-text").html(UserManager.messages["user.pwd.updated.msg"]);
                    $("#update-password-body div.info-msg-block").show();
                },
                error: UserManager.ErrorDlg.errorHandler
            });
            return false;
        });
    }
};


/**
 * Initialize any objects that are active on the current page
 */
$(function() {
    //make visible any elements that require scripting to be enabled 
    $(".noscript-hide").removeClass("noscript-hide");
    
    if ($("#sidebar-nav").length > 0) {
        UserManager.SideBar.init();
    }
    
    if ($("#find-authorizables-form").length > 0) {
        UserManager.Authorizables.init();
    }
    
    if ($("#update-group-form").length > 0) {
        UserManager.Group.Update.init();
    }
    
    if ($("#create-group-form").length > 0) {
        UserManager.Group.Create.init();
    }
    
    if ($("#update-user-form").length > 0) {
        UserManager.User.Update.init();
    }
    
    if ($("#update-password-form").length > 0) {
        UserManager.User.UpdatePassword.init();
    }
    
    if ($("#create-user-form").length > 0) {
        UserManager.User.Create.init();
    }
});
