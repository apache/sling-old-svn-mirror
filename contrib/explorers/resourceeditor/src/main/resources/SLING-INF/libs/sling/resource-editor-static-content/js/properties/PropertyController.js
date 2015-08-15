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

// creating the namespace
var org = org || {};
org.apache = org.apache || {};
org.apache.sling = org.apache.sling || {};
org.apache.sling.reseditor = org.apache.sling.reseditor || {};

/*
 * The PropertyController is responsible for the property page functionality.
 */

//defining the module
org.apache.sling.reseditor.PropertyController = (function() {

	function PropertyController(settings, mainController){
		this.mainController = mainController;
		this.settings = settings;
		
		var thisPropertyController = this;
		
		$(document).ready(function() {
			$.notifyDefaults({
				delay: 500,
				template: '<div data-notify="container" class="col-xs-11 col-sm-3 growl-notify alert alert-{0}" role="alert">' +
				'<button type="button" aria-hidden="true" class="close" data-notify="dismiss">×</button>' +
				'<span data-notify="icon"></span> ' +
				'<span data-notify="title">{1}</span> ' +
				'<span data-notify="message">{2}</span>' +
				'<a href="{3}" target="{4}" data-notify="url"></a>' +
			'</div>'
			});

			$( "#node-content" ).on( "click", ".dropdown-menu.add-property-menu li a", function() {
				var dataType = $(this).attr('data-property-type');
				thisPropertyController.openAddPropertyDialog(dataType);
			});
			$('#addPropertyDialog .submit').click(function(){
				thisPropertyController.addProperty();
			});
			$( "#node-content" ).on( "click", ".property-icon.glyphicon-remove", function() {
				var parentRow = $(this).parents(".row:first");
				var propertyKey = parentRow.find(".proplabel").attr("for");
				thisPropertyController.removeProperty(propertyKey, parentRow);
			});
			$( "#node-content" ).on( "click", ".property-icon.glyphicon-save", function() {
				var parentRow = $(this).parents(".row:first");
				var key, value;
				if (parentRow.hasClass('new-property')){
					key = $('#newStringPropertyKey').val();
				} else {
					key = parentRow.find(".proplabel").attr("for");
				}
				value = parentRow.find(".form-control").val();
				
				thisPropertyController.saveProperty(key, value);
			});

			$("#properties-info-icon").on("click", function(e, data) {
				$('#node-content .info-content-container').slideToggle();
			});
			$("#node-content .info-content-container .close").on("click", function(e, data) {
				$('#node-content .info-content-container').slideToggle();
			});
			$( "#node-content" ).on( "keydown", function(event, data) {
		    	// see http://www.javascripter.net/faq/keycodes.htm
				if (event.ctrlKey || event.metaKey) {
					var pressedKey = String.fromCharCode(event.which).toLowerCase();
					var n = 78;
					var s = 83;
					var del = 46;
					switch (event.which){
					    case s:
					    	event.preventDefault();
					    	var parentRow = $( document.activeElement ).parents(".row:first");
					    	var key = parentRow.find(".proplabel").attr("for");
							var value = parentRow.find(".form-control").val();
							thisPropertyController.saveProperty(key, value);
					        break;
					    case del:
					    	event.preventDefault();
					    	var parentRow = $( document.activeElement ).parents(".row:first");
					    	var key = parentRow.find(".proplabel").attr("for");
							thisPropertyController.removeProperty(key, parentRow);
					        break;
					    case n:
					    	event.preventDefault();
					    	$('#node-content .add-property-menu-item.dropdown-toggle').dropdown('toggle');
					        break;
					}
				}
			});
		});
	};
	
	PropertyController.prototype.openAddPropertyDialog = function(dataType){
		$('#addPropertyDialog').modal('show');
		$('#addPropertyDialog .property-editor').hide();
		$("#addPropertyDialog .property-editor[data-property-type='"+dataType+"']").show();
	};
	
	PropertyController.prototype.removeProperty = function(key, row){
		var thisPropertyController = this;
		var confirmationMsg = "You are about to delete the property with the key '"+key+"'. Are you sure?";
		bootbox.confirm(confirmationMsg, function(result) {
			if (result){
				var data = {};
				data[key+"@Delete"] = "";
				$.ajax({
			  	  type: 'POST',
				  url: location.href,
				  dataType: "json",
			  	  data: data
			  	})
				.done(function() {
					row.remove();
					$.notify({
						message: 'Property \''+key+'\' deleted.' 
					},{
						type: 'success'
					});
				})
				.fail(function(errorJson) {
					thisPropertyController.mainController.displayAlert(errorJson);
				});
			}
		});
	};
	
	PropertyController.prototype.saveProperty = function(key, value){
		var thisPropertyController = this;
		var data = {};
//		data[key] = [value,value];
		data[key] = value;
		data["_charset_"] = "utf-8";
		$.ajax({
	  	  type: 'POST',
		  url: location.href,
		  dataType: "json",
	  	  data: data
	  	})
		.done(function() {
			$.notify({
				message: 'Property \''+key+'\' saved.' 
			},{
				type: 'success'
			});
		})
		.fail(function(errorJson) {
			thisPropertyController.mainController.displayAlert(errorJson);
		});
	};

	PropertyController.prototype.addProperty = function(){
		var thisPropertyController = this;
		var key = $('#new-property-key').val();
		var value = $("#addPropertyDialog .property-editor:visible .property-value").val();
		var data = {};
		var propertyType = $("#addPropertyDialog .property-editor:visible").attr("data-property-type");
		data[key] = value;
		data[key+"@TypeHint"] = propertyType;
		data["_charset_"] = "utf-8";
		$.ajax({
	  	  type: 'POST',
	  	  "_charset_": "utf-8",
		  url: location.href,
		  dataType: "json",
	  	  data: data
	  	})
		.done(function() {
			 location.reload();
		})
		.fail(function(errorJson) {
			$('#addPropertyDialog').modal('hide');
			thisPropertyController.mainController.displayAlert(errorJson);
		});
	}
	
	return PropertyController;
}());
