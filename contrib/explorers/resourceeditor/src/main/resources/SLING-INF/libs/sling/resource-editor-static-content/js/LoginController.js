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

/*
 * As the name implies, the LoginController contains functionality for the user login.
 */

// creating the namespace
var org = org || {};
org.apache = org.apache || {};
org.apache.sling = org.apache.sling || {};
org.apache.sling.reseditor = org.apache.sling.reseditor || {};

//defining the module
org.apache.sling.reseditor.LoginController = (function() {

	function LoginController(settings, mainController){
		var authorized = settings.authorized;
		$(document).ready(function() {
			setLoginTabLabel(settings.authorizedUser);
			
			$('#login_tab').click(function(e) {	
				if (authorized) {
					//@TODO: Use real <a href="/system/sling/logout.html"... instead
					//make sure the context path is used
					//check if there is a settings.requestURI, if not redirect to "/"
		        	location.href='/system/sling/logout.html?resource='+settings.requestURI;
				} else {
					$('#login_tab_content').slideToggle(function() {mainController.adjust_height();});
					$("#login_form input[name='j_username']").focus();
				}
			});

			$('#login_form input').keydown(function(event) {
		        if (event.keyCode == 13/*Return key*/) {	
		    		submitForm();
		            return false;
		         }
		    });
			
			$('#login_submit').click(function(e) {	
				submitForm();
			});
		});
		

		function setLoginTabLabel(authorizedUser){
			$('#login_tab').text(authorized ? 'Logout '+authorizedUser : authorizedUser);
			if (authorized) {
				$('#login .nav-tabs').removeClass('nav-tabs').addClass('logout');
			}
		}

		function submitForm(){
			$('#login').removeClass('animated shake');
			$('#login .form-group.error').hide();
			
			$.ajax({
		  	  type: 'POST',
				  url: settings.contextPath + $('#login_form').attr('action') + '?' + $('#login_form').serialize(),
		  	  success: function(data, textStatus, jqXHR) {
		  		authorized=true;
		  		$('#login_tab_content').slideToggle(function() {
		  			mainController.adjust_height();
		  			setLoginTabLabel($('#login_form input[name="j_username"]').val());
		  		});
			  },
		  	  error: function(data) {
		  			$('#login_error').text(data.responseText);
		  			$('#login .form-group.error').slideToggle();
		  			$('#login').addClass('animated shake');
			  }
		  	});
		}
	};

	return LoginController;
}());