/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
/** replace the default get and post jQuery utility functions */
(function($) {
    Sling.contextPath = "";
    if (Sling.baseurl.lastIndexOf('/') > window.location.protocol.length + 3) {
    	Sling.contextPath = Sling.baseurl.substring(Sling.baseurl.lastIndexOf('/')); 
    };
    
    $.ajaxSetup({
       timeout: 5 * 60 * 1000 // in ms, 5 minutes
    });
    $.postRaw = $.post;
    $.ajaxRaw = $.ajax;
    $.post = function(url, parameters, callback) {
      var params = { "_charset_":"utf-8" };
      $.extend(params, parameters);
      return $.postRaw(Sling.baseurl + url, parameters, callback);
    };
    $.ajax = function(settings) {
    	var url = settings.url;
    	if (url.charAt(0) == '/' && url.indexOf(Sling.contextPath) == -1) {
        	settings.url = Sling.baseurl + url;
    	}
    	if (settings.url && settings.url.charAt(0) == '/') {
    	}
        return $.ajaxRaw(settings);
     };
     
})(jQuery);

/** load the initial tree on editor startup */
init_load = function(path) {
    load_branch(path, function() { load_props(path); } );
    // load properties    
    return;
    // load root node
    $.get("/.explorer.item.html", function( data, textStatus, XMLHttpRequest ) {
        $('#_').append( data );
    });
    
    $.ajax({
        url: path,
        type: 'GET',
        // data: params,
        success: function( data, textStatus, xmlHttpRequest ) {
            try
            {
                // load others
                var paths = path.split("/");
                paths.splice(0, 1); // remove first slash
                var rPath = "";
                for (p in paths) {
                    rPath += paths[p];
                    load_branch(rPath);
                    rPath += "/";
                }

                // load properties
                load_props( path );
            }
            catch(e)
            {
                show_error( e );
            }
        },
        error: function( xmlHttpRequest, textStatus, errorThrown ) {
            show_error( "(" + textStatus + ")" + xmlHttpRequest.responseText );
        }
    });
}

/** toggle a node in the menu */
explorer_toggle = function( path ) {
    var id = path_2_id( path );
    var already_selected = (id == $('#expl_content').data("currently_selected_node"));
    var is_open = $('p#'+id+'>a').hasClass('open');

    if (!already_selected && is_open)
    {
        // just load properties
        load_props( path );
    }
    else
    {
        // toggle          
        if (is_open) {
            // close, i.e. remove children
            $('p#'+id).parent().find('ul').each(function(){
                $(this).empty();
                this.parentNode.removeChild( this )
            });
            $('p#'+id+">a").removeClass('open');  // closed
            $('p#'+id).parent().removeClass('branch'); // remove css class    
            load_props( path );
        } else {
            // just reload properties
            load_branch(path, function(){ load_props( path ); });
        }
    }
}

var loadingbranch = false;
var lastloaded = null;

/** load branch/subtree **/
load_branch = function( path, callback, reload ) {
    if ( loadingbranch ) {
        alert("busy...");
        return;
    }
    if ( path != '' ) 
    {
        loadingbranch = true;
        var id = path_2_id( path );
        $('p#' + id + ">a").removeAttr('href'); // remove onclick

        var uri = path + ".explorer.item.html";
        if (uri[0] != '/')
        {
            uri = '/' + uri;
        };
        // fetch children/subnodes
        $.ajax({
            url: uri,
            type: 'GET',
            success: function( data, textStatus, xmlHttpRequest ) {
                try
                {
                    if ( data.length > 0 ) {
                        $('p#' + id).parent().addClass('branch'); // add css class
                        if ( reload )
                        {
                            $('ul', $('p#' + id).parent()).remove();
                        }
                        $('p#' + id).after( data ); // add data
                        $('p#' + id + ">a").attr('href', "#"); // reactivate onclick
                        $('p#' + id + ">a").addClass('open'); // open
                        $('p#' + id).addClass('loaded');
                        lastloaded  = path;
                        if ( callback ) {
                            loadingbranch = false;
                            callback() 
                        };
                    }
                }
                catch(e)
                {
                    show_error( e + data );
                }

            },
            error: function( xmlHttpRequest, textStatus, errorThrown ) {
                show_error( "(" + textStatus + ")" + xmlHttpRequest.responseText );
            },
            complete: function( xmlHttpRequest, textStatus) {
                loadingbranch = false;
                if ( textStatus == "timeout" )
                {
                    show_error("Timeout!");
                }
            }
        });
    }
}

var currentPath = null;

load_props = function( path ) {
    var id = path_2_id( path );
    $('#expl_content').data("currently_selected_node", id);
    if ( $('p#' + id) )
    {
        $('p', $('#expl_sidebar')).removeClass('selected'); // deselect all
        $('p[id="' + id + '"]').addClass('selected'); // select the current node
    }
    
    $.get( ((path[0]!='/') ? '/' : '') + path + ".explorer.node.html", 
        function( data, textStatus, XMLHttpRequest ) {
            if ( data.length > 0 ) {
                $('#expl_content').html( data );
                currentPath = path;
            }
    });
    // window.location.replace( path );
}

reload_properties = function() {
    load_props(currentPath);
}

add_prop = function( node ) {
    var name = $('#expl_add_prop_name').attr('value');
    var type = $('#expl_add_prop_type').val();
    if ( $('#expl_add_prop_multi').is(':checked') )
    {
        type += '[]';
    }
    var value = $('#expl_add_prop_value').attr('value');

    var params = {};
    params[name + '@TypeHint'] = type;
    params[name] = value;
    $.ajax({
        url: node,
        type: 'POST',
        data: params,
        success: function( data, textStatus, xmlHttpRequest ) {
            try
            {
                reload_properties();
            }
            catch(e)
            {
                show_error( e );
            }

        },
        error: function( xmlHttpRequest, textStatus, errorThrown ) {
            show_error( "(" + textStatus + ")" + xmlHttpRequest.responseText );
        }
    });
}

search = function( language, expression, page ) {
    if ( page != null )
    {
        // $('#sql_search_result').html( "" );
        // adjust_height();
    }
    $('#searchButton').attr("value", "Please Wait...");
    $('#searchButton').attr('disabled', true);
    // search and load search results
    $.ajax({
        url: "/.explorer.search.html",
        type: 'GET',
        data: { "language" : language, "expression" : expression, "page" : page },
        success: function( data, textStatus, xmlHttpRequest ) {
            try
            {
                $('#sql_search_result').html( data );
                adjust_height();
            }
            catch(e)
            {
                show_error( e );
            }

        },
        error: function( xmlHttpRequest, textStatus, errorThrown ) {
            
            show_error( "(" + textStatus + ")" + xmlHttpRequest.responseText );
        },
        complete: function( xmlHttpRequest, textStatus) {
            $('#searchButton').attr("value", "Execute!");
            $('#searchButton').removeAttr('disabled');
        }
    });
}

skip_to = function( path ) {
    expand_tree( path, function() { load_props(path); } );
}

expand_tree = function( path, callback ) {
    // expand tree and select corresponding node    
    var pathParts = path.split('/');
    var partialPath = '';
    for (idx in pathParts)
    {   
        if (pathParts[idx] != '')
        {
            partialPath += (((partialPath != '') ? '/' : '') + pathParts[idx]);
            var id = path_2_id( partialPath ); // replacing / with _
            if (($('p#' + id).length === 0) || !$('p#' + id).hasClass('loaded'))
            {
                if ( partialPath == lastloaded )
                {
                    show_error("failed to expand path " + partialPath);
                }
                else
                {
                    // asynchronous recursion: expand_tree is re-called after ajax call has finished
                    load_branch( partialPath, function() { expand_tree( path, callback ); } );
                }
                return;
            }
        }
    }
    callback();
}

path_2_id = function( path ) {
    // WARNING: have a look at item.esp - duplicate code!
    var id = path.replace(/\//g, "_"); // replacing / with _
    if (path.length > 1)
    {
        id = id.replace(/^_/, ""); // remove trailing _
    }
    id = id.replace(/\./g, '_');// due to the css selectors
    id = id.replace(/\,/g, '_');// due to the css selectors
    id = id.replace(/:/g, '_');// due to the css selectors
    id = id.replace(/\[/g, '_');// due to the css selectors
    id = id.replace(/\]/g, '_');// due to the css selectors
    id = id.replace(/\+/g, '_');// due to the css selectors
    id = id.replace(/\-/g, '_');// due to the css selectors
    id = id.replace(/\(/g, '_');// due to the css selectors
    id = id.replace(/\)/g, '_');// due to the css selectors
    id = id.replace(/\s/g, '_');// due to the css selectors
    id = id.replace(/=/g, '_');// due to the css selectors
    return id;
}

update_credentials = function() {
    var info = Sling.getSessionInfo();
    // alert(info.authType);    
    if ( info )
    {
        $("#username").html(info.userID);
        $("#workspace").html(info.workspace);
        $("#menu_username").html(info.userID);
    }
    if ( info && (info.authType == 'FORM') ) {  
      $("#login").hide();
      $("#logout").show();
      $("#menu_login").hide();
      $("#menu_logout").show();
    } else {
      $("#login").show();
      $("#logout").hide();
      $("#menu_login").show();
      $("#menu_logout").hide();
    }
}

show_error = function(msg) {
    $('#error_dialog').html( msg );
    $('#error_dialog').dialog('open');
}