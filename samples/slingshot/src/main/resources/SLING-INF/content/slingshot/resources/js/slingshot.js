$( document ).ready(function() {
	$(".ui-slingshot-clickable").click(function() { 
	    var $input = $( this );
	    window.location.href=$input.attr("data-link");
    });
	$("#breadcrumbs").breadcrumbs("home");
	$(".breadcrumb_icon_home").click(function() {
		window.location.href=$("#breadcrumbs").attr("data-home");
	});
	$(".ui-form-form").submit(function(event) {
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
                form.j_password.value="";
                $(".ui-form-label").css("background-color","red");
            }
        });
        return true;
	});
	$(".ui-form-button-new").click(function() {
		alert("Self registration is not implemented yet. Try username slingshot1 or slingshot2 with the password being the same as the username.");
	});
	$(".ui-form-button-help").click(function() {
		alert("Help function is not implemented yet. Try username slingshot1 or slingshot2 with the password being the same as the username.");
	});
});
