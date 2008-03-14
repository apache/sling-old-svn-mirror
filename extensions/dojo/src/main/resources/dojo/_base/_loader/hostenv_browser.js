if(typeof window != 'undefined'){
	dojo.isBrowser = true;
	dojo._name = "browser";


	// attempt to figure out the path to dojo if it isn't set in the config
	(function(){
		var d = dojo;
		// this is a scope protection closure. We set browser versions and grab
		// the URL we were loaded from here.

		// grab the node we were loaded from
		if(document && document.getElementsByTagName){
			var scripts = document.getElementsByTagName("script");
			var rePkg = /dojo(\.xd)?\.js([\?\.]|$)/i;
			for(var i = 0; i < scripts.length; i++){
				var src = scripts[i].getAttribute("src");
				if(!src){ continue; }
				var m = src.match(rePkg);
				if(m){
					// find out where we came from
					if(!djConfig["baseUrl"]){
						djConfig["baseUrl"] = src.substring(0, m.index);
					}
					// and find out if we need to modify our behavior
					var cfg = scripts[i].getAttribute("djConfig");
					if(cfg){
						var cfgo = eval("({ "+cfg+" })");
						for(var x in cfgo){
							djConfig[x] = cfgo[x];
						}
					}
					break; // "first Dojo wins"
				}
			}
		}
		d.baseUrl = djConfig["baseUrl"];

		// fill in the rendering support information in dojo.render.*
		var n = navigator;
		var dua = n.userAgent;
		var dav = n.appVersion;
		var tv = parseFloat(dav);

		d.isOpera = (dua.indexOf("Opera") >= 0) ? tv : 0;
		d.isKhtml = (dav.indexOf("Konqueror") >= 0)||(dav.indexOf("Safari") >= 0) ? tv : 0;
		if(dav.indexOf("Safari") >= 0){
			d.isSafari = parseFloat(dav.split("Version/")[1]) || 2;
		}
		var geckoPos = dua.indexOf("Gecko");
		d.isMozilla = d.isMoz = ((geckoPos >= 0)&&(!d.isKhtml)) ? tv : 0;
		d.isFF = 0;
		d.isIE = 0;
		try{
			if(d.isMoz){
				d.isFF = parseFloat(dua.split("Firefox/")[1].split(" ")[0]);
			}
			if((document.all)&&(!d.isOpera)){
				d.isIE = parseFloat(dav.split("MSIE ")[1].split(";")[0]);
			}
		}catch(e){}

		//Workaround to get local file loads of dojo to work on IE 7
		//by forcing to not use native xhr.
		if(dojo.isIE && (window.location.protocol === "file:")){
			djConfig.ieForceActiveXXhr=true;
		}

		var cm = document["compatMode"];
		d.isQuirks = (cm == "BackCompat")||(cm == "QuirksMode")||(d.isIE < 6);

		// TODO: is the HTML LANG attribute relevant?
		d.locale = djConfig.locale || (d.isIE ? n.userLanguage : n.language).toLowerCase();

		d._println = console.debug;

		// These are in order of decreasing likelihood; this will change in time.
		d._XMLHTTP_PROGIDS = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'];

		d._xhrObj= function(){
			// summary: 
			//		does the work of portably generating a new XMLHTTPRequest
			//		object.
			var http = null;
			var last_e = null;
			if(!dojo.isIE || !djConfig.ieForceActiveXXhr){
				try{ http = new XMLHttpRequest(); }catch(e){}
			}
			if(!http){
				for(var i=0; i<3; ++i){
					var progid = dojo._XMLHTTP_PROGIDS[i];
					try{
						http = new ActiveXObject(progid);
					}catch(e){
						last_e = e;
					}

					if(http){
						dojo._XMLHTTP_PROGIDS = [progid];  // so faster next time
						break;
					}
				}
			}

			if(!http){
				throw new Error("XMLHTTP not available: "+last_e);
			}

			return http; // XMLHTTPRequest instance
		}

		d._isDocumentOk = function(http){
			var stat = http.status || 0;
			return ( (stat>=200)&&(stat<300))|| 	// Boolean
				(stat==304)|| 						// allow any 2XX response code
				(stat==1223)|| 						// get it out of the cache
				(!stat && (location.protocol=="file:" || location.protocol=="chrome:") ); // Internet Explorer mangled the status code
		}

		//See if base tag is in use.
		//This is to fix http://trac.dojotoolkit.org/ticket/3973,
		//but really, we need to find out how to get rid of the dojo._Url reference
		//below and still have DOH work with the dojo.i18n test following some other
		//test that uses the test frame to load a document (trac #2757).
		//Opera still has problems, but perhaps a larger issue of base tag support
		//with XHR requests (hasBase is true, but the request is still made to document
		//path, not base path).
		var owloc = window.location+"";
		var base = document.getElementsByTagName("base");
		var hasBase = (base && base.length > 0);

		d._getText = function(/*URI*/ uri, /*Boolean*/ fail_ok){
			// summary: Read the contents of the specified uri and return those contents.
			// uri:
			//		A relative or absolute uri. If absolute, it still must be in
			//		the same "domain" as we are.
			// fail_ok:
			//		Default false. If fail_ok and loading fails, return null
			//		instead of throwing.
			// returns: The response text. null is returned when there is a
			//		failure and failure is okay (an exception otherwise)

			// alert("_getText: " + uri);

			// NOTE: must be declared before scope switches ie. this._xhrObj()
			var http = this._xhrObj();

			if(!hasBase && dojo._Url){
				uri = (new dojo._Url(owloc, uri)).toString();
			}
			/*
			console.debug("_getText:", uri);
			console.debug(window.location+"");
			alert(uri);
			*/

			http.open('GET', uri, false);
			try{
				http.send(null);
				// alert(http);
				if(!d._isDocumentOk(http)){
					var err = Error("Unable to load "+uri+" status:"+ http.status);
					err.status = http.status;
					err.responseText = http.responseText;
					throw err;
				}
			}catch(e){
				if(fail_ok){ return null; } // null
				// rethrow the exception
				throw e;
			}
			return http.responseText; // String
		}
	})();

	dojo._initFired = false;
	//	BEGIN DOMContentLoaded, from Dean Edwards (http://dean.edwards.name/weblog/2006/06/again/)
	dojo._loadInit = function(e){
		dojo._initFired = true;
		// allow multiple calls, only first one will take effect
		// A bug in khtml calls events callbacks for document for event which isnt supported
		// for example a created contextmenu event calls DOMContentLoaded, workaround
		var type = (e && e.type) ? e.type.toLowerCase() : "load";
		if(arguments.callee.initialized || (type!="domcontentloaded" && type!="load")){ return; }
		arguments.callee.initialized = true;
		if(typeof dojo["_khtmlTimer"] != 'undefined'){
			clearInterval(dojo._khtmlTimer);
			delete dojo._khtmlTimer;
		}

		if(dojo._inFlightCount == 0){
			dojo._modulesLoaded();
		}
	}

	//	START DOMContentLoaded
	// Mozilla and Opera 9 expose the event we could use
	if(document.addEventListener){
		// NOTE: 
		//		due to a threading issue in Firefox 2.0, we can't enable
		//		DOMContentLoaded on that platform. For more information, see:
		//		http://trac.dojotoolkit.org/ticket/1704
		if(dojo.isOpera|| (dojo.isMoz && (djConfig["enableMozDomContentLoaded"] === true))){
			document.addEventListener("DOMContentLoaded", dojo._loadInit, null);
		}

		//	mainly for Opera 8.5, won't be fired if DOMContentLoaded fired already.
		//  also used for Mozilla because of trac #1640
		window.addEventListener("load", dojo._loadInit, null);
	}

	if(/(WebKit|khtml)/i.test(navigator.userAgent)){ // sniff
		dojo._khtmlTimer = setInterval(function(){
			if(/loaded|complete/.test(document.readyState)){
				dojo._loadInit(); // call the onload handler
			}
		}, 10);
	}
	//	END DOMContentLoaded

	(function(){

		var _w = window;
		var _handleNodeEvent = function(/*String*/evtName, /*Function*/fp){
			// summary:
			//		non-destructively adds the specified function to the node's
			//		evtName handler.
			// evtName: should be in the form "onclick" for "onclick" handlers.
			// Make sure you pass in the "on" part.
			var oldHandler = _w[evtName] || function(){};
			_w[evtName] = function(){
				fp.apply(_w, arguments);
				oldHandler.apply(_w, arguments);
			}
		}

		if(dojo.isIE){
			// 	for Internet Explorer. readyState will not be achieved on init
			// 	call, but dojo doesn't need it however, we'll include it
			// 	because we don't know if there are other functions added that
			// 	might.  Note that this has changed because the build process
			// 	strips all comments -- including conditional ones.

			document.write('<scr'+'ipt defer src="//:" '
				+ 'onreadystatechange="if(this.readyState==\'complete\'){dojo._loadInit();}">'
				+ '</scr'+'ipt>'
			);

			// IE WebControl hosted in an application can fire "beforeunload" and "unload"
			// events when control visibility changes, causing Dojo to unload too soon. The
			// following code fixes the problem
			// Reference: http://support.microsoft.com/default.aspx?scid=kb;en-us;199155
			var _unloading = true;
			_handleNodeEvent("onbeforeunload", function(){
				_w.setTimeout(function(){ _unloading = false; }, 0);
			});
			_handleNodeEvent("onunload", function(){
				if(_unloading){ dojo.unloaded(); }
			});

			try{
				document.namespaces.add("v","urn:schemas-microsoft-com:vml");
				document.createStyleSheet().addRule("v\\:*", "behavior:url(#default#VML)");
			}catch(e){}
		}else{
			// FIXME: dojo.unloaded requires dojo scope, so using anon function wrapper.
			_handleNodeEvent("onbeforeunload", function() { dojo.unloaded(); });
		}

	})();

	/*
	OpenAjax.subscribe("OpenAjax", "onload", function(){
		if(dojo._inFlightCount == 0){
			dojo._modulesLoaded();
		}
	});

	OpenAjax.subscribe("OpenAjax", "onunload", function(){
		dojo.unloaded();
	});
	*/
} //if (typeof window != 'undefined')

//Load debug code if necessary.
// dojo.requireIf((djConfig["isDebug"] || djConfig["debugAtAllCosts"]), "dojo.debug");

//window.widget is for Dashboard detection
//The full conditionals are spelled out to avoid issues during builds.
//Builds may be looking for require/requireIf statements and processing them.
// dojo.requireIf(djConfig["debugAtAllCosts"] && !window.widget && !djConfig["useXDomain"], "dojo.browser_debug");
// dojo.requireIf(djConfig["debugAtAllCosts"] && !window.widget && djConfig["useXDomain"], "dojo.browser_debug_xd");

if(djConfig.isDebug){
		dojo.require("dojo._firebug.firebug");
}

if(djConfig.debugAtAllCosts){
	djConfig.useXDomain = true;
	dojo.require("dojo._base._loader.loader_xd");
	dojo.require("dojo._base._loader.loader_debug");
	dojo.require("dojo.i18n");
}
