/*
* Rhino host environment
*/

if(djConfig["baseUrl"]){
	dojo.baseUrl = djConfig["baseUrl"];
}else{
	dojo.baseUrl = "./";
}

dojo.locale = dojo.locale || String(java.util.Locale.getDefault().toString().replace('_','-').toLowerCase());
dojo._name = 'rhino';
dojo.isRhino = true;

if(typeof print == "function"){
	console.debug = print;
}

if(typeof dojo["byId"] == "undefined"){
	dojo.byId = function(id, doc){
		if(id && (typeof id == "string" || id instanceof String)){
			if(!doc){ doc = document; }
			return doc.getElementById(id);
		}
		return id; // assume it's a node
	}
}

// see comments in spidermonkey loadUri
dojo._loadUri = function(uri, cb){
	try{
		var local = (new java.io.File(uri)).exists();
		if(!local){
			try{
				// try it as a file first, URL second
				var stream = (new java.net.URL(uri)).openStream();
				// close the stream so we don't leak resources
				stream.close();
			}catch(e){
				// no debug output; this failure just means the uri was not found.
				return false;
			}
		}
		//FIXME: Use Rhino 1.6 native readFile/readUrl if available?
		if(cb){
			var contents = (local ? readText : readUri)(uri, "UTF-8");
			cb(eval('('+contents+')'));
		}else{
			load(uri);
		}
		return true;
	}catch(e){
		console.debug("rhino load('" + uri + "') failed. Exception: " + e);
		return false;
	}
}

dojo.exit = function(exitcode){ 
	quit(exitcode);
}

// Hack to determine current script...
//
// These initial attempts failed:
//   1. get an EcmaError and look at e.getSourceName(): try {eval ("static in return")} catch(e) { ...
//   Won't work because NativeGlobal.java only does a put of "name" and "message", not a wrapped reflecting object.
//   Even if the EcmaError object had the sourceName set.
//  
//   2. var e = Packages.org.mozilla.javascript.Context.getCurrentContext().reportError('');
//   Won't work because it goes directly to the errorReporter, not the return value.
//   We want context.interpreterSourceFile and context.interpreterLine, which are used in static Context.getSourcePositionFromStack
//   (set by Interpreter.java at interpretation time, if in interpreter mode).
//
//   3. var e = Packages.org.mozilla.javascript.Context.getCurrentContext().reportRuntimeError('');
//   This returns an object, but e.message still does not have source info.
//   In compiler mode, perhaps not set; in interpreter mode, perhaps not used by errorReporter?
//
// What we found works is to do basically the same hack as is done in getSourcePositionFromStack,
// making a new java.lang.Exception() and then calling printStackTrace on a string stream.
// We have to parse the string for the .js files (different from the java files).
// This only works however in compiled mode (-opt 0 or higher).
// In interpreter mode, entire stack is java.
// When compiled, printStackTrace is like:
// java.lang.Exception
//	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
//	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:39)
//	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:27)
//	at java.lang.reflect.Constructor.newInstance(Constructor.java:274)
//	at org.mozilla.javascript.NativeJavaClass.constructSpecific(NativeJavaClass.java:228)
//	at org.mozilla.javascript.NativeJavaClass.construct(NativeJavaClass.java:185)
//	at org.mozilla.javascript.ScriptRuntime.newObject(ScriptRuntime.java:1269)
//	at org.mozilla.javascript.gen.c2.call(/Users/mda/Sites/burstproject/testrhino.js:27)
//    ...
//	at org.mozilla.javascript.tools.shell.Main.main(Main.java:76)
//
// Note may get different answers based on:
//    Context.setOptimizationLevel(-1)
//    Context.setGeneratingDebug(true)
//    Context.setGeneratingSource(true) 
//
// Some somewhat helpful posts:
//    http://groups.google.com/groups?hl=en&lr=&ie=UTF-8&oe=UTF-8&safe=off&selm=9v9n0g%246gr1%40ripley.netscape.com
//    http://groups.google.com/groups?hl=en&lr=&ie=UTF-8&oe=UTF-8&safe=off&selm=3BAA2DC4.6010702%40atg.com
//
// Note that Rhino1.5R5 added source name information in some exceptions.
// But this seems not to help in command-line Rhino, because Context.java has an error reporter
// so no EvaluationException is thrown.

// do it by using java java.lang.Exception
dojo._rhinoCurrentScriptViaJava = function(depth){
    var optLevel = Packages.org.mozilla.javascript.Context.getCurrentContext().getOptimizationLevel();  
    var caw = new java.io.CharArrayWriter();
    var pw = new java.io.PrintWriter(caw);
    var exc = new java.lang.Exception();
    var s = caw.toString();
    // we have to exclude the ones with or without line numbers because they put double entries in:
    //   at org.mozilla.javascript.gen.c3._c4(/Users/mda/Sites/burstproject/burst/Runtime.js:56)
    //   at org.mozilla.javascript.gen.c3.call(/Users/mda/Sites/burstproject/burst/Runtime.js)
    var matches = s.match(/[^\(]*\.js\)/gi);
    if(!matches){
		throw Error("cannot parse printStackTrace output: " + s);
	}

    // matches[0] is entire string, matches[1] is this function, matches[2] is caller, ...
    var fname = ((typeof depth != 'undefined')&&(depth)) ? matches[depth + 1] : matches[matches.length - 1];
    var fname = matches[3];
	if(!fname){ fname = matches[1]; }
    // print("got fname '" + fname + "' from stack string '" + s + "'");
    if (!fname){ throw Error("could not find js file in printStackTrace output: " + s); }
    //print("Rhino getCurrentScriptURI returning '" + fname + "' from: " + s); 
    return fname;
}

// reading a file from disk in Java is a humiliating experience by any measure.
// Lets avoid that and just get the freaking text
function readText(path, encoding){
	encoding = encoding || "utf-8";
	// NOTE: we intentionally avoid handling exceptions, since the caller will
	// want to know
	var jf = new java.io.File(path);
	var is = new java.io.FileInputStream(jf);
	return dj_readInputStream(is, encoding);
}

function readUri(uri, encoding){
	var conn = (new java.net.URL(uri)).openConnection();
	encoding = encoding || conn.getContentEncoding() || "utf-8";
	var is = conn.getInputStream();
	return dj_readInputStream(is, encoding);
}

function dj_readInputStream(is, encoding){
	var input = new java.io.BufferedReader(new java.io.InputStreamReader(is, encoding));
	try {
		var sb = new java.lang.StringBuffer();
		var line = "";
		while((line = input.readLine()) !== null){
			sb.append(line);
			sb.append(java.lang.System.getProperty("line.separator"));
		}
		return sb.toString();
	} finally {
		input.close();
	}
}

// call this now because later we may not be on the top of the stack
if(!djConfig.libraryScriptUri.length){
	try{
		djConfig.libraryScriptUri = dojo._rhinoCurrentScriptViaJava(1);
	}catch(e){
		// otherwise just fake it
		if(djConfig["isDebug"]){
			print("\n");
			print("we have no idea where Dojo is located.");
			print("Please try loading rhino in a non-interpreted mode or set a");
			print("\n\tdjConfig.libraryScriptUri\n");
			print("Setting the dojo path to './'");
			print("This is probably wrong!");
			print("\n");
			print("Dojo will try to load anyway");
		}
		djConfig.libraryScriptUri = "./";
	}
}

// summary:
//		return the document object associated with the dojo.global
dojo.doc = typeof(document) != "undefined" ? document : null;

dojo.body = function(){
	return document.body;	
}

dojo._timeouts = [];

function clearTimeout(idx){
	if(!dojo._timeouts[idx]){ return; }
	dojo._timeouts[idx].stop();
}

function setTimeout(func, delay){
	// summary: provides timed callbacks using Java threads

	var def={
		sleepTime:delay,
		hasSlept:false,
		
		run:function(){
			if(!this.hasSlept){
				this.hasSlept=true;
				java.lang.Thread.currentThread().sleep(this.sleepTime);
			}
			try{
				func();
			}catch(e){
				console.debug("Error running setTimeout thread:" + e);
			}
		}
	};
	
	var runnable = new java.lang.Runnable(def);
	var thread = new java.lang.Thread(runnable);
	thread.start();
	return dojo._timeouts.push(thread)-1;
}

//Register any module paths set up in djConfig. Need to do this
//in the hostenvs since hostenv_browser can read djConfig from a
//script tag's attribute.
if(djConfig["modulePaths"]){
	for(var param in djConfig["modulePaths"]){
		dojo.registerModulePath(param, djConfig["modulePaths"][param]);
	}
}
