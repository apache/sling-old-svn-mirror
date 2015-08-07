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

    var logarea;
    var follow = true;
    var searchCriteria = [];
    var logfontsize = 9;
    var refreshInterval = 3000;
    var _load = false;
    var _lineNum = 0;
    var _isLineNumShowing = false;

    var modal;

    var showLine = function(text) {
        logarea.append("<br/>");

        if(text.indexOf("\t") == 0) {
            text = "<span style='padding-left:5%;'>" + text + "</span>";
        }
        else {
            text = "<span>" + text + "</span>"
        }

        for(var i=0; i < searchCriteria.length; i++) {
            if(text.indexOf(searchCriteria[i]["string"]) >=0) {
                if(searchCriteria[i]["bold"]) {text = "<b>" + text + "</b>";}
                if(searchCriteria[i]["italic"]) {text = "<i>" + text + "</i>";}
                var color = "red"; if(searchCriteria[i]["forecolor"]) {color = searchCriteria[i]["forecolor"];}
                var bg = ""; if(searchCriteria[i]["backcolor"]) {bg = searchCriteria[i]["backcolor"];}
                text = "<span style='color:" + color + ";background-color:" + bg + "'>" + text + "</span>";
            }
        }
		_lineNum++;
        logarea.append("<a class='lineNumberCol " + (_isLineNumShowing?"":"hide") + "' name='line_" + _lineNum + "'>&nbsp;&nbsp;&nbsp;" + _lineNum + "&nbsp;&nbsp;&nbsp;</a>" + text);

    };

    var loadTail = function() {
		_load = false;

        $.ajax({
            url: "/system/console/tail",
            data: {},
            dataType: "json",
            method: "GET",
            async: true,
            success: function(s) {
                if(s.content) {
                    $(".loadingstatus").data("status", "active");
                    for(var i = 0; i < s.content.length; i++) {
						var line = s.content[i].line;
                        showLine(line);
                    }
                    if(follow) {
			            $("html,body").scrollTop(logarea[0].scrollHeight);
        			}
                }
                else {
					$(".loadingstatus").data("status", "inactive");
                }
            },
            error: function(e) {
				$(".loadingstatus").data("status", "error");
        	},
            complete: function(d) {
				_load = true;
            }
        });
    };

    var sendCmd = function(cmd, callback) {
        $.ajax({
            url: "/system/console/tail",
            data: {command: cmd},
            dataType: "json",
            method: "POST",
            async: true,
            success: function(s) {},
            error: function(e) {},
            complete: function(d) {
                if(callback) {
                    callback();
                }
            }
        });
    }

    var clearAll = function() {
        logarea.find("span").css({"color":"black", "background-color":"white"});
        var b = logarea[0].getElementsByTagName('b');

        while(b.length) {
            var parent = b[ 0 ].parentNode;
            while( b[ 0 ].firstChild ) {
                parent.insertBefore(  b[ 0 ].firstChild, b[ 0 ] );
            }
            parent.removeChild( b[ 0 ] );
        }

        var i = logarea[0].getElementsByTagName('i');

        while(i.length) {
            var parent = i[ 0 ].parentNode;
            while( i[ 0 ].firstChild ) {
                parent.insertBefore(  i[ 0 ].firstChild, i[ 0 ] );
            }
            parent.removeChild( i[ 0 ] );
        }

    };

    $(document).ready(function(e){
		logarea = $("#logarea");

		if ($("#highlighting").length === 0) {
	        var insertModal = $("<div>", {"class": "", "id": "highlighting", "style": "width:30rem", "title": "Highlighting"}).hide();
			$(document.body).append(insertModal);
			var criteria = "";
            for(var i=0; i < searchCriteria.length; i++) {
                criteria = criteria + "<li class='criteria-item'><div class='box'>" + searchCriteria[i]["string"] + "</div></li>";
        	}

            var content = "<div id='criteria' class='highlight-content-inner-div'><ul class='criteria-list'>" + criteria + "</ul></div>" +
				"<div class='highlight-content-inner-div'><button class='add'>Add</button><button class='delete'>Delete</button></div>" +
                "<div class='highlight-content-inner-div'><input id='search'>String</input></div>" +
                "<div class='highlight-content-inner-div'><input type='checkbox' id='bold' value='off'>Bold</input> <input type='checkbox' id='italic' value='off'>Italic</input></div> " +
                "<div class='highlight-content-inner-div'><input type='color' id='forecolor' value='#FFFFFF'>Foreground Color</input> &nbsp;<input type='color' id='backcolor' value='#ff0000'>Background Color</input></div> ";
            $("#highlighting").append(content);
			var buttonsArr = [];
			buttonsArr.push({
				 text : "OK",
				 click : function() {
				    clearAll();
                    if(searchCriteria.length > 0) {
                    	var logEntries = logarea[0].getElementsByTagName("span");
                        for(var j=0; j<logEntries.length; j++) {
                            var text = logEntries[j].innerHTML;
                            for(var i=0; i < searchCriteria.length; i++) {
                                if(text.indexOf(searchCriteria[i]["string"]) >=0) {
                                    if(searchCriteria[i]["bold"]) {text = "<b>" + text + "</b>";}
                                    if(searchCriteria[i]["italic"]) {text = "<i>" + text + "</i>";}
                                    var color = "red"; if(searchCriteria[i]["forecolor"]) {color = searchCriteria[i]["forecolor"];}
                                    var bg = ""; if(searchCriteria[i]["backcolor"]) {bg = searchCriteria[i]["backcolor"];}
                                    logEntries[j].innerHTML = text;
                                    logEntries[j].style.color = color;
                                    logEntries[j].style.backgroundColor = bg;
                                }
                            }
                        }
                    }
                    $(this).dialog("close");
				 }
			});
			buttonsArr.push({
				 text : "Cancel",
				 click : function() {
					$(this).dialog("close");
				}
			});

            var modal = $("#highlighting").dialog({
                autoOpen: false,
                width: "30rem",
                modal: true,
                buttons: buttonsArr,
                open: function(event, ui) {
                    $(this).data("bkp-load-val", _load);
                    if(_load) {
                        $(".pause").click();
                    }
                },
                close: function(event, ui) {
                    if($(this).data("bkp-load-val") && !_load) {
                        $(".pause").click();
                    }
                    $(this).removeData("bkp-load-val");
                }
            });

            $("#highlighting").find(".add").click(function(e) {
                var val = $("#highlighting").find("#search").val();
                var color = $("#forecolor").val();
                var bg = $("#backcolor").val();
                var b = $("#bold").is(":checked");
                var i = $("#italic").is(":checked");
                var index = searchCriteria.length;
                searchCriteria.push({"string":val, "bold":b, "italic":i, "forecolor":color, "backcolor":bg});
                $("#highlighting").find(".criteria-list").append("<li class='criteria-item' data-index='" + index + "'><span class='box' style='color:"+color+";background-color:"+bg+";font:" + (b?" bold ":"") + (i?" italic ":"") + ";'>" + val + "</span>" + val + "</li>");
                $("#highlighting").find("#search").val("");
                $("#highlighting").find("#bold")[0].checked = false;
                $("#highlighting").find("#italic")[0].checked = false;
            });

            $("#highlighting").find(".delete").click(function(e) {
                var $selected = $("#highlighting").find(".criteria-item.selected");
                if($selected.length == 0) return;

				var index = parseInt($(e.target).data("index"));
				searchCriteria.splice(index, 1);

				$selected.remove();
            });

            $("#highlighting").on("click", ".criteria-item", function(e) {
            	$(e.target).toggleClass("selected").siblings().removeClass("selected");
            });

		}

        $(".highlighting").click(function(e) {
			$("#highlighting").dialog("open");
        });

        $(".clear").click(function(e) {
            logarea.empty();
        });

        $(".refresh").click(function(e) {
			sendCmd("reset");
            _load = false;
            logarea.empty();
			$("#filter").val("");
            document.cookie = "log.tail.position=0";
            _load = true;
        });

        $(".filter").click(function(e) {
            var filterVal = $("#filter").val();
            sendCmd("filter:"+filterVal);
        });

        $(".filterClear").click(function(e) {
            $("#filter").val("");
            $(".filter").click();
        });

		$(".tail").click(function(e) {
            var $elem = $(e.target);
            var currStatus = $elem.data("following");
            $elem.data("following", !currStatus);
            follow = $elem.data("following");
            if(follow) {
				$elem.attr("title", "Unfollow Tail");
				$elem.html("Unfollow");
            }
            else {
                $elem.attr("title", "Follow Tail");
                $elem.html("Follow");
            }
        });

        $(".sizeplus").click(function(e) {
            logfontsize++;
			$(".content").css("font-size", logfontsize+"px");
        });

        $(".sizeminus").click(function(e) {
            logfontsize--;
			$(".content").css("font-size", logfontsize+"px");
        });

        $(".pause").click(function(e) {
            var $elem = $(e.target);
            if(_load) {
                $elem.attr("title", "Click to Resume");
                $elem.html("Resume");

                $(".loadingstatus").data("status", "inactive");
                _load = false;
	        }
            else {
				$elem.attr("title", "Click to Pause");
                $elem.html("Pause");
                _load = true;
            }
        });

        $(".top").click(function(e) {
			$("html,body").scrollTop(0);
            if(follow) {
                $(".tail").click();
            }
        });

        $(".bottom").click(function(e) {
			$("html,body").scrollTop(logarea[0].scrollHeight);
            follow = true;
        });

        $(".numbering").click(function(e) {
			var $elem = $(e.target);
			var currStatus = $elem.data("numbers");
			$elem.data("numbers", !currStatus);
			_isLineNumShowing = $elem.data("numbers");
            if(_isLineNumShowing) {
                $(".lineNumberCol").removeClass("hide");
                $elem.attr("title", "Hide Line Numbers");
                $elem.html("Hide Line No.");
            }
            else {
                $(".lineNumberCol").addClass("hide");
                $elem.attr("title", "Show Line Numbers");
                $elem.html("Show Line No.");
            }
        });

        $(".slower").click(function(e) {
            refreshInterval += 500;
			$("#speed").val(refreshInterval);
        });

        $(".faster").click(function(e) {
            if(refreshInterval >= 1500) {
            	refreshInterval -= 500;
            }
			$("#speed").val(refreshInterval);
        });

        var timerFunc = function(){
            if(_load) {
                loadTail();
            }
        };

        $("#speed").change(function(e) {
			refreshInterval = parseInt($(e.target).val());
            clearInterval(intervalObj);
            intervalObj = setInterval(timerFunc, refreshInterval);
        });

        var intervalObj = setInterval(timerFunc, refreshInterval);

        var getScrollbarWidth = function() {
            var outer = document.createElement("div");
            outer.style.visibility = "hidden";
            outer.style.width = "100px";
            outer.style.msOverflowStyle = "scrollbar"; // needed for WinJS apps

            document.body.appendChild(outer);

            var widthNoScroll = outer.offsetWidth;
            // force scrollbars
            outer.style.overflow = "scroll";

            // add innerdiv
            var inner = document.createElement("div");
            inner.style.width = "100%";
            outer.appendChild(inner);

            var widthWithScroll = inner.offsetWidth;

            // remove divs
            outer.parentNode.removeChild(outer);

            return widthNoScroll - widthWithScroll;
        };

        var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0) - getScrollbarWidth();

        $(".pulldown").width(w);
        $(".header").width(w);
        $(".pulldown").click(function() {
             if($(".header").is(":visible")) {
                $(".header").slideUp();
             }
             else {
                $(".header").slideDown();
                $(".pulldown").attr("title", "Click to hide options");
             }
        });

        var statusOpacity = 0.2;

        setInterval(function() {
            var status = $(".loadingstatus").data("status");
            var color = "grey";
            switch(status) {
                case "error":color="red";break;
                case "inactive":color="grey";break;
                case "active":color="green";break;
                default:color="grey";
            }
            $(".loadingstatus").find("li").css("color", color);
            $(".loadingstatus").find("li").html("<span style='border-radius:10px;'>"+status+"</span>");

            if(status == "active") {
                $(".loadingstatus").fadeTo(1000, statusOpacity);
                if(statusOpacity == 0.2) {
                    statusOpacity = 1.0;
                }
                else {
                    statusOpacity = 0.2;
                }
            }
            else {
                statusOpacity = 1.0;
                $(".loadingstatus").css("opacity", statusOpacity);
            }
        }, 1000);

        $("#logfiles").change(function() {
            var selected = $("#logfiles").val();
            if(selected != "") {
                _load = false;
                sendCmd("file:"+selected, function(){_load=true;});
            }
        });
	});