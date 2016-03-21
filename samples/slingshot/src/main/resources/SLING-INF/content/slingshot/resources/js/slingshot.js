/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
$( document ).ready(function() {
	$(".form-signin").submit(function(event) {
        event.preventDefault();
        var form = this;
	    var path = form.action;
	    var user = form.j_username.value;
	    var pass = form.j_password.value;

	    // if no user is given, avoid login request
        if (!user) {
            return true;
        }

        // send user/id password to check and persist
        $.ajax({
            url: path + "/j_security_check",
            type: "POST",
            async: false,
            global: false,
            dataType: "text",
            data: {
                _charset_: "utf-8",
                j_username: user,
                j_password: pass
            },
            success: function (data, code, jqXHR){
                var u = form.action;
                if (window.location.hash && u.indexOf('#') < 0) {
                    u = u + window.location.hash;
                }
                document.location = u;
            },
            error: function() {
        		alert("Try username slingshot1 or slingshot2 with the password being the same as the username.");
                form.j_password.value="";
                $(".ui-form-label").css("background-color","red");
            }
        });
        return true;
	});
	$(".form-button-new").click(function() {
		alert("Self registration is not implemented yet. Try username slingshot1 or slingshot2 with the password being the same as the username.");
	});
	$(".form-button-help").click(function() {
		alert("Help function is not implemented yet. Try username slingshot1 or slingshot2 with the password being the same as the username.");
	});
});
