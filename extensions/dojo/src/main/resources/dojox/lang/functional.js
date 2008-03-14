if(!dojo._hasResource["dojox.lang.functional"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.lang.functional"] = true;
dojo.provide("dojox.lang.functional");

// This module adds high-level functions and related constructs:
//	- list comprehensions similar to JavaScript 1.7
//	- anonymous functions built from the string
//	- zip combiners
//	- "reduce" family of functions
//	- currying and partial functions
//	- argument pre-processing: mixer and flip
//	- miscellaneous useful functions

// Acknoledgements:
//	- parts of this module (most notably lambda, constFun, invoke, pluck, and partial) 
//		are based on work by Oliver Steele (http://osteele.com/sources/javascript/functional/functional.js)
//		which was published under MIT License
//	- Simple "maybe" monad was donated by Alex Russell.

// Notes:
//	- Dojo provides following high-level functions in dojo/_base/array.js: 
//		forEach, map, filter, every, some
//	- These functions implemented with optional lambda expression as a parameter.
//	- missing high-level functions are provided with the compatible API: 
//		foldl, foldl1, scanl, scanl1, foldr, foldr1, scanr, scanr1,
//		reduce, reduceRight
//	- lambda() and listcomp() produce functions, which after the compilation step are 
//		as fast as regular JS functions (at least theoretically).

(function(){
	var d = dojo, df = dojox.lang.functional, g_re = /\bfor\b|\bif\b/gm, empty = {};
	
	// split() is augmented on IE6 to ensure the uniform behavior
	var split = "ab".split(/a*/).length > 1 ? String.prototype.split :
			function(sep){
				 var r = this.split.call(this, sep),
					 m = sep.exec(this);
				 if(m && m.index == 0){ r.unshift(""); }
				 return r;
			};
	var lambda = function(/*String*/ s){
		var args = [], sects = split.call(s, /\s*->\s*/m);
		if(sects.length > 1){
			while(sects.length){
				s = sects.pop();
				args = sects.pop().split(/\s*,\s*|\s+/m);
				if(sects.length){ sects.push("(function(" + args + "){return (" + s + ")})"); }
			}
		} else if(s.match(/\b_\b/)) {
			args = ["_"];
		} else {
			var l = s.match(/^\s*(?:[+*\/%&|\^\.=<>]|!=)/m),
				r = s.match(/[+\-*\/%&|\^\.=<>!]\s*$/m);
			if(l || r){
				if(l){
					args.push("$1");
					s = "$1" + s;
				}
				if(r){
					args.push("$2");
					s = s + "$2";
				}
			} else {
				var vars = s.
					replace(/(?:\b[A-Z]|\.[a-zA-Z_$])[a-zA-Z_$\d]*|[a-zA-Z_$][a-zA-Z_$\d]*:|this|true|false|null|undefined|typeof|instanceof|in|delete|new|void|arguments|decodeURI|decodeURIComponent|encodeURI|encodeURIComponent|escape|eval|isFinite|isNaN|parseFloat|parseInt|unescape|dojo|dijit|dojox|'(?:[^'\\]|\\.)*'|"(?:[^"\\]|\\.)*"/g, "").
					match(/([a-z_$][a-z_$\d]*)/gi) || [];
				var t = {};
				d.forEach(vars, function(v){
					if(!(v in t)){
						args.push(v);
						t[v] = 1;
					}
				});
			}
		}
		return {args: args, body: "return (" + s + ");"};	// Object
	};
	
	var listcomp = function(/*String*/ s){
		var frag = s.split(g_re), act = s.match(g_re),
			head = ["var r = [];"], tail = [];
		for(var i = 0; i < act.length;){
			var a = act[i], f = frag[++i];
			if(a == "for" && !/^\s*\(\s*(;|var)/.test(f)){
				f = f.replace(/^\s*\(/, "(var ");
			}
			head.push(a, f, "{");
			tail.push("}");
		}
		return head.join("") + "r.push(" + frag[0] + ");" + tail.join("") + "return r;";	// String
	};
	
	var currying = function(/*Object*/ info){
		return function(){	// Function
			if(arguments.length + info.args.length < info.arity){
				return currying({func: info.func, arity: info.arity, 
					args: Array.prototype.concat.apply(info.args, arguments)});
			}
			return info.func.apply(this, Array.prototype.concat.apply(info.args, arguments));
		};
	};
	
	var identity = function(x){ return x; };
	var compose = function(/*Array*/ a){
		return a.length ? function(){
			var i = a.length - 1, x = df.lambda(a[i]).apply(this, arguments);
			for(--i; i >= 0; --i){ x = df.lambda(a[i]).call(this, x); }
			return x;
		} : identity;
	};
	
	d.mixin(df, {
		// lambda
		buildLambda: function(/*String*/ s){
			// summary: builds a function from a snippet, returns a string, 
			//	which represents the function.
			// description: This method returns a textual representation of a function 
			//	built from the snippet. It is meant to be evaled in the proper context, 
			//	so local variables can be pulled from the environment.
			s = lambda(s);
			return "function(" + s.args.join(",") + "){" + s.body + "}";	// String
		},
		lambda: function(/*Function|String|Array*/ s){
			// summary: builds a function from a snippet, or array (composing), returns 
			//	a function object; functions are passed through unmodified.
			// description: This method is used to normalize a functional representation
			//	(a text snippet, an array, or a function) to a function object.
			if(typeof s == "function"){ return s; }
			if(s instanceof Array){ return compose(s); }
			s = lambda(s);
			return new Function(s.args, s.body);	// Function
		},
		// sequence generators
		repeat: function(/*Number*/ n, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: builds an array by repeatedly applying a unary function N times 
			//	with a seed value Z.
			o = o || d.global; f = df.lambda(f);
			var t = new Array(n);
			t[0] = z;
			for(var i = 1; i < n; t[i] = z = f.call(o, z), ++i);
			return t;	// Array
		},
		until: function(/*Function|String|Array*/ pr, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: builds an array by repeatedly applying a unary function with 
			//	a seed value Z until the predicate is satisfied.
			o = o || d.global; f = df.lambda(f); pr = df.lambda(pr);
			var t = [];
			for(; !pr.call(o, z); t.push(z), z = f.call(o, z));
			return t;	// Array
		},
		buildListcomp: function(/*String*/ s){
			// summary: builds a function from a text snippet, which represents a valid
			//	JS 1.7 list comprehension, returns a string, which represents the function.
			// description: This method returns a textual representation of a function 
			//	built from the list comprehension text snippet (conformant to JS 1.7). 
			//	It is meant to be evaled in the proper context, so local variable can be 
			//	pulled from the environment.
			return "function(){" + listcomp(s) + "}";	// String
		},
		compileListcomp: function(/*String*/ s){
			// summary: builds a function from a text snippet, which represents a valid
			//	JS 1.7 list comprehension, returns a function object.
			// description: This method returns a function built from the list 
			//	comprehension text snippet (conformant to JS 1.7). It is meant to be 
			//	reused several times.
			return new Function([], listcomp(s));	// Function
		},
		listcomp: function(/*String*/ s){
			// summary: executes the list comprehension building an array.
			return (new Function([], listcomp(s)))();	// Array
		},
		// classic reduce-class functions
		foldl: function(/*Array*/ a, /*Function*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from left 
			//	to right using a seed value as a starting point; returns the final 
			//	value.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			for(var i = 0; i < a.length; z = f.call(o, z, a[i], i, a), ++i);
			return z;	// Object
		},
		foldl1: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from left 
			//	to right; returns the final value.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var z = a[0];
			for(var i = 1; i < a.length; z = f.call(o, z, a[i], i, a), ++i);
			return z;	// Object
		},
		scanl: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from left 
			//	to right using a seed value as a starting point; returns an array
			//	of values produced by foldl() at that point.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, t = new Array(n + 1);
			t[0] = z;
			for(var i = 0; i < n; z = f.call(o, z, a[i], i, a), t[++i] = z);
			return t;	// Array
		},
		scanl1: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from left 
			//	to right; returns an array of values produced by foldl1() at that 
			//	point.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, t = new Array(n), z = a[0];
			t[0] = z;
			for(var i = 1; i < n; z = f.call(o, z, a[i], i, a), t[i++] = z);
			return t;	// Array
		},
		foldr: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from right
			//	to left using a seed value as a starting point; returns the final 
			//	value.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			for(var i = a.length; i > 0; --i, z = f.call(o, z, a[i], i, a));
			return z;	// Object
		},
		foldr1: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from right
			//	to left; returns the final value.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, z = a[n - 1];
			for(var i = n - 1; i > 0; --i, z = f.call(o, z, a[i], i, a));
			return z;	// Object
		},
		scanr: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from right
			//	to left using a seed value as a starting point; returns an array
			//	of values produced by foldr() at that point.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, t = new Array(n + 1);
			t[n] = z;
			for(var i = n; i > 0; --i, z = f.call(o, z, a[i], i, a), t[i] = z);
			return t;	// Array
		},
		scanr1: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object*/ z, /*Object?*/ o){
			// summary: repeatedly applies a binary function to an array from right
			//	to left; returns an array of values produced by foldr1() at that 
			//	point.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, t = new Array(n), z = a[n - 1];
			t[n - 1] = z;
			for(var i = n - 1; i > 0; --i, z = f.call(o, z, a[i], i, a), t[i] = z);
			return t;	// Array
		},
		// JS 1.6 standard array functions, which can take a lambda as a parameter.
		// Consider using dojo._base.array functions, if you don't need the lambda support.
		filter: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: creates a new array with all elements that pass the test 
			//	implemented by the provided function.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, t = [], v;
			for(var i = 0; i < n; ++i){
				v = a[i];
				if(f.call(o, v, i, a)){ t.push(v); }
			}
			return t;	// Array
		},
		forEach: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: executes a provided function once per array element.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length;
			for(var i = 0; i < n; f.call(o, a[i], i, a), ++i);
		},
		map: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: creates a new array with the results of calling 
			//	a provided function on every element in this array.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length, t = new Array(n);
			for(var i = 0; i < n; t[i] = f.call(o, a[i], i, a), ++i);
			return t;	// Array
		},
		every: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: tests whether all elements in the array pass the test 
			//	implemented by the provided function.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length;
			for(var i = 0; i < n; ++i){
				if(!f.call(o, a[i], i, a)){
					return false;	// Boolean
				}
			}
			return true;	// Boolean
		},
		some: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: tests whether some element in the array passes the test 
			//	implemented by the provided function.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			var n = a.length;
			for(var i = 0; i < n; ++i){
				if(f.call(o, a[i], i, a)){
					return true;	// Boolean
				}
			}
			return false;	// Boolean
		},
		// JS 1.8 standard array functions, which can take a lambda as a parameter.
		reduce: function(/*Array*/ a, /*Function*/ f, /*Object?*/ z){
			// summary: apply a function simultaneously against two values of the array 
			//	(from left-to-right) as to reduce it to a single value.
			return arguments.length < 3 ? df.foldl1(a, f) : df.foldl(a, f, z);	// Object
		},
		reduceRight: function(/*Array*/ a, /*Function*/ f, /*Object?*/ z){
			// summary: apply a function simultaneously against two values of the array 
			//	(from right-to-left) as to reduce it to a single value.
			return arguments.length < 3 ? df.foldr1(a, f) : df.foldr(a, f, z);	// Object
		},
		// currying and partial functions
		curry: function(/*Function|String|Array*/ f, /*Number?*/ arity){
			// summary: curries a function until the arity is satisfied, at 
			//	which point it returns the calculated value.
			f = df.lambda(f);
			arity = typeof arity == "number" ? arity : f.length;
			return currying({func: f, arity: arity, args: []});	// Function
		},
		arg: {},	// marker for missing arguments
		partial: function(/*Function|String|Array*/ f){
			// summary: creates a function where some arguments are bound, and
			//	some arguments (marked as dojox.lang.functional.arg) are will be 
			//	accepted by the final function in the order they are encountered.
			// description: This method is used to produce partially bound 
			//	functions. If you want to change the order of arguments, use
			//	dojox.lang.functional.mixer() or dojox.lang.functional.flip().
			var a = arguments, args = new Array(a.length - 1), p = [];
			f = df.lambda(f);
			for(var i = 1; i < a.length; ++i){
				var t = a[i];
				args[i - 1] = t;
				if(t == df.arg){
					p.push(i - 1);
				}
			}
			return function(){	// Function
				var t = Array.prototype.slice.call(args, 0); // clone the array
				for(var i = 0; i < p.length; ++i){
					t[p[i]] = arguments[i];
				}
				return f.apply(this, t);
			};
		},
		// argument pre-processing
		mixer: function(/*Function|String|Array*/ f, /*Array*/ mix){
			// summary: changes the order of arguments using an array of
			//	numbers mix --- i-th argument comes from mix[i]-th place
			//	of supplied arguments.
			f = df.lambda(f);
			return function(){	// Function
				var t = new Array(mix.length);
				for(var i = 0; i < mix.length; ++i){
					t[i] = arguments[mix[i]];
				}
				return f.apply(this, t);
			};
		},
		flip: function(/*Function|String|Array*/ f){
			// summary: changes the order of arguments by reversing their
			//	order.
			f = df.lambda(f);
			return function(){	// Function
				// reverse arguments
				var a = arguments, l = a.length - 1, t = new Array(l + 1), i;
				for(i = 0; i <= l; ++i){
					t[l - i] = a[i];
				}
				return f.apply(this, t);
			};
		},
		// combiners
		zip: function(){
			// summary: returns an array of arrays, where the i-th array 
			//	contains the i-th element from each of the argument arrays.
			// description: This is the venerable zip combiner (for example,
			//	see Python documentation for general details). The returned
			//	array is truncated to match the length of the shortest input
			//	array.
			var n = arguments[0].length, m = arguments.length, i;
			for(i = 1; i < m; n = Math.min(n, arguments[i++].length));
			var t = new Array(n), j;
			for(i = 0; i < n; ++i){
				var p = new Array(m);
				for(j = 0; j < m; p[j] = arguments[j][i], ++j);
				t[i] = p;
			}
			return t;	// Array
		},
		unzip: function(/*Array*/ a){
			// summary: similar to dojox.lang.functional.zip(), but takes 
			//	a single array of arrays as the input.
			// description: This function is similar to dojox.lang.functional.zip() 
			//	and can be used to unzip objects packed by 
			//	dojox.lang.functional.zip(). It is here mostly to provide 
			//	a short-cut for the different method signature.
			return df.zip.apply(null, a);	// Array
		},
		// miscelaneous functional adapters
		constFun: function(/*Object*/ x){
			// summary: returns a function, which produces a constant value 
			//	regardless of supplied parameters.
			return function(){ return x; };	// Function
		},
		invoke: function(/*String*/ m){
			// summary: returns a function, which invokes a method on supplied 
			//	object using optional parameters.
			return function(/*Object*/ o){	// Function
				return o[m].apply(o, Array.prototype.slice.call(arguments, 1));
			};
		},
		pluck: function(/*String*/ m){
			// summary: returns a function, which returns a named object member.
			return function(/*Object*/ o){	// Function
				return o[m];
			};
		},
		// object helpers
		forIn: function(/*Object*/ obj, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: iterates over all object members skipping members, which 
			//	are present in the empty object (IE and/or 3rd-party libraries).
			o = o || d.global; f = df.lambda(f);
			for(var i in obj){
				if(i in empty){ continue; }
				f.call(o, obj[i], i, obj);
			}
		},
		forEachReversed: function(/*Array*/ a, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: executes a provided function once per array element.
			a = typeof a == "string" ? a.split("") : a; o = o || d.global; f = df.lambda(f);
			for(var i = a.length - 1; i >= 0; f.call(o, a[i], i, a), --i);
		}
	});

	// monads
	dojo.declare("dojox.lang.functional.MaybeMonad", null, {
		constructor: function(/*Object*/ value){
			// summary: constructs a monad optionally initializing all additional members
			if(arguments.length){
				this.value = value;
			}
		},
		bind: function(/*dojox.lang.functional.Monad*/ monad, /*Function|String|Array*/ f, /*Object?*/ o){
			// summary: this is the classic bind method, which applies a function to a monad,
			//	and returns a result as a monad; it is meant to be overwritten to incorporate
			//	side effects
			if(!("value" in monad)){
				return new this.constructor();	// dojox.lang.functional.MaybeMonad
			}
			// => possible side-effects go here
			o = o || d.global; f = df.lambda(f);
			return f.call(o, monad.value);	// dojox.lang.functional.Monad
		},
		// class-specific methods
		isNothing: function(){
			// summary: check if there is no bound value.
			return !("value" in this);	// Boolean
		}
	});
	df.MaybeMonad.returnMonad = function(/*Object*/ value){
		// summary: puts a valye in the Maybe monad.
		return new df.MaybeMonad(value);	// dojox.lang.functional.MaybeMonad
	};
	df.MaybeMonad.zero = new df.MaybeMonad();
})();

}
