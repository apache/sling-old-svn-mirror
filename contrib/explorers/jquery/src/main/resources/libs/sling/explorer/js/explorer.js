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
 
/** load the initial tree on editor startup */
init_load = function(path){
  
  // load root node
  $.get("/.explorer.item.html", function(data){
    $('#expl_sidebar').append(data);
  });  

  // load others
  var paths = path.split("/");
  paths.splice(0,1); // remove first slash
  var rPath = "";
  for (p in paths){
    rPath += paths[p];
    load_branch(rPath );    
    rPath += "/";
  }
  
  // load properties
  load_props(path);  
}


/** toggle a node in the menu */
explorer_toggle = function(path){
  var id= path.replace(/\//g, "_"); // replacing / with _
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

load_branch = function(path){
  var id= path.replace(/\//g, "_"); // replacing / with _
  
  $('p#'+id+">a").removeAttr('href');  // remove onclick
  
  // fetch children
  $.get("/"+path + ".explorer.item.html", function(data){
    if (data.length > 0) {
      $('p#'+id).parent().addClass('branch'); // add css class
      $('p#'+id).after(data); // add data
      $('p#'+id+">a").attr('href', "#");  // reactivate onclick
      $('p#'+id+">a").addClass('open');  // open
    } 
  });
}

load_props = function(path) {
  $.get(path + ".explorer.properties.html", function(data){
    if (data.length > 0) {
      $('#expl_content').html(data);
    } 
  });
}

add_prop = function(node) {
  var name  = $('#expl_add_prop_name').attr('value');
  var type  = $('#expl_add_prop_type').val();
  var value = $('#expl_add_prop_value').attr('value');

  var params = {};
  params[name + '@TypeHint'] = type;
  params[name] = value;
  $.post(node, params, function(data){ 
    window.location = node + '.explorer.html';
  } );  
}