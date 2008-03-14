if(!dojo._hasResource["dojox.dtl._base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl._base"] = true;
dojo.provide("dojox.dtl._base");

dojo.require("dojox.string.Builder");
dojo.require("dojox.string.tokenize");

dojox.dtl.Context = function(dict){
	dojo.mixin(this, dict || {});
	this._dicts = [];
	this._this = {};
}
dojo.extend(dojox.dtl.Context, {
	_dicts: [],
	_this: {},
	extend: function(/*dojox.dtl.Context|Object*/ obj){
		// summary: Returns a clone of this context object, with the items from the
		//		passed objecct mixed in.
		var context = new dojox.dtl.Context();
		var keys = this.getKeys();
		for(var i = 0, key; key = keys[i]; i++){
			if(typeof obj[key] != "undefined"){
				context[key] = obj[key];
			}else{
				context[key] = this[key];
			}
		}

		if(obj instanceof dojox.dtl.Context){
			keys = obj.getKeys();
		}else if(typeof obj == "object"){
			keys = [];
			for(var key in obj){
				keys.push(key);
			}
		}

		for(var i = 0, key; key = keys[i]; i++){
			context[key] = obj[key];
		}

		return context;
	},
	filter: function(/*dojox.dtl.Context|Object|String...*/ filter){
		// summary: Returns a clone of this context, only containing the items
		//		defined in the filter.
		var context = new dojox.dtl.Context();
		var keys = [];
		if(filter instanceof dojox.dtl.Context){
			keys = filter.getKeys();
		}else if(typeof filter == "object"){
			for(var key in filter){
				keys.push(key);
			}
		}else{
			for(var i = 0, arg; arg = arguments[i]; i++){
				if(typeof arg == "string"){
					keys.push(arg);
				}
			}
		}

		for(var i = 0, key; key = keys[i]; i++){
			context[key] = this[key];
		}

		return context;
	},
	setThis: function(/*Object*/ _this){
		this._this = _this;
	},
	getThis: function(){
		return this._this;
	},
	push: function(){
		var dict = {};
		var keys = this.getKeys();
		for(var i = 0, key; key = keys[i]; i++){
			dict[key] = this[key];
			delete this[key];
		}
		this._dicts.unshift(dict);
	},
	pop: function(){
		if(!this._dicts.length){
			throw new Error("pop() has been called more times than push() on the Context");
		}
		var dict = this._dicts.shift();
		dojo.mixin(this, dict);
	},
	hasKey: function(key){
		if(typeof this[key] != "undefined"){
			return true;
		}

		for(var i = 0, dict; dict = this._dicts[i]; i++){
			if(typeof dict[key] != "undefined"){
				return true;
			}
		}

		return false;
	},
	getKeys: function(){
		var keys = [];
		for(var key in this){
			if(isNaN(key)){
				var found = false;
				for(var protoKey in dojox.dtl.Context.prototype){
					if(key == protoKey){
						found = true;
						break;
					}
				}
				if(!found){
					keys.push(key);
				}
			}
		}
		return keys;
	},
	get: function(key, otherwise){
		if(typeof this[key] != "undefined"){
			return this[key];
		}

		for(var i = 0, dict; dict = this._dicts[i]; i++){
			if(typeof dict[key] != "undefined"){
				return dict[key];
			}
		}

		return otherwise;
	},
	update: function(dict){
		this.push();
		if(dict){
			dojo.mixin(this, dict);
		}
	},
	toString: function(){ return "dojox.dtl.Context"; }
});

dojox.dtl.text = {
	types: {tag: -1, varr: -2,	text: 3},
	pySplit: function(str){
		// summary: Split a string according to Python's split function
		str = str.replace(/^\s+|\s+$/, "");
		if(!str.length){
			return [];
		}
		return str.split(/\s+/g);
	},
	urlquote: function(/*String*/ url, /*String?*/ safe){
		if(!safe){
			safe = "/";
		}
		return dojox.string.tokenize(url, /([^\w-_.])/g, function(token){
			if(safe.indexOf(token) == -1){
				if(token == " "){
					return "+";
				}else{
					return "%" + token.charCodeAt(0).toString(16).toUpperCase();
				}
			}
			return token;
		}).join("");
	},
	_get: function(module, name, errorless){
		// summary: Used to find both tags and filters
		var params = dojox.dtl.register.get(module, name, errorless);
		if(!params) return;

		var require = params.getRequire();
		var obj = params.getObj();
		var fn = params.getFn();

		if(fn.indexOf(":") != -1){
			var parts = fn.split(":");
			fn = parts.pop();
		}

		dojo.requireIf(true, require);

		var parent = window;
		var parts = obj.split(".");
		for(var i = 0, part; part = parts[i]; i++){
			if(!parent[part]) return;
			parent = parent[part];
		}
		return parent[fn || name] || parent[name + "_"];
	},
	getTag: function(name, errorless){
		return dojox.dtl.text._get("tag", name, errorless);
	},
	getFilter: function(name, errorless){
		return dojox.dtl.text._get("filter", name, errorless);
	},
	getTemplate: function(file){
		return new dojox.dtl.Template(dojox.dtl.getTemplateString(file));
	},
	getTemplateString: function(file){
		return dojo._getText(file.toString()) || "";
	},
	_re: /(?:\{\{\s*(.+?)\s*\}\}|\{%\s*(.+?)\s*%\})/g,
	tokenize: function(str){
		return dojox.string.tokenize(str, dojox.dtl.text._re, dojox.dtl.text._parseDelims);
	},
	_parseDelims: function(varr, tag){
		var types = dojox.dtl.text.types;
		if(varr){
			return [types.varr, varr];
		}else{
			return [types.tag, tag];
		}
	}
}

dojox.dtl.Template = function(str){
	var st = dojox.dtl;
	var tokens = st.text.tokenize(str);
	var parser = new st.Parser(tokens);
	this.nodelist = parser.parse();
}
dojo.extend(dojox.dtl.Template, {
	render: function(context, /*concatenatable?*/ buffer){
		context = context || new dojox.dtl.Context({});
		if(!buffer){
			dojo.require("dojox.string.Builder");
			buffer = new dojox.string.Builder();
		}
		return this.nodelist.render(context, buffer) + "";
	},
	toString: function(){ return "dojox.dtl.Template"; }
});

dojox.dtl.Filter = function(token){
	// summary: Uses a string to find (and manipulate) a variable
	if(!token) throw new Error("Filter must be called with variable name");
	this.contents = token;
	var key = null;
	var re = this._re;
	var matches, filter, arg, fn;
	var filters = [];
	while(matches = re.exec(token)){
		if(key === null){
			if(this._exists(matches, 3)){
				// variable
				key = matches[3];
			}else if(this._exists(matches, 1)){
				// _("text")
				key = '"' + matches[1] + '"';
			}else if(this._exists(matches, 2)){
				// "text"
				key = '"' + matches[2] + '"';
			}else if(this._exists(matches, 9)){
				// 'text'
				key = '"' + matches[9] + '"';
			}
		}else{
			if(this._exists(matches, 7)){
				// :variable
				arg = [true, matches[7]];
			}else if(this._exists(matches, 5)){
				// :_("text")
				arg = [false, dojox.dtl.replace(matches[5], '\\"', '"')];
			}else if(this._exists(matches, 6)){
				// :"text"
				arg = [false, dojox.dtl.replace(matches[6], '\\"', '"')];
			}else if(this._exists(matches, 8)){
				// :"text"
				arg = [false, dojox.dtl.replace(matches[8], "\\'", "'")];
			}
			// Get a named filter
			fn = dojox.dtl.text.getFilter(matches[4]);
			if(typeof fn != "function") throw new Error(matches[4] + " is not registered as a filter");
			filters.push([fn, arg]);
		}
	}

	this.key = key;
	this.filters = filters;
} 
dojo.extend(dojox.dtl.Filter, {
	_re: /(?:^_\("([^\\"]*(?:\\.[^\\"])*)"\)|^"([^\\"]*(?:\\.[^\\"]*)*)"|^([a-zA-Z0-9_.]+)|\|(\w+)(?::(?:_\("([^\\"]*(?:\\.[^\\"])*)"\)|"([^\\"]*(?:\\.[^\\"]*)*)"|([a-zA-Z0-9_.]+)|'([^\\']*(?:\\.[^\\']*)*)'))?|^'([^\\']*(?:\\.[^\\']*)*)')/g,
	_exists: function(arr, index){
		if(typeof arr[index] != "undefined" && arr[index] !== ""){
			return true;
		}
		return false;
	},
	resolve: function(context){
		var str = this.resolvePath(this.key, context);
		for(var i = 0, filter; filter = this.filters[i]; i++){
			// Each filter has the function in [0], a boolean in [1][0] of whether it's a variable or a string
			// and [1][1] is either the variable name of the string content.
			if(filter[1]){
				if(filter[1][0]){
					str = filter[0](str, this.resolvePath(filter[1][1], context));
				}else{
					str = filter[0](str, filter[1][1]);
				}
			}else{
				str = filter[0](str);
			}
		}
		return str;
	},
	resolvePath: function(path, context){
		var current, parts;
		var first = path.charAt(0);
		var last = path.charAt(path.length - 1);
		if(!isNaN(parseInt(first))){
			current = (path.indexOf(".") == -1) ? parseInt(path) : parseFloat(path);
		}else if(first == '"' && first == last){
			current = path.substring(1, path.length - 1);
		}else{;
			if(path == "true") return true;
			if(path == "false") return false;
			if(path == "null" || path == "None") return null;
			parts = path.split(".");
			current = context.get(parts.shift());
			while(parts.length){
				if(current && typeof current[parts[0]] != "undefined"){
					current = current[parts[0]];
					if(typeof current == "function"){
						if(current.alters_data){
							current = "";
						}else{
							current = current();
						}
					}
				}else{
					return "";
				}
				parts.shift();
			}
		}
		return current;
	},
	toString: function(){ return "dojox.dtl.Filter"; }
});

dojox.dtl.Node = function(/*Object*/ obj){
	// summary: Basic catch-all node
	this.contents = obj;
}
dojo.extend(dojox.dtl.Node, {
	render: function(context, buffer){
		// summary: Adds content onto the buffer
		return buffer.concat(this.contents);
	},
	toString: function(){ return "dojox.dtl.Node"; }
});

dojox.dtl.NodeList = function(/*Node[]*/ nodes){
	// summary: Allows us to render a group of nodes
	this.contents = nodes || [];
}
dojo.extend(dojox.dtl.NodeList, {
	push: function(node){
		// summary: Add a new node to the list
		this.contents.push(node);
	},
	render: function(context, buffer){
		// summary: Adds all content onto the buffer
		for(var i = 0; i < this.contents.length; i++){
			buffer = this.contents[i].render(context, buffer);
			if(!buffer) throw new Error("Template node render functions must return their buffer");
		}
		return buffer;
	},
	unrender: function(context, buffer){ return buffer; },
	clone: function(){ return this; },
	toString: function(){ return "dojox.dtl.NodeList"; }
});

dojox.dtl.TextNode = dojox.dtl.Node;

dojox.dtl.VarNode = function(str){
	// summary: A node to be processed as a variable
	this.contents = new dojox.dtl.Filter(str);
}
dojo.extend(dojox.dtl.VarNode, {
	render: function(context, buffer){
		var str = this.contents.resolve(context);
		return buffer.concat(str);
	},
	toString: function(){ return "dojox.dtl.VarNode"; }
});

dojox.dtl.Parser = function(tokens){
	// summary: Parser used during initialization and for tag groups.
	this.contents = tokens;
}
dojo.extend(dojox.dtl.Parser, {
	parse: function(/*Array?*/ stop_at){
		// summary: Turns tokens into nodes
		// description: Steps into tags are they're found. Blocks use the parse object
		//		to find their closing tag (the stop_at array). stop_at is inclusive, it
		//		returns the node that matched.
		var st = dojox.dtl;
		var types = st.text.types;
		var terminators = {};
		var tokens = this.contents;
		stop_at = stop_at || [];
		for(var i = 0; i < stop_at.length; i++){
			terminators[stop_at[i]] = true;
		}

		var nodelist = new st.NodeList();
		while(tokens.length){
			token = tokens.shift();
			if(typeof token == "string"){
				nodelist.push(new st.TextNode(token));
			}else{
				var type = token[0];
				var text = token[1];
				if(type == types.varr){
					nodelist.push(new st.VarNode(text));
				}else if(type == types.tag){
					if(terminators[text]){
						tokens.unshift(token);
						return nodelist;
					}
					var cmd = text.split(/\s+/g);
					if(cmd.length){
						cmd = cmd[0];
						var fn = dojox.dtl.text.getTag(cmd);
						if(fn){
							nodelist.push(fn(this, text));
						}
					}
				}
			}
		}

		if(stop_at.length){
			throw new Error("Could not find closing tag(s): " + stop_at.toString());
		}

		return nodelist;
	},
	next: function(){
		// summary: Returns the next token in the list.
		var token = this.contents.shift();
		return {type: token[0], text: token[1]};
	},
	skipPast: function(endtag){
		var types = dojox.dtl.text.types;
		while(this.contents.length){
			var token = this.contents.shift();
			if(token[0] == types.tag && token[1] == endtag){
				return;
			}
		}
		throw new Error("Unclosed tag found when looking for " + endtag);
	},
	getVarNode: function(){
		return dojox.dtl.VarNode;
	},
	getTextNode: function(){
		return dojox.dtl.TextNode;
	},
	getTemplate: function(file){
		return new dojox.dtl.Template(file);
	},
	toString: function(){ return "dojox.dtl.Parser"; }
});

dojox.dtl.register = function(module, cols, args, /*Function*/ normalize){
	// summary: Used to create dojox.dtl.register[module] function, and as a namespace
	// expand: Used if the call structure is reformatted for a more compact view.
	//		Should return an array of normalized arguments.
	// description: The function produced will accept a "name"
	//		as the first parameter and all other parameters will
	//		be associated with the parameter names listed in cols.
	var ddr = dojox.dtl.register;
	var registry = ddr._mod[module] = {
		params: [],
		Getter: function(params){
			ddr._params = params || {};
		}
	};

	cols.unshift("name");
	for(var i = 0, col; col = cols[i]; i++){
		registry.Getter.prototype["get" + col.substring(0, 1).toUpperCase() + col.substring(1, col.length)] = ddr._ret(i);
	}

	ddr[module] = function(/*String*/ name, /*mixed...*/ parameters){
		if(normalize){
			var normalized = normalize(arguments);
		}else{
			var normalized = [arguments];
		}

		for(var i = 0, args; args = normalized[i]; i++){
			var params = [];
			for(var j = 0; j < cols.length; j++){
				params.push(args[j] || null);
			}
			if(typeof args[0] == "string"){
				// Strings before regexes for speed
				registry.params.unshift(params);
			}else{
				// break
				// module RegExp
				registry.params.push(params);
			}
		}
	}

	ddr[module].apply(null, args);
}
dojo.mixin(dojox.dtl.register, {
	_mod: {},
	_ret: function(i){
		// summary: Just lets use i and _params within a closure
		return function(){
			return dojox.dtl.register._params[i] || "";
		}
	},
	get: function(/*String*/ module, /*String*/ name, /*Boolean*/ errorless){
		// summary: Returns a "Getter", based on the registry
		// description: The getter functions correspond with the registered cols
		//		used in dojo.register
		var registry = this._mod[module] || {};
		if(registry.params){
			for(var i = 0, param; param = registry.params[i]; i++){
				var search = param[0];
				if(typeof search == "string"){
					if(search == name){
						return new registry.Getter(param);
					}
				}else if(name.match(search)){
					var matches = search.exec(name);
					var mixin = [];
					dojo.mixin(mixin, param);
					mixin[0] = matches[1];
					return new registry.Getter(param);
				}
			}
		}
		if(!errorless) throw new Error("'" + module + "' of name '" + name + "' does not exist");
	},
	_normalize: function(args){
		// summary:
		//		Translates to the signature (/*String*/ name, /*String*/ require, /*String*/ obj, /*String*/ fn)
		var items = args[2];
		var output = [];
		for(var i = 0, item; item = items[i]; i++){
			if(typeof item == "string"){
				output.push([item, args[0], args[1], item]);
			}else{
				output.push([item[0], args[0], args[1], item[1]]);
			}
		}
		return output;
	},
 	tag: function(/*String*/ require, /*String*/ obj, /*String[]|[RegExp, String][]*/ fns){
		// summary:
		//		Specify the location of a given tag function.
		// require:
		//		The file this function is in
		// obj:
		//		The base object to use for lookups
		// fn:
		//		List of functions within obj to use
		// description:
		//		When we are looking up a tag as specified in a template, we either use a
		//		string in the fns array, or the RegExp item of the [RegExp, String] pair.
		//		When that string is found, it requires the file specified in the require
		//		parameter, uses the base object as a starting point and checks for obj.fn
		//		or obj.fn_ in case fn is a reserved word.
		this("tag", ["require", "obj", "fn"], arguments, this._normalize);
	},
	filter: function(/*String*/ require, /*String*/ obj, /*String[]|[RegExp, String][]*/ fns){
		// summary:
		//		Specify the location of a given filter function.
		// require:
		//		The file this function is in
		// obj:
		//		The base object to use for lookups
		// fn:
		//		List of functions within obj to use
		// description:
		//		When we are looking up a tag as specified in a template, we either use a
		//		string in the fns array, or the RegExp item of the [RegExp, String] pair.
		//		When that string is found, it requires the file specified in the require
		//		parameter, uses the base object as a starting point and checks for obj.fn
		//		or obj.fn_ in case fn is a reserved word.
		this("filter", ["require", "obj", "fn"], arguments, this._normalize);
	}
});

(function(){
	var register = dojox.dtl.register;
	var dtt = "dojox.dtl.tag";
	register.tag(dtt + ".logic", dtt + ".logic", ["if", "for"]);
	register.tag(dtt + ".loader", dtt + ".loader", ["extends", "block"]);
	register.tag(dtt + ".misc", dtt + ".misc", ["comment", "debug", "filter"]);
	register.tag(dtt + ".loop", dtt + ".loop", ["cycle"]);

	var dtf = "dojox.dtl.filter";
	register.filter(dtf + ".dates", dtf + ".dates", ["date", "time", "timesince", "timeuntil"]);
	register.filter(dtf + ".htmlstrings", dtf + ".htmlstrings", ["escape", "linebreaks", "linebreaksbr", "removetags", "striptags"]);
	register.filter(dtf + ".integers", dtf + ".integers", ["add", "get_digit"]);
	register.filter(dtf + ".lists", dtf + ".lists", ["dictsort", "dictsortreversed", "first", "join", "length", "length_is", "random", "slice", "unordered_list"]);
	register.filter(dtf + ".logic", dtf + ".logic", ["default", "default_if_none", "divisibleby", "yesno"]);
	register.filter(dtf + ".misc", dtf + ".misc", ["filesizeformat", "pluralize", "phone2numeric", "pprint"]);
	register.filter(dtf + ".strings", dtf + ".strings", ["addslashes", "capfirst", "center", "cut", "fix_ampersands", "floatformat", "iriencode", "linenumbers", "ljust", "lower", "make_list", "rjust", "slugify", "stringformat", "title", "truncatewords", "truncatewords_html", "upper", "urlencode", "urlize", "urlizetrunc", "wordcount", "wordwrap"]);
})();

dojox.dtl.replace = function(str, token, repl){
	repl = repl || "";	
	var pos, len = token.length;
	while(1){
		pos = str.indexOf(token);
		if(pos == -1) break;
		str = str.substring(0, pos) + repl + str.substring(pos + len);
	}
	return str;
}

dojox.dtl.resolveVariable = function(token, context){
	// summary: Quickly resolve a variables
	var filter = new dojox.dtl.Filter(token);
	return filter.resolve(context);
}

}
