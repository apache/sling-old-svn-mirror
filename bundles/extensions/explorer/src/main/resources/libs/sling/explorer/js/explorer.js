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
    $.getRaw = $.get;
    $.getJSONRaw = $.getJSON;
    $.postRaw = $.post;
    $.get = function(url, parameters, callback) {
       return $.getRaw(Sling.baseurl + url, parameters, callback)
    };
    $.getJson = function(url, parameters, callback) {
       return $.getJSONRaw(Sling.baseurl + url, parameters, callback)
    };
    $.post = function(url, parameters, callback) {
       return $.postRaw(Sling.baseurl + url, parameters, callback)
    };
})(jQuery);

/** load the initial tree on editor startup */
init_load = function(path, resourceType) {

	// load root node

	$.get("/.explorer.item.html", function( data, textStatus, XMLHttpRequest ) {
		$('#expl_sidebar').append(data);
	});

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
	load_props(path, resourceType);
}

/** toggle a node in the menu */
explorer_toggle = function( path, resourceType ) {
  var id = path_2_id( path ); // replacing / with _
  var is_open = $('p#'+id+'>a').hasClass('open');
  
  load_props("/" + path);
  
  if (is_open){
    //remove children
    $('p#'+id).parent().find('ul').each(function(){
      $(this).empty();
      this.parentNode.removeChild( this )
    });
    $('p#'+id+">a").removeClass('open');  // closed
    $('p#'+id).parent().removeClass('branch'); // remove css class    
  } else {
    load_branch(path);
  }
}

/** load branch/subtree **/
load_branch = function( path, callback, reload ) {
	if (path != '') {
		var id = path_2_id( path );
		$('p#' + id + ">a").removeAttr('href'); // remove onclick

		// fetch children
		var uri = path + ".explorer.item.html";
		if (uri[0] != '/')
		{
			uri = '/' + uri;
		};
		$.get( uri, function( data, textStatus, XMLHttpRequest ) {
			if ( data.length > 0 ) {
				$('p#' + id).parent().addClass('branch'); // add css class
				if ( reload )
				{
					$('ul', $('p#' + id).parent()).remove();
				}
				$('p#' + id).after(data); // add data
				$('p#' + id + ">a").attr('href', "#"); // reactivate onclick
				$('p#' + id + ">a").addClass('open'); // open
				$('p#' + id).addClass('loaded');
				if ( callback ) { callback() };
			}
		});
	}
}

var currentPath = null;
var currentResourceType = null;
load_props = function( path, resourceType ) {
	// check whether currently selected node is on 'path'
	//var currently_selected_node = $('#expl_content').data( "currently_selected_node" );
	//if (currently_selected_node && path == currently_selected_node) return;
	
	$('#expl_content').data("currently_selected_node", path);
	
	var id = path_2_id( path );
	if ($('p#' + id))
	{
		$('p', $('#expl_sidebar')).removeClass('selected'); // deselect all
		$('p[id="' + id + '"]').addClass('selected'); // select the current node
	}
	$.get(path + ".explorer.edit."+ (resourceType == null ? '' : (resourceType.replace(':','_') + '.') ) + "html", 
		function( data, textStatus, XMLHttpRequest ) {
			if ( data.length > 0 ) {
				$('#expl_content').html( data );
				currentPath = path;
				currentResourceType = resourceType;
			}
	});
	// window.location.replace( path );
}

reload_properties = function() {
	load_props(currentPath, currentResourceType);
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
			reload_properties();
		},
		error: function( xmlHttpRequest, textStatus, errorThrown ) {
			show_error( xmlHttpRequest.responseText );
		}
	});
}

search = function( language, expression, page ) {
	// search and load search results
	$.ajax({
		url: "/.explorer.search.html",
		type: 'GET',
		data: { "language" : language, "expression" : expression, "page" : page },
		success: function( data, textStatus, xmlHttpRequest ) {
			$('#sql_search_result').html( data );
			adjust_height();
		},
		error: function( xmlHttpRequest, textStatus, errorThrown ) {
			show_error( xmlHttpRequest.responseText );
		}
	});
}

skip_to = function( path, resourceType ) {
	expand_tree( path, function() { load_props(path, resourceType); } );
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
				// asynchronous recursion: expand_tree is re-called after ajax call has finished
				load_branch( partialPath, function() { expand_tree( path, callback ); } );
				return;
			}
		}
	}
	callback();
}

path_2_id = function(path) {
	// WARNING: have a look at item.esp - duplicate code!
	var id = path.replace(/\//g, "_"); // replacing / with _
	id = id.replace(/^_/, ""); // remove trailing _
	id = id.replace(/\./g, '_');// due to the css selectors
	id = id.replace(/:/g, '_');// due to the css selectors
	id = id.replace(/\[/g, '_');// due to the css selectors
	id = id.replace(/\]/g, '_');// due to the css selectors
	return id;
}

update_credentials = function() {
	var info = Sling.getSessionInfo();
	if ( info )
	{
		document.getElementById("username").innerHTML = info.userID;
		document.getElementById("workspace").innerHTML = info.workspace;
		document.getElementById("menu_username").innerHTML = info.userID;
	}
	if ( info && info.authType ) { 	
	  document.getElementById("login").style.display="none";
	  document.getElementById("logout").style.display="block";
	  document.getElementById("menu_login").style.display="none";
	  document.getElementById("menu_logout").style.display="block";
	} else {	
	  document.getElementById("login").style.display="block";
	  document.getElementById("logout").style.display="none";
	  document.getElementById("menu_login").style.display="block";
	  document.getElementById("menu_logout").style.display="none";
	}
}

show_error = function(msg) {
	$('#error_dialog').html( msg );
	$('#error_dialog').dialog('open');
}