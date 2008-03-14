if(!dojo._hasResource["dojox.dtl.filter.dates"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.filter.dates"] = true;
dojo.provide("dojox.dtl.filter.dates");

dojo.require("dojox.dtl.utils.date");

dojo.mixin(dojox.dtl.filter.dates, {
	date: function(value, arg){
		// summary: Formats a date according to the given format
		if(!value || !(value instanceof Date)) return "";
		arg = arg || "N j, Y";
		return dojox.dtl.utils.date.format(value, arg);
	},
	time: function(value, arg){
		// summary: Formats a time according to the given format
		if(!value || !(value instanceof Date)) return "";
		arg = arg || "P";
		return dojox.dtl.utils.date.format(value, arg);
	},
	timesince: function(value, arg){
		// summary: Formats a date as the time since that date (i.e. "4 days, 6 hours")
		var timesince = dojox.dtl.utils.date.timesince;
		if(!value) return "";
		if(arg) return timesince(arg, value);
		return timesince(value);
	},
	timeuntil: function(value, arg){
		// summary: Formats a date as the time until that date (i.e. "4 days, 6 hours")
		var timesince = dojox.dtl.utils.date.timesince;
		if(!value) return "";
		if(arg) return timesince(arg, value);
		return timesince(new Date(), value);
	}
});

}
