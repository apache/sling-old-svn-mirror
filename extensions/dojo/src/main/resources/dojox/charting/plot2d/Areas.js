if(!dojo._hasResource["dojox.charting.plot2d.Areas"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.plot2d.Areas"] = true;
dojo.provide("dojox.charting.plot2d.Areas");

dojo.require("dojox.charting.plot2d.Default");

dojo.declare("dojox.charting.plot2d.Areas", dojox.charting.plot2d.Default, {
	constructor: function(){
		this.opt.lines = true;
		this.opt.areas = true;
	}
});

}
