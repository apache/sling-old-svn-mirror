if(!dojo._hasResource["dojo._base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojo._base"] = true;
dojo.provide("dojo._base");
dojo.require("dojo._base.lang");
dojo.require("dojo._base.declare");
dojo.require("dojo._base.connect");
dojo.require("dojo._base.Deferred");
dojo.require("dojo._base.json");
dojo.require("dojo._base.array");
dojo.require("dojo._base.Color");
dojo.requireIf(dojo.isBrowser, "dojo._base.window");
dojo.requireIf(dojo.isBrowser, "dojo._base.event");
dojo.requireIf(dojo.isBrowser, "dojo._base.html");
dojo.requireIf(dojo.isBrowser, "dojo._base.NodeList");
dojo.requireIf(dojo.isBrowser, "dojo._base.query");
dojo.requireIf(dojo.isBrowser, "dojo._base.xhr");
dojo.requireIf(dojo.isBrowser, "dojo._base.fx");

(function(){
	if(djConfig.require){
		for(var x=0; x<djConfig.require.length; x++){
			dojo["require"](djConfig.require[x]);
		}
	}
})();

}
