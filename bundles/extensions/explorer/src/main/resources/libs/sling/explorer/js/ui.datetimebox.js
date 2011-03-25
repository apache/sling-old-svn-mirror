/*
* Time functionality added by mhostalacio
*/

(function (d) {
    function DateTimeBox() {
        this.debug = false;
        this._curInst = null;
        this._keyEvent = false;
        this._disabledInputs = [];
        this._inDialog = this._datepickerShowing = false;
        this._mainDivId = "ui-datepicker-div";
        this._inlineClass = "ui-datepicker-inline";
        this._appendClass = "ui-datepicker-append";
        this._triggerClass = "ui-datepicker-trigger";
        this._dialogClass = "ui-datepicker-dialog";
        this._disableClass = "ui-datepicker-disabled";
        this._unselectableClass = "ui-datepicker-unselectable";
        this._currentClass = "ui-datepicker-current-day";
        this._dayOverClass = "ui-datepicker-days-cell-over";
        this.regional = [];
        this.regional[""] =
        {
            firstDay: 0,
            isRTL: false,
            showMonthAfterYear: false,
            yearSuffix: ""
        };
        this._defaults = {
            showOn: "focus",
            showAnim: "",
            showOptions: {},
            defaultDate: null,
            appendText: "",
            buttonText: "...",
            buttonImage: "",
            buttonImageOnly: false,
            hideIfNoPrevNext: false,
            navigationAsDateFormat: false,
            gotoCurrent: false,
            changeMonth: false,
            changeYear: false,
            yearRange: "c-10:c+10",
            showOtherMonths: false,
            selectOtherMonths: false,
            showWeek: false,
            calculateWeek: this.iso8601Week,
            shortYearCutoff: "+10",
            minDate: null,
            maxDate: null,
            duration: "fast",
            beforeShowDay: null,
            beforeShow: null,
            onSelect: null,
            onChangeMonthYear: null,
            onClose: null,
            numberOfMonths: 1,
            showCurrentAtPos: 0,
            stepMonths: 1,
            stepBigMonths: 12,
            altField: "",
            altFormat: "",
            constrainInput: true,
            showButtonPanel: false,
            autoSize: false
        };

        d.extend(this._defaults, this.regional[""]);

        this.dpDiv = d('<div id="' + this._mainDivId + '" class="ui-datepicker ui-widget ui-widget-content ui-helper-clearfix ui-corner-all ui-helper-hidden-accessible"></div>')
    }


    function E(a, b) {
        d.extend(a, b);
        for (var c in b)
            if (b[c] == null || b[c] == undefined)
                a[c] = b[c];
        return a
    }

    d.extend(d.ui, { datetimebox: { version: "1.0.0"} });

    var y = (new Date).getTime();

    d.extend(DateTimeBox.prototype, {
        markerClassName: "hasDatepicker",
        log: function () { this.debug && console.log.apply("", arguments) },
        _widgetDatepicker: function () { return this.dpDiv },
        setDefaults: function (a) {
            E(this._defaults, a || {});
            return this
        },

        _attachDatepicker: function (a, b) {
            var c = null; for (var e in this._defaults) {
                var f = a.getAttribute("date:" + e); if (f) {
                    c = c || {}; try { c[e] = eval(f) } catch (h) {
                        c[e] = f
                    }
                }
            }
            e = a.nodeName.toLowerCase();
            f = e == "div" || e == "span";
            if (!a.id) {
                this.uuid += 1;
                a.id = "dp" + this.uuid
            }
            var i = this._newInst(d(a), f);
            i.settings = d.extend({}, b || {}, c || {});
            if (e == "input")
                this._connectDatepicker(a, i);
            else
                f && this._inlineDatepicker(a, i)
        },

        _newInst: function (a, b) { return { id: a[0].id.replace(/([^A-Za-z0-9_])/g, "\\\\$1"), input: a, selectedDay: 0, selectedMonth: 0, selectedYear: 0, drawMonth: 0, drawYear: 0, inline: b, dpDiv: !b ? this.dpDiv : d('<div class="' + this._inlineClass + ' ui-datepicker ui-widget ui-widget-content ui-helper-clearfix ui-corner-all"></div>'), selectedHour: 0, selectedMinute: 0, drawHour: 0, drawMinute: 0} },

        _connectDatepicker: function (a, b) {
            var c = d(a);
            b.append = d([]);
            b.trigger = d([]);
            if (!c.hasClass(this.markerClassName)) {
                this._attachments(c, b);
                c.addClass(this.markerClassName).keydown(this._doKeyDown).keypress(this._doKeyPress).keyup(this._doKeyUp).bind("setData.datetimebox", function (e, f, h) { b.settings[f] = h }).bind("getData.datetimebox", function (e, f) { return this._get(b, f) });
                this._autoSize(b);
                d.data(a, "datetimebox", b)
            }
        }, 
        
        _attachments: function (a, b) {
            var c = this._get(b, "appendText"), e = this._get(b, "isRTL");
            b.append && b.append.remove();
            if (c) {
                b.append = d('<span class="' + this._appendClass + '">' + c + "</span>");
                a[e ? "before" : "after"](b.append)
            } a.unbind("focus", this._showDatepicker);
            b.trigger && b.trigger.remove();
            c = this._get(b, "showOn");
            if (c == "focus" || c == "both") a.focus(this._showDatepicker);
            if (c == "button" || c == "both") {
                c = this._get(b, "buttonText");
                var f = this._get(b, "buttonImage");
                b.trigger = d(this._get(b, "buttonImageOnly") ? d("<img/>").addClass(this._triggerClass).attr({ src: f, alt: c, title: c }) : d('<button type="button"></button>').addClass(this._triggerClass).html(f ==
"" ? c : d("<img/>").attr({ src: f, alt: c, title: c })));
                a[e ? "before" : "after"](b.trigger);
                b.trigger.click(function () {
                    d.datetimebox._datepickerShowing && d.datetimebox._lastInput == a[0] ? d.datetimebox._hideDatepicker() : d.datetimebox._showDatepicker(a[0]);
                    return false
                })
            }
        },


        _autoSize: function (a) {
            if (this._get(a, "autoSize") && !a.inline) {
                var b = new Date(2009, 11, 20), c = this._get(a, "dateFormat"); if (c.match(/[DM]/)) {
                    var e = function (f) { for (var h = 0, i = 0, g = 0; g < f.length; g++) if (f[g].length > h) { h = f[g].length; i = g } return i }; b.setMonth(e(this._get(a,
c.match(/MM/) ? "monthNames" : "monthNamesShort"))); b.setDate(e(this._get(a, c.match(/DD/) ? "dayNames" : "dayNamesShort")) + 20 - b.getDay())
                } a.input.attr("size", this._formatDateTime(a, b).length)
            }
        },


        _inlineDatepicker: function (a, b) {
            var c = d(a); if (!c.hasClass(this.markerClassName)) {
                c.addClass(this.markerClassName).append(b.dpDiv).bind("setData.datetimebox", function (e, f, h) { b.settings[f] = h }).bind("getData.datetimebox", function (e, f) { return this._get(b, f) }); d.data(a, "datetimebox", b); this._setDate(b, this._getDefaultDate(b),
true); this._updateDatepicker(b); this._updateAlternate(b)
            }
        },


        _dialogDatepicker: function (a, b, c, e, f) {
            a = this._dialogInst; if (!a) { this.uuid += 1; this._dialogInput = d('<input type="text" id="' + ("dp" + this.uuid) + '" style="position: absolute; top: -100px; width: 0px; z-index: -10;"/>'); this._dialogInput.keydown(this._doKeyDown); d("body").append(this._dialogInput); a = this._dialogInst = this._newInst(this._dialogInput, false); a.settings = {}; d.data(this._dialogInput[0], "datetimebox", a) } E(a.settings, e || {}); b = b && b.constructor ==
Date ? this._formatDateTime(a, b) : b; this._dialogInput.val(b); this._pos = f ? f.length ? f : [f.pageX, f.pageY] : null; if (!this._pos) this._pos = [document.documentElement.clientWidth / 2 - 100 + (document.documentElement.scrollLeft || document.body.scrollLeft), document.documentElement.clientHeight / 2 - 150 + (document.documentElement.scrollTop || document.body.scrollTop)]; this._dialogInput.css("left", this._pos[0] + 20 + "px").css("top", this._pos[1] + "px"); a.settings.onSelect = c; this._inDialog = true; this.dpDiv.addClass(this._dialogClass); this._showDatepicker(this._dialogInput[0]);
            d.blockUI && d.blockUI(this.dpDiv); d.data(this._dialogInput[0], "datetimebox", a); return this
        },

        _destroyDatepicker: function (a) { var b = d(a), c = d.data(a, "datetimebox"); if (b.hasClass(this.markerClassName)) { var e = a.nodeName.toLowerCase(); d.removeData(a, "datetimebox"); if (e == "input") { c.append.remove(); c.trigger.remove(); b.removeClass(this.markerClassName).unbind("focus", this._showDatepicker).unbind("keydown", this._doKeyDown).unbind("keypress", this._doKeyPress).unbind("keyup", this._doKeyUp) } else if (e == "div" || e == "span") b.removeClass(this.markerClassName).empty() } },


        _enableDatepicker: function (a) { var b = d(a), c = d.data(a, "datetimebox"); if (b.hasClass(this.markerClassName)) { var e = a.nodeName.toLowerCase(); if (e == "input") { a.disabled = false; c.trigger.filter("button").each(function () { this.disabled = false }).end().filter("img").css({ opacity: "1.0", cursor: "" }) } else if (e == "div" || e == "span") b.children("." + this._inlineClass).children().removeClass("ui-state-disabled"); this._disabledInputs = d.map(this._disabledInputs, function (f) { return f == a ? null : f }) } }, _disableDatepicker: function (a) {
            var b =
d(a), c = d.data(a, "datetimebox"); if (b.hasClass(this.markerClassName)) { var e = a.nodeName.toLowerCase(); if (e == "input") { a.disabled = true; c.trigger.filter("button").each(function () { this.disabled = true }).end().filter("img").css({ opacity: "0.5", cursor: "default" }) } else if (e == "div" || e == "span") b.children("." + this._inlineClass).children().addClass("ui-state-disabled"); this._disabledInputs = d.map(this._disabledInputs, function (f) { return f == a ? null : f }); this._disabledInputs[this._disabledInputs.length] = a }
        },


        _isDisabledDatepicker: function (a) {
            if (!a) return false;
            for (var b = 0; b < this._disabledInputs.length; b++) if (this._disabledInputs[b] == a) return true; return false
        }, 
        
        _getInst: function (a) { try { return d.data(a, "datetimebox") } catch (b) { throw "Missing instance data for this datetimebox"; } }, 
        
        _optionDatepicker: function (a, b, c) {
            var e = this._getInst(a); if (arguments.length == 2 && typeof b == "string") return b == "defaults" ? d.extend({}, d.datetimebox._defaults) : e ? b == "all" ? d.extend({}, e.settings) : this._get(e, b) : null; var f = b || {}; if (typeof b == "string") { f = {}; f[b] = c } if (e) {
                this._curInst == e && this._hideDatepicker(); 
                var h = this._getDateDatepicker(a, true); 
                E(e.settings, f);
                 this._attachments(d(a), e); 
                 this._autoSize(e); 
                 this._setDateDatepicker(a, h); 
                 this._updateDatepicker(e)
            }
        },


        _changeDatepicker: function (a, b, c) { this._optionDatepicker(a, b, c) }, 
        
        _refreshDatepicker: function (a) { (a = this._getInst(a)) && this._updateDatepicker(a) }, 
        
        _setDateDatepicker: function (a, b) { if (a = this._getInst(a)) { this._setDate(a, b); this._updateDatepicker(a); this._updateAlternate(a) } }, 
        
        _getDateDatepicker: function (a, b) {(a = this._getInst(a)) && !a.inline && this._setDateFromField(a, b); return a ? this._getDate(a) : null },


        _doKeyDown: function (a) {
            var b = d.datetimebox._getInst(a.target), c = true, e = b.dpDiv.is(".ui-datepicker-rtl"); b._keyEvent = true; if (d.datetimebox._datepickerShowing) switch (a.keyCode) {
                case 9: d.datetimebox._hideDatepicker(); c = false; break; case 13: c = d("td." + d.datetimebox._dayOverClass, b.dpDiv).add(d("td." + d.datetimebox._currentClass, b.dpDiv)); c[0] ? d.datetimebox._selectDay(a.target, b.selectedMonth, b.selectedYear, b.selectedHour, b.selectedMinute, c[0]) : d.datetimebox._hideDatepicker();
                    return false; case 27: d.datetimebox._hideDatepicker(); break; case 33: d.datetimebox._adjustDate(a.target, a.ctrlKey ? -d.datetimebox._get(b, "stepBigMonths") : -d.datetimebox._get(b, "stepMonths"), "M"); break; case 34: d.datetimebox._adjustDate(a.target, a.ctrlKey ? +d.datetimebox._get(b, "stepBigMonths") : +d.datetimebox._get(b, "stepMonths"), "M"); break; case 35: if (a.ctrlKey || a.metaKey) d.datetimebox._clearDate(a.target); c = a.ctrlKey || a.metaKey; break; case 36: if (a.ctrlKey || a.metaKey) d.datetimebox._gotoToday(a.target); c = a.ctrlKey ||
a.metaKey; break; case 37: if (a.ctrlKey || a.metaKey) d.datetimebox._adjustDate(a.target, e ? +1 : -1, "D"); c = a.ctrlKey || a.metaKey; if (a.originalEvent.altKey) d.datetimebox._adjustDate(a.target, a.ctrlKey ? -d.datetimebox._get(b, "stepBigMonths") : -d.datetimebox._get(b, "stepMonths"), "M"); break; case 38: if (a.ctrlKey || a.metaKey) d.datetimebox._adjustDate(a.target, -7, "D"); c = a.ctrlKey || a.metaKey; break; case 39: if (a.ctrlKey || a.metaKey) d.datetimebox._adjustDate(a.target, e ? -1 : +1, "D"); c = a.ctrlKey || a.metaKey; if (a.originalEvent.altKey) d.datetimebox._adjustDate(a.target,
a.ctrlKey ? +d.datetimebox._get(b, "stepBigMonths") : +d.datetimebox._get(b, "stepMonths"), "M"); break; case 40: if (a.ctrlKey || a.metaKey) d.datetimebox._adjustDate(a.target, +7, "D"); c = a.ctrlKey || a.metaKey; break; default: c = false
            } else if (a.keyCode == 36 && a.ctrlKey) d.datetimebox._showDatepicker(this); else c = false; if (c) { a.preventDefault(); a.stopPropagation() }
        },


        _doKeyPress: function (a) {
            var b = d.datetimebox._getInst(a.target); if (d.datetimebox._get(b, "constrainInput")) {
                b = d.datetimebox._possibleChars(d.datetimebox._get(b, "dateFormat"));
                var c = String.fromCharCode(a.charCode == undefined ? a.keyCode : a.charCode); return a.ctrlKey || c < " " || !b || b.indexOf(c) > -1
            }
        },


        _doKeyUp: function (a) { a = d.datetimebox._getInst(a.target); if (a.input.val() != a.lastVal) try { if (d.datetimebox.parseDate(d.datetimebox._get(a, "dateFormat"), a.input ? a.input.val() : null, d.datetimebox._getFormatConfig(a))) { d.datetimebox._setDateFromField(a); d.datetimebox._updateAlternate(a); d.datetimebox._updateDatepicker(a) } } catch (b) { d.datetimebox.log(b) } return true }, 
        
        _showDatepicker: function (a) {
            a = a.target || a; 
            if (a.nodeName.toLowerCase() != "input") 
                a = d("input", a.parentNode)[0]; 
            if (!(d.datetimebox._isDisabledDatepicker(a) || d.datetimebox._lastInput == a)) 
            {
                var b = d.datetimebox._getInst(a); 
                d.datetimebox._curInst && d.datetimebox._curInst != b && d.datetimebox._curInst.dpDiv.stop(true, true); 
                var c = d.datetimebox._get(b, "beforeShow"); 
                E(b.settings, c ? c.apply(a, [a, b]) : {}); 
                b.lastVal = null; 
                d.datetimebox._lastInput = a; 
                d.datetimebox._setDateFromField(b); 
                if (d.datetimebox._inDialog) a.value = ""; 
                if (!d.datetimebox._pos) {
                    d.datetimebox._pos = d.datetimebox._findPos(a);
                    d.datetimebox._pos[1] += a.offsetHeight
                } 
                var e = false; 
                d(a).parents().each(function () { e |= d(this).css("position") == "fixed"; return !e }); 
                if (e && d.browser.opera) 
                { 
                    d.datetimebox._pos[0] -= document.documentElement.scrollLeft; 
                    d.datetimebox._pos[1] -= document.documentElement.scrollTop 
                } 
                c = { left: d.datetimebox._pos[0], top: d.datetimebox._pos[1] }; 
                d.datetimebox._pos = null; 
                b.dpDiv.css({ position: "absolute", display: "block", top: "-1000px" }); 
                d.datetimebox._updateDatepicker(b); 
                c = d.datetimebox._checkOffset(b, c, e); 
                b.dpDiv.css({ position: d.datetimebox._inDialog && d.blockUI ? "static" : e ? "fixed" : "absolute", display: "none", left: c.left + "px", top: c.top + "px"}); 
                
                if (!b.inline) {
                    c = d.datetimebox._get(b, "showAnim"); 
                    var f = d.datetimebox._get(b, "duration"), h = function () { d.datetimebox._datepickerShowing = true; 
                    var i = d.datetimebox._getBorders(b.dpDiv); 
                    b.dpDiv.find("iframe.ui-datepicker-cover").css({ left: -i[0], top: -i[1], width: b.dpDiv.outerWidth(), height: b.dpDiv.outerHeight() }) }; 
                    b.dpDiv.zIndex(d(a).zIndex() + 1); d.effects && d.effects[c] ? b.dpDiv.show(c, d.datetimebox._get(b, "showOptions"), f, h) : b.dpDiv[c || "show"](c ? f : null, h); 
                    if (!c || !f) 
                        h(); 
                    b.input.is(":visible") && !b.input.is(":disabled") && b.input.focus(); d.datetimebox._curInst = b
                }
            }
        },


        _updateDatepicker: function (a) {
            var b = this;
            var c = d.datetimebox._getBorders(a.dpDiv); 
            a.dpDiv.empty().append(this._generateHTML(a)).find("iframe.ui-datepicker-cover").css({ left: -c[0], top: -c[1], width: a.dpDiv.outerWidth(), height: a.dpDiv.outerHeight() }).end().find("button, .ui-datepicker-prev, .ui-datepicker-next, .ui-datepicker-calendar td a").bind("mouseout", function () {
                d(this).removeClass("ui-state-hover");
                this.className.indexOf("ui-datepicker-prev") != -1 && d(this).removeClass("ui-datepicker-prev-hover"); this.className.indexOf("ui-datepicker-next") != -1 && d(this).removeClass("ui-datepicker-next-hover")
            }).bind("mouseover", function () {
                if (!b._isDisabledDatepicker(a.inline ? a.dpDiv.parent()[0] : a.input[0])) {
                    d(this).parents(".ui-datepicker-calendar").find("a").removeClass("ui-state-hover"); d(this).addClass("ui-state-hover"); this.className.indexOf("ui-datepicker-prev") != -1 && d(this).addClass("ui-datepicker-prev-hover");
                    this.className.indexOf("ui-datepicker-next") != -1 && d(this).addClass("ui-datepicker-next-hover")
                }
            }).end().find("." + this._dayOverClass + " a").trigger("mouseover").end(); 
            c = this._getNumberOfMonths(a); 
            var e = c[1]; 
            e > 1 ? a.dpDiv.addClass("ui-datepicker-multi-" + e).css("width", 17 * e + "em") : a.dpDiv.removeClass("ui-datepicker-multi-2 ui-datepicker-multi-3 ui-datepicker-multi-4").width(""); 
            a.dpDiv[(c[0] != 1 || c[1] != 1 ? "add" : "remove") + "Class"]("ui-datepicker-multi"); 
            a.dpDiv[(this._get(a, "isRTL") ? "add" : "remove") + "Class"]("ui-datepicker-rtl");
            a == d.datetimebox._curInst && d.datetimebox._datepickerShowing && a.input && a.input.is(":visible") && !a.input.is(":disabled") && a.input.focus()
        },


        _getBorders: function (a) { var b = function (c) { return { thin: 1, medium: 2, thick: 3}[c] || c }; return [parseFloat(b(a.css("border-left-width"))), parseFloat(b(a.css("border-top-width")))] }, _checkOffset: function (a, b, c) {
            var e = a.dpDiv.outerWidth(), f = a.dpDiv.outerHeight(), h = a.input ? a.input.outerWidth() : 0, i = a.input ? a.input.outerHeight() : 0, g = document.documentElement.clientWidth + d(document).scrollLeft(),
k = document.documentElement.clientHeight + d(document).scrollTop(); b.left -= this._get(a, "isRTL") ? e - h : 0; b.left -= c && b.left == a.input.offset().left ? d(document).scrollLeft() : 0; b.top -= c && b.top == a.input.offset().top + i ? d(document).scrollTop() : 0; b.left -= Math.min(b.left, b.left + e > g && g > e ? Math.abs(b.left + e - g) : 0); b.top -= Math.min(b.top, b.top + f > k && k > f ? Math.abs(f + i) : 0); return b
        },


        _findPos: function (a) {
            for (var b = this._get(this._getInst(a), "isRTL"); a && (a.type == "hidden" || a.nodeType != 1); ) a = a[b ? "previousSibling" : "nextSibling"];
            a = d(a).offset(); return [a.left, a.top]
        },


        _hideDatepicker: function (a) {
            var b = this._curInst; if (!(!b || a && b != d.data(a, "datepicker"))) if (this._datepickerShowing) {
                a = this._get(b, "showAnim"); var c = this._get(b, "duration"), e = function () { d.datetimebox._tidyDialog(b); this._curInst = null }; d.effects && d.effects[a] ? b.dpDiv.hide(a, d.datetimebox._get(b, "showOptions"), c, e) : b.dpDiv[a == "slideDown" ? "slideUp" : a == "fadeIn" ? "fadeOut" : "hide"](a ? c : null, e); a || e(); if (a = this._get(b, "onClose")) a.apply(b.input ? b.input[0] : null, [b.input ? b.input.val() :
"", b]); this._datepickerShowing = false; this._lastInput = null; if (this._inDialog) { this._dialogInput.css({ position: "absolute", left: "0", top: "-100px" }); if (d.blockUI) { d.unblockUI(); d("body").append(this.dpDiv) } } this._inDialog = false
            }
        },


        _tidyDialog: function (a) { a.dpDiv.removeClass(this._dialogClass).unbind(".ui-datepicker-calendar") }, _checkExternalClick: function (a) {
            if (d.datetimebox._curInst) {
                a = d(a.target); a[0].id != d.datetimebox._mainDivId && a.parents("#" + d.datetimebox._mainDivId).length == 0 && !a.hasClass(d.datetimebox.markerClassName) &&
!a.hasClass(d.datetimebox._triggerClass) && d.datetimebox._datepickerShowing && !(d.datetimebox._inDialog && d.blockUI) && d.datetimebox._hideDatepicker()
            }
        },


        _adjustDate: function (a, b, c) {
            a = d(a);
            var e = this._getInst(a[0]);
            if (!this._isDisabledDatepicker(a[0])) {
                this._adjustInstDate(e, b + (c == "M" ? this._get(e, "showCurrentAtPos") : 0), c);
                this._updateDatepicker(e)
            }
        },



        _gotoToday: function (a) {
            a = d(a); var b = this._getInst(a[0]); if (this._get(b, "gotoCurrent") && b.currentDay) {
                b.selectedDay = b.currentDay;
                b.drawMonth = b.selectedMonth = b.currentMonth;
                b.drawYear = b.selectedYear = b.currentYear;
                b.drawHour = b.selectedHour = b.currentHour;
                b.drawMinute = b.selectedMinute = b.currentMinute
            } else {
                var c = new Date;
                b.selectedDay = c.getDate();
                b.drawMonth = b.selectedMonth = c.getMonth();
                b.drawYear = b.selectedYear = c.getFullYear();
                b.drawHour = b.selectedHour = c.getHours();
                b.drawMinute = b.selectedMinute = c.getMinutes();
            }
            this._notifyChange(b);
            this._adjustDate(a)
        },


        _selectMonthYear: function (a, b, c) {
            a = d(a);
            var e = this._getInst(a[0]);
            e._selectingMonthYear = false;
            e["selected" + (c == "M" ? "Month" : "Year")] = e["draw" + (c == "M" ? "Month" : "Year")] = parseInt(b.options[b.selectedIndex].value, 10);
            this._notifyChange(e);
            this._adjustDate(a)
        },

        _selectHourMinute: function (a, b, c) {
            a = d(a);
            var inst = this._getInst(a[0]);
            inst._selectingHourMinute = false;
            inst["selected" + (c == "h" ? "Hour" : "Minute")] = inst["draw" + (c == "h" ? "Hour" : "Minute")] = parseInt(b.options[b.selectedIndex].value, 10);
            inst.selectedHour = inst.currentHour = inst.drawHour;
            inst.selectedMinute = inst.currentMinute = inst.drawMinute;
            this._selectDate(a, this._formatDateTime(inst, inst.currentDay, inst.currentMonth, inst.currentYear, inst.currentHour, inst.currentMinute));
            this._notifyChange(inst);

        },

        _clickMonthYear: function (a) {
            a = this._getInst(d(a)[0]);
            a.input && a._selectingMonthYear && !d.browser.msie && a.input.focus();
            a._selectingMonthYear = !a._selectingMonthYear
        },

        _clickHourMinute: function (a) {
            a = this._getInst(d(a)[0]);
            a.input && a._selectingHourMinute && !d.browser.msie && a.input.focus();
            a._selectingHourMinute = !a._selectingHourMinute
        },

        _selectDay: function(id, month, year, hour, minute, td) {
            var inst = d(id);
            if (!(d(td).hasClass(this._unselectableClass) || this._isDisabledDatepicker(inst[0]))) {
                inst = this._getInst(inst[0]);
                inst.selectedDay = inst.currentDay = td != null ? d("a", td).html() : inst.currentDay;
                inst.selectedMonth = inst.currentMonth = month != null ? month : inst.currentMonth;
                inst.selectedYear = inst.currentYear = year != null ? year : inst.currentYear;
                inst.selectedHour = inst.currentHour = hour != null ? hour : inst.currentHour;
                inst.selectedMinute = inst.currentMinute = minute != null ? minute : inst.currentMinute;
                this._selectDate(id, this._formatDateTime(inst, inst.currentDay, inst.currentMonth, inst.currentYear, inst.currentHour, inst.currentMinute));
            }
        },


        _clearDate: function (a) {
            a = d(a);
            this._getInst(a[0]);
            this._selectDate(a, "")
        },


        _selectDate: function (a, b) {
            a = this._getInst(d(a)[0]);
            b = b != null ? b : this._formatDateTime(a);
            a.input && a.input.val(b);
            this._updateAlternate(a);
            var c = this._get(a, "onSelect");
            if (c)
                c.apply(a.input ? a.input[0] : null, [b, a]);
            else
                a.input && a.input.trigger("change");
            if (a.inline)
            {
                this._updateDatepicker(a);
            }
            else {
                //this._hideDatepicker();
                this._updateDatepicker(a);
                this._lastInput = a.input[0];
                typeof a.input[0] != "object" && a.input.focus();
                this._lastInput = null
            }
        },


        _updateAlternate: function (a) {
            var b = this._get(a, "altField"); 
            if (b) {
                var c = this._get(a, "altFormat") || this._get(a, "dateFormat"), e = this._getDate(a), f = this.formatDate(c, e, this._getFormatConfig(a)); 
                d(b).each(function () { d(this).val(f) })
            }
        },


        noWeekends: function (a) { a = a.getDay(); return [a > 0 && a < 6, ""] },


        iso8601Week: function (a) {
            a = new Date(a.getTime());
            a.setDate(a.getDate() + 4 - (a.getDay() || 7));
            var b = a.getTime();
            a.setMonth(0); a.setDate(1);
            return Math.floor(Math.round((b - a) / 864E5) / 7) + 1
        }, 
            
        
        parseDate: function (format, value, settings) {
            if (format == null || value == null)
                throw 'Invalid arguments';
            value = (typeof value == 'object' ? value.toString() : value + '');
            if (value == '')
                return null;
            var shortYearCutoff = (settings ? settings.shortYearCutoff : null) || this._defaults.shortYearCutoff;
            var dayNamesShort = (settings ? settings.dayNamesShort : null) || this._defaults.dayNamesShort;
            var dayNames = (settings ? settings.dayNames : null) || this._defaults.dayNames;
            var monthNamesShort = (settings ? settings.monthNamesShort : null) || this._defaults.monthNamesShort;
            var monthNames = (settings ? settings.monthNames : null) || this._defaults.monthNames;
            var year = -1;
            var month = -1;
            var day = -1;
            var hour = -1;
            var minute = -1;
            var literal = false;
            // Check whether a format character is doubled
            var lookAhead = function(match) {
                var matches = (iFormat + 1 < format.length && format.charAt(iFormat + 1) == match);
                if (matches)
                    iFormat++;
                return matches; 
            };
            // Extract a number from the string value
            var getNumber = function(match) {
                lookAhead(match);
                var size = (match == 'y' ? 4 : 2);
                var num = 0;
                while (size > 0 && iValue < value.length &&
                        value.charAt(iValue) >= '0' && value.charAt(iValue) <= '9') {
                    num = num * 10 + (value.charAt(iValue++) - 0);
                    size--;
                }
                if (size == (match == 'y' ? 4 : 2))
                    throw 'Missing number at position ' + iValue;
                return num;
            };
            // Extract a name from the string value and convert to an index
            var getName = function(match, shortNames, longNames) {
                var names = (lookAhead(match) ? longNames : shortNames);
                var size = 0;
                for (var j = 0; j < names.length; j++)
                    size = Math.max(size, names[j].length);
                var name = '';
                var iInit = iValue;
                while (size > 0 && iValue < value.length) {
                    name += value.charAt(iValue++);
                    for (var i = 0; i < names.length; i++)
                        if (name == names[i])
                            return i + 1;
                    size--;
                }
                throw 'Unknown name at position ' + iInit;
            };
            // Confirm that a literal character matches the string value
            var checkLiteral = function() {
                if (value.charAt(iValue) != format.charAt(iFormat))
                    throw 'Unexpected literal at position ' + iValue;
                iValue++;
            };
            var iValue = 0;
            for (var iFormat = 0; iFormat < format.length; iFormat++) {
                if (literal)
                    if (format.charAt(iFormat) == "'" && !lookAhead("'"))
                        literal = false;
                    else
                        checkLiteral();
                else
                    switch (format.charAt(iFormat)) {
                        case 'h':
                            hour = getNumber('h');
                            break;
                        case 'i':
                            minute = getNumber('i');
                            break;
                        case 'd':
                            day = getNumber('d');
                            break;
                        case 'D': 
                            getName('D', dayNamesShort, dayNames);
                            break;
                        case 'm': 
                            month = getNumber('m');
                            break;
                        case 'M':
                            month = getName('M', monthNamesShort, monthNames); 
                            break;
                        case 'y':
                            year = getNumber('y');
                            break;
                        case "'":
                            if (lookAhead("'"))
                                checkLiteral();
                            else
                                literal = true;
                            break;
                        default:
                            checkLiteral();
                    }
            }
            if (year < 100) {
                year += new Date().getFullYear() - new Date().getFullYear() % 100 +
                    (year <= shortYearCutoff ? 0 : -100);
            }
            var date = new Date(year, month - 1, day,hour,minute);
            if (date.getFullYear() != year || date.getMonth() + 1 != month || date.getDate() != day) {
                throw 'Invalid date'; // E.g. 31/02/*
            }
            return date;
        }, 
        
        ATOM: "yy-mm-dd", 
        COOKIE: "D, dd M yy", 
        ISO_8601: "yy-mm-dd", 
        RFC_822: "D, d M y", 
        RFC_850: "DD, dd-M-y", 
        RFC_1036: "D, d M y", 
        RFC_1123: "D, d M yy", 
        RFC_2822: "D, d M yy", 
        RSS: "D, d M y", 
        TICKS: "!", 
        TIMESTAMP: "@", 
        W3C: "yy-mm-dd", 
        _ticksTo1970: (718685 + Math.floor(492.5) - Math.floor(19.7) + Math.floor(4.925)) * 24 * 60 * 60 * 1E7, 
        
        formatDate: function (a, b, c) {
            if (!b) return ""; 
            var e = (c ? c.dayNamesShort : null) || this._defaults.dayNamesShort, f = (c ? c.dayNames : null) || this._defaults.dayNames;
            var h = (c ? c.monthNamesShort : null) || this._defaults.monthNamesShort; 
            c = (c ? c.monthNames : null) || this._defaults.monthNames; 
            var i = function (o) { (o = j + 1 < a.length && a.charAt(j + 1) == o) && j++; return o };
            var g = function (o, m, n) { m = "" + m; if (i(o)) for (; m.length < n; ) m = "0" + m; return m };
            var k = function (o, m, n, r) { return i(o) ? r[m] : n[m] }, l = "", u = false; 
            if (b) 
                for (var j = 0; j < a.length; j++) 
                    if (u) 
                        if (a.charAt(j) == "'" && !i("'")) 
                            u = false; 
                        else 
                            l += a.charAt(j); 
                    else 
                        switch (a.charAt(j)) {
                            case 'h':
                                l += g('h', b.getHours(), 2);
                                break;
                            case 'i':
                                l += g('i', b.getMinutes(), 2);
                                break;
                            case "d": 
                                l += g("d", b.getDate(), 2); 
                                break;
                            case "D": 
                                l += k("D", b.getDay(), e, f); 
                                break; 
                            case "o": 
                                l += g("o", (b.getTime() - (new Date(b.getFullYear(), 0, 0)).getTime()) / 864E5, 3); 
                                break; 
                            case "m": 
                                l += g("m", b.getMonth() + 1, 2); 
                                break; 
                            case "M": 
                                l += k("M", b.getMonth(), h, c); 
                                break; 
                            case "y": 
                                l += i("y") ? b.getFullYear() : (b.getYear() % 100 < 10 ? "0" : "") + b.getYear() % 100; 
                                break; 
                            case "@": 
                                l += b.getTime(); 
                                break; 
                            case "!": 
                                l += b.getTime() * 1E4 + this._ticksTo1970; 
                                break; 
                            case "'": 
                                if (i("'")) 
                                    l += "'"; 
                                else 
                                    u = true; 
                                break; 
                            default: 
                                l += a.charAt(j)
            } return l
        },


        _possibleChars: function (a) {
            for (var b = "", c = false,
e = function (h) { (h = f + 1 < a.length && a.charAt(f + 1) == h) && f++; return h }, f = 0; f < a.length; f++) if (c) if (a.charAt(f) == "'" && !e("'")) c = false; else b += a.charAt(f); else switch (a.charAt(f)) { case "d": case "m": case "y": case "@": b += "0123456789"; break; case "D": case "M": return null; case "'": if (e("'")) b += "'"; else c = true; break; default: b += a.charAt(f) } return b
        },


        _get: function (a, b) { return a.settings[b] !== undefined ? a.settings[b] : this._defaults[b] },
        
        
         _setDateFromField: function (a, b) {
            if (a.input.val() != a.lastVal) {
                var c = this._get(a, "dateFormat");
                var e = a.lastVal = a.input ? a.input.val() : null, f, h; 
                f = h = this._getDefaultDate(a); 
                var i = this._getFormatConfig(a); 
                try { f = this.parseDate(c, e, i) || h } 
                catch (g) { this.log(g); e = b ? "" : e } 
                a.selectedDay = f.getDate(); 
                a.drawMonth = a.selectedMonth = f.getMonth(); 
                a.drawYear = a.selectedYear = f.getFullYear();
                a.drawHour = a.selectedHour = f.getHours();
                a.drawMinute = a.selectedMinute = f.getMinutes(); 
                a.currentDay = e ? f.getDate() : 0; 
                a.currentMonth = e ? f.getMonth() : 0; 
                a.currentYear = e ? f.getFullYear() : 0; 
                a.currentHour = e ? f.getHours() : 0; 
                a.currentMinute = e ? f.getMinutes() : 0; 
                this._adjustInstDate(a)
            }
        },


        _getDefaultDate: function (a) { return this._restrictMinMax(a, this._determineDate(a, this._get(a, "defaultDate"), new Date)) },



        _determineDate: function (a, b, c) {
            var e = function (h) { 
                var i = new Date; 
                i.setDate(i.getDate() + h); 
                return i 
            };
            var f = function (h) 
            {
                try 
                { 
                    return d.datetimebox.parseDate(d.datetimebox._get(a, "dateFormat"), h, d.datetimebox._getFormatConfig(a)) 
                } catch (i) { } 
                var g = (h.toLowerCase().match(/^c/) ? d.datetimebox._getDate(a) : null) || new Date;
                var k = g.getFullYear();
                var l = g.getMonth(); 
                var hour = g.getHours();
                var minute = g.getMinutes();
                g = g.getDate(); 
                for (var u = /([+-]?[0-9]+)\s*(d|D|w|W|m|M|y|Y)?/g, j = u.exec(h); j; ) 
                {
                    switch (j[2] || "d") 
                    {
                        case "d": case "D": 
                            g += parseInt(j[1], 10); 
                            break; 
                        case "w": case "W": 
                            g += parseInt(j[1],10) * 7; 
                            break; 
                        case "m": case "M": 
                            l += parseInt(j[1], 10); 
                            g = Math.min(g, d.datetimebox._getDaysInMonth(k, l)); 
                            break; 
                        case "y": case "Y": 
                            k += parseInt(j[1], 10); 
                            g = Math.min(g, d.datetimebox._getDaysInMonth(k, l)); 
                            break
                    } 
                    j = u.exec(h)
                } 
                return new Date(k, l, g, hour, minute)
            }; 
            if (b = (b = b == null ? c : typeof b == "string" ? f(b) : typeof b == "number" ? isNaN(b) ? c : e(b) : b) && b.toString() == "Invalid Date" ? c : b) 
            { 
                //b.setHours(0); 
                //b.setMinutes(0); 
                //b.setSeconds(0); 
                //b.setMilliseconds(0) 
            } 
            return this._daylightSavingAdjust(b)
        },

        _daylightSavingAdjust: function (a) {
            if (!a) return null;
            //a.setHours(a.getHours() > 12 ? a.getHours() + 2 : 0); return a
            return a
        },


        _setDate: function (a, b, c) { 
            var e = !b, f = a.selectedMonth, h = a.selectedYear; 
            b = this._restrictMinMax(a, this._determineDate(a, b, new Date)); 
            a.selectedDay = a.currentDay = b.getDate();
            a.drawMonth = a.selectedMonth = a.currentMonth = b.getMonth();
            a.drawYear = a.selectedYear = a.currentYear = b.getFullYear(); 
            a.drawHour = a.selectedHour = a.currentHour = b.getHours(); 
            a.drawMinute = a.selectedMinute = a.currentMinute = b.getMinutes(); 
            if ((f != a.selectedMonth || h != a.selectedYear) && !c) 
                this._notifyChange(a); 
                this._adjustInstDate(a); 
                if (a.input) a.input.val(e ? "" : this._formatDateTime(a)) 
         }, 
         
                
         _getDate: function (a) {
            return !a.currentYear || a.input && a.input.val() == "" ? null : this._daylightSavingAdjust(new Date(a.currentYear, a.currentMonth, a.currentDay, a.currentHour, a.currentMinute))
         },


        _generateHTML: function (a) {
            var b = new Date; 
            b = this._daylightSavingAdjust(new Date(b.getFullYear(), b.getMonth(), b.getDate(), b.getHours(), b.getMinutes())); 
            var c = this._get(a, "isRTL");
            var e = this._get(a, "showButtonPanel");
            var f = this._get(a, "hideIfNoPrevNext");
            var h = this._get(a, "navigationAsDateFormat");
            var i = this._getNumberOfMonths(a);
            var g = this._get(a, "showCurrentAtPos");
            var k = this._get(a, "stepMonths");
            var l = i[0] != 1 || i[1] != 1;
            var u = this._daylightSavingAdjust(!a.currentDay ? new Date(9999, 9, 9,dHour,dMin) : new Date(a.currentYear, a.currentMonth, a.currentDay, a.currentHour, a.currentMinute));
            var j = this._getMinMaxDate(a, "min");
            var o = this._getMinMaxDate(a, "max"); 

            g = a.drawMonth - g; 
            var m = a.drawYear; 
            var dHour = a.drawHour;
            var dMin = a.drawMinute;
            if (g < 0) { g += 12; m-- } 
            if (o) 
            { 
                var n = this._daylightSavingAdjust(new Date(o.getFullYear(), o.getMonth() - i[0] * i[1] + 1, o.getDate(), o.getHours(), o.getMinutes())); 
                for (n = j && n < j ? j : n; this._daylightSavingAdjust(new Date(m, g, 1,dHour,dMin)) > n; ) 
                { 
                    g--; 
                    if (g < 0) 
                    { 
                        g = 11; 
                        m-- 
                    } 
                } 
             } 
             a.drawMonth = g; 
             a.drawYear = m; 
             n = this._get(a, "prevText"); 
             n = !h ? n : this.formatDate(n, this._daylightSavingAdjust(new Date(m, g - k, 1, dHour,dMin)), this._getFormatConfig(a));
             n = this._canAdjustMonth(a, -1, m, g) ? '<a class="ui-datepicker-prev ui-corner-all" onclick="DP_jQuery_' + y + ".datetimebox._adjustDate('#" + a.id + "', -" + k + ", 'M');\" title=\"" + n + '"><span class="ui-icon ui-icon-circle-triangle-' + (c ? "e" : "w") + '">' + n + "</span></a>" : f ? "" : '<a class="ui-datepicker-prev ui-corner-all ui-state-disabled" title="' + n + '"><span class="ui-icon ui-icon-circle-triangle-' + (c ? "e" : "w") + '">' + n + "</span></a>"; 
             var r = this._get(a, "nextText"); 
             r = !h ? r : this.formatDate(r, this._daylightSavingAdjust(new Date(m, g + k, 1, dHour, dMin)), this._getFormatConfig(a)); 
             f = this._canAdjustMonth(a, +1, m, g) ? '<a class="ui-datepicker-next ui-corner-all" onclick="DP_jQuery_' + y + ".datetimebox._adjustDate('#" + a.id + "', +" + k + ", 'M');\" title=\"" + r + '"><span class="ui-icon ui-icon-circle-triangle-' + (c ? "w" : "e") + '">' + r + "</span></a>" : f ? "" : '<a class="ui-datepicker-next ui-corner-all ui-state-disabled" title="' + r + '"><span class="ui-icon ui-icon-circle-triangle-' + (c ? "w" : "e") + '">' + r + "</span></a>"; 
             k = this._get(a, "currentText"); 
             r = this._get(a, "gotoCurrent") && a.currentDay ? u : b; k = !h ? k : this.formatDate(k, r, this._getFormatConfig(a)); 
             h = !a.inline ? '<button type="button" class="ui-datepicker-close ui-state-default ui-priority-primary ui-corner-all" onclick="DP_jQuery_' + y + '.datetimebox._hideDatepicker();">' + this._get(a, "closeText") + "</button>" : ""; 
             e = e ? '<div class="ui-datepicker-buttonpane ui-widget-content">' + (c ? h : "") + (this._isInRange(a, r) ? '<button type="button" class="ui-datepicker-current ui-state-default ui-priority-secondary ui-corner-all" onclick="DP_jQuery_' + y + ".datetimebox._gotoToday('#" + a.id + "');\">" + k + "</button>" : "") + (c ? "" : h) + "</div>" : ""; 
             h = parseInt(this._get(a, "firstDay"), 10); 
             h = isNaN(h) ? 0 : h; 
             k = this._get(a, "showWeek"); 
             r = this._get(a, "dayNames"); 
             this._get(a, "dayNamesShort"); 
             var s = this._get(a, "dayNamesMin");
             var z = this._get(a, "monthNames");
             var v = this._get(a, "monthNamesShort");
             var p = this._get(a, "beforeShowDay");
             var w = this._get(a, "showOtherMonths");
             var G = this._get(a, "selectOtherMonths"); 
             this._get(a, "calculateWeek"); 
             for (var K = this._getDefaultDate(a), H = "", C = 0; C < i[0]; C++) 
             {
                for (var L = "", D = 0; D < i[1]; D++) 
                {
                    var M = this._daylightSavingAdjust(new Date(m, g, a.selectedDay));
                    var t = " ui-corner-all";
                    var x = ""; 
                    
                    if (l) 
                    { 
                        x += '<div class="ui-datepicker-group'; 
                        if (i[1] > 1) 
                            switch (D) 
                            { 
                                case 0: 
                                    x += " ui-datepicker-group-first"; 
                                    t = " ui-corner-" + (c ? "right" : "left"); 
                                    break; 
                                case i[1] - 1: 
                                    x += " ui-datepicker-group-last"; 
                                    t = " ui-corner-" + (c ? "left" : "right"); 
                                    break; 
                                default: 
                                    x += " ui-datepicker-group-middle"; 
                                    t = ""; 
                                    break; 
                            } 
                            x += '">' 
                    } 
                    x += '<div class="ui-datepicker-header ui-widget-header ui-helper-clearfix' + t + '">' + (/all|left/.test(t) && C == 0 ? c ? f : n : "") + (/all|right/.test(t) && C == 0 ? c ? n : f : "") + this._generateCloseButton(a) + this._generateMonthYearHeader(a, g, m, j, o, C > 0 || D > 0, z, v,dHour,dMin) + '</div>';
                    x += '<table class="ui-datepicker-calendar"><thead><tr>'; 
                    var A = k ? '<th class="ui-datepicker-week-col">' + this._get(a, "weekHeader") + "</th>" : ""; 
                    for (t = 0; t < 7; t++) 
                    { 
                        var q = (t + h) % 7; 
                        A += "<th" + ((t + h + 6) % 7 >= 5 ? ' class="ui-datepicker-week-end"' : "") + '><span title="' + r[q] + '">' + s[q] + "</span></th>" 
                    } 
                    x += A + "</tr></thead><tbody>"; 
                    A = this._getDaysInMonth(m, g); 
                    if (m == a.selectedYear && g == a.selectedMonth) 
                        a.selectedDay = Math.min(a.selectedDay, A); 
                    t = (this._getFirstDayOfMonth(m, g) - h + 7) % 7; 
                    A = l ? 6 : Math.ceil((t + A) / 7); 
                    q = this._daylightSavingAdjust(new Date(m, g, 1 - t,dHour,dMin)); 
                    for (var N = 0; N < A; N++) 
                    {
                        x += "<tr>"; 
                        var O = !k ? "" : '<td class="ui-datepicker-week-col">' + this._get(a, "calculateWeek")(q) + "</td>"; 
                        for (t = 0; t < 7; t++) 
                        {
                            var F = p ? p.apply(a.input ? a.input[0] : null, [q]) : [true, ""], B = q.getMonth() != g, I = B && !G || !F[0] || j && q < j || o && q > o; 
                            O += '<td class="' + ((t + h + 6) % 7 >= 5 ? " ui-datepicker-week-end" : "");
                            O += (B ? " ui-datepicker-other-month" : "") + (q.getTime() == M.getTime() && g == a.selectedMonth && a._keyEvent || K.getTime() == q.getTime() && K.getTime() == M.getTime() ? " " + this._dayOverClass : "");
                            O += (I ? " " + this._unselectableClass + " ui-state-disabled" : "") + (B && !w ? "" : " " + F[1] + (q.getTime() == u.getTime() ? " " + this._currentClass : "") + (q.getTime() == b.getTime() ? " ui-datepicker-today" : "")) + '"' + ((!B || w) && F[2] ? ' title="' + F[2] + '"' : "") 
                            O += (I ? "" : ' onclick="DP_jQuery_' + y + ".datetimebox._selectDay('#" + a.id + "'," + q.getMonth() + "," + q.getFullYear() + ', null, null, this);return false;"') + ">" + (B && !w ? "&#xa0;" : I ? '<span class="ui-state-default">' + q.getDate() + "</span>" : '<a class="ui-state-default' + (q.getTime() == b.getTime() ? " ui-state-highlight" : "") + (q.getTime() == u.getTime() ? " ui-state-active" : "") + (B ? " ui-priority-secondary" : "") + '" href="#">' + q.getDate() + "</a>") + "</td>"; 
                            q.setDate(q.getDate() + 1); 
                            q = this._daylightSavingAdjust(q)
                        } 
                        x += O + "</tr>"
                    } 
                    g++; 
                    if (g > 11) 
                    { 
                        g = 0; 
                        m++ 
                    } 
                    x += "</tbody></table>" + (l ? "</div>" + (i[0] > 0 && D == i[1] - 1 ? '<div class="ui-datepicker-row-break"></div>' : "") : ""); 
                    L += x
                } 
                H += L
            } 
            H += e + (d.browser.msie && parseInt(d.browser.version, 10) < 7 && !a.inline ? '<iframe src="javascript:false;" class="ui-datepicker-cover" frameborder="0"></iframe>' :""); 
            a._keyEvent = false; 
            return H
        }, 
        
        
        _generateCloseButton: function(a)
        {
            var j = '';
            var txt = this._get(a, "closeText");
            var img = this._get(a, "closeImage");
            j += "<div class='ui-datepicker-close'>"; 
            j += '<a href="javascript:void(0)" onclick="DP_jQuery_' + y + '.datetimebox._hideDatepicker();">';
            if (img)
            {
                j += '<img src="' + img + '" title="' + txt + '"/>';
            }
            else
            {
                j += txt;
            }
            j += '</a>';
            j += "</div>";
            return j
        },

        _generateMonthYearHeader: function (a, b, c, e, f, h, i, g, drawHour,drawMinute) {
            var k = this._get(a, "changeMonth");
            var l = this._get(a, "changeYear");
            var u = this._get(a, "showMonthAfterYear");
            var tm = this._get(a, "timeText");
            var j = '<div class="ui-datepicker-title">';
            var o = ""; 
            if (h || !k) {
                o += '<span class="ui-datepicker-month">' + i[b] + "</span>"; 
            } else {

                //months dropdown
                i = e && e.getFullYear() == c; 
                var m = f && f.getFullYear() == c; 
                o += '<select class="ui-datepicker-month" onchange="DP_jQuery_' + y + ".datetimebox._selectMonthYear('#" + a.id + "', this, 'M');\" onclick=\"DP_jQuery_" + y + ".datetimebox._clickMonthYear('#" + a.id + "');\">"; 
                for (var n = 0; n < 12; n++) 
                {
                    if ((!i || n >= e.getMonth()) && (!m || n <= f.getMonth())) 
                        o += '<option value="' + n + '"' + (n == b ? ' selected="selected"' : "") + ">" + g[n] + "</option>"; 
                }
                o += "</select>"
            } u || (j += o + (h || !(k && l) ? "&#xa0;" : "")); 

            //year dropdown
            if (h || !l) 
                j += '<span class="ui-datepicker-year">' + c + "</span>"; 
            else {
                g = this._get(a, "yearRange").split(":"); 
                var r = (new Date).getFullYear(); 
                i = function (s) { s = s.match(/c[+-].*/) ? c + parseInt(s.substring(1), 10) : s.match(/[+-].*/) ? r + parseInt(s, 10) : parseInt(s, 10); return isNaN(s) ? r : s }; 
                b = i(g[0]); 
                g = Math.max(b, i(g[1] || "")); 
                b = e ? Math.max(b, e.getFullYear()) : b; 
                g = f ? Math.min(g, f.getFullYear()) : g; 
                for (j += '<select class="ui-datepicker-year" onchange="DP_jQuery_' + y + ".datetimebox._selectMonthYear('#" + a.id + "', this, 'Y');\" onclick=\"DP_jQuery_" + y + ".datetimebox._clickMonthYear('#" + a.id + "');\">"; b <= g; b++) 
                {
                    j += '<option value="' + b + '"' + (b == c ? ' selected="selected"' : "") + ">" + b + "</option>"; 
                }
                j += "</select>"
            } 
            
            j += this._get(a, "yearSuffix"); 
            if (u) 
                j += (h || !(k && l) ? "&#xa0;" : "") + o; 

             

            //hour and minute
            j += '<br />';

            j += '<span class="ui-datepicker-time">' + tm + '</span>';
            j += '<select id="'+a.id+'_selectHour" class="ui-datepicker-hour" onchange="DP_jQuery_' + y + ".datetimebox._selectHourMinute('#" + a.id + "', this, 'h');\" onclick=\"DP_jQuery_" + y + ".datetimebox._clickHourMinute('#" + a.id + "');\">";
            for (hour=0; hour < 24; hour++) {
                j += '<option value="' + hour + '"' +
                    (hour == drawHour ? ' selected="selected"' : '') +
                    '>' + ((hour<10)?'0'+hour:hour) + '</option>';
            }
            j += '</select>';
            j += '<span class="ui-datepicker-time">&nbsp;:&nbsp;</span>';
            j += '<select id="'+a.id+'_selectMinute" class="ui-datepicker-minute" onchange="DP_jQuery_' + y + ".datetimebox._selectHourMinute('#" + a.id + "', this, 'i');\" onclick=\"DP_jQuery_" + y + ".datetimebox._clickHourMinute('#" + a.id + "');\">";
            for (minute=0; minute < 60; minute++) {
                j += '<option value="' + minute + '"' +
                    (minute == drawMinute ? ' selected="selected"' : '') +
                    '>' + ((minute<10)?'0'+minute:minute) + '</option>';
            }
            j += '</select>';

            j += "</div>"; 

            return j
        }, 
        
        
        _adjustInstDate: function (a, b, c) {
            var e = a.drawYear + (c == "Y" ? b : 0); 
            var f = a.drawMonth + (c == "M" ? b : 0); 
            b = Math.min(a.selectedDay, this._getDaysInMonth(e, f)) + (c == "D" ? b : 0); 
            var g = a.drawHour + (c == "h" ? b : 0); 
            var h = a.drawMinute + (c == "i" ? b : 0); 

            e = this._restrictMinMax(a, this._daylightSavingAdjust(new Date(e, f, b, g, h, 0, 0))); 
            a.selectedDay = e.getDate(); 
            a.drawMonth = a.selectedMonth = e.getMonth(); 
            a.drawYear = a.selectedYear = e.getFullYear(); 
            a.drawHour = a.selectedHour = e.getHours();
            a.drawMinute = a.selectedMinute = e.getMinutes();
            if (c == "M" || c == "Y" || c == "H" || c == "MM") 
                this._notifyChange(a)
        }, 
        
        
        _restrictMinMax: function (a, b) { 
            var c = this._getMinMaxDate(a, "min"); 
            a = this._getMinMaxDate(a, "max"); 
            b = c && b < c ? c : b; 
            return b = a && b > a ? a : b 
        }, 
            
            
        _notifyChange: function (a) {
            var b = this._get(a, "onChangeMonthYear"); 
            if (b) b.apply(a.input ? a.input[0] : null, [a.selectedYear, a.selectedMonth + 1, a])
        }, 
        
        
        _getNumberOfMonths: function (a) { a = this._get(a, "numberOfMonths"); return a == null ? [1, 1] : typeof a == "number" ? [1, a] : a }, 
        
        _getMinMaxDate: function (a, b) 
        { 
            return this._determineDate(a, this._get(a, b + "Date"), null) 
        }, 
        
        _getDaysInMonth: function (a, b) { return 32 - (new Date(a, b, 32)).getDate() }, _getFirstDayOfMonth: function (a, b) { return (new Date(a, b, 1)).getDay() }, 
        
        _canAdjustMonth: function (a, b, c, e) {
            var f = this._getNumberOfMonths(a);
            c = this._daylightSavingAdjust(new Date(c, e + (b < 0 ? b : f[0] * f[1]), 1)); 
            b < 0 && c.setDate(this._getDaysInMonth(c.getFullYear(), c.getMonth())); 
            return this._isInRange(a, c)
        }, 
        
       
        _isInRange: function (a, b) { 
            var c = this._getMinMaxDate(a, "min"); 
            a = this._getMinMaxDate(a, "max"); 
            return (!c || b.getTime() >= c.getTime()) && (!a || b.getTime() <= a.getTime()) 
        },


        _getFormatConfig: function (a) 
        {
            var b = this._get(a, "shortYearCutoff");
            b = typeof b != "string" ? b : (new Date).getFullYear() % 100 + parseInt(b, 10); 
            return { shortYearCutoff: b, dayNamesShort: this._get(a,"dayNamesShort"), dayNames: this._get(a, "dayNames"), monthNamesShort: this._get(a, "monthNamesShort"), monthNames: this._get(a, "monthNames")
            }
        },

    
        _formatDateTime: function (a, b, c, e, f, g) {
            if (!b) {
                a.currentDay = a.selectedDay;
                a.currentMonth = a.selectedMonth;
                a.currentYear = a.selectedYear;
                a.currentHour = a.selectedHour;
                a.currentMinute = a.selectedMinute
            }
            b = b ? typeof b == "object" ? b : this._daylightSavingAdjust(new Date(e, c, b, f, g)) : this._daylightSavingAdjust(new Date(a.currentYear, a.currentMonth, a.currentDay, a.currentHour, a.currentMinute));
            return this.formatDate(this._get(a, "dateFormat"), b, this._getFormatConfig(a))
        },
  


    });




    d.fn.datetimebox = function (a) {
        if (!d.datetimebox.initialized) {
            d(document).mousedown(d.datetimebox._checkExternalClick).find("body").append(d.datetimebox.dpDiv);
            d.datetimebox.initialized = true
        }
        var b = Array.prototype.slice.call(arguments, 1);
        if (typeof a == "string" && (a == "isDisabled" || a == "getDate" || a == "widget"))
            return d.datetimebox["_" + a + "Datetimebox"].apply(d.datetimebox, [this[0]].concat(b));
        if (a == "option" && arguments.length == 2 && typeof arguments[1] == "string")
            return d.datetimebox["_" + a + "Datetimebox"].apply(d.datetimebox, [this[0]].concat(b));

        return this.each(
            function () { typeof a == "string" ? d.datetimebox["_" + a + "Datetimebox"].apply(d.datetimebox, [this].concat(b)) : d.datetimebox._attachDatepicker(this, a) }
            )
    };
    d.datetimebox = new DateTimeBox;
    d.datetimebox.initialized = false;
    d.datetimebox.uuid = (new Date).getTime();
    d.datetimebox.version = "1.0.0";
    window["DP_jQuery_" + y] = d
})(jQuery);
;