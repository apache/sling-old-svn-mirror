if(!dojo._hasResource["dojo._base.Color"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojo._base.Color"] = true;
dojo.provide("dojo._base.Color");
dojo.require("dojo._base.array");
dojo.require("dojo._base.lang");

dojo.Color = function(/*Array|String|Object*/ color){
	// summary:
	//		takes a named string, hex string, array of rgb or rgba values,
	//		an object with r, g, b, and a properties, or another dojo.Color object
	if(color){ this.setColor(color); }
};

// FIXME: there's got to be a more space-efficient way to encode or discover these!!  Use hex?
dojo.Color.named = {
	black:      [0,0,0],
	silver:     [192,192,192],
	gray:       [128,128,128],
	white:      [255,255,255],
	maroon:		[128,0,0],
	red:        [255,0,0],
	purple:		[128,0,128],
	fuchsia:	[255,0,255],
	green:	    [0,128,0],
	lime:	    [0,255,0],
	olive:		[128,128,0],
	yellow:		[255,255,0],
	navy:       [0,0,128],
	blue:       [0,0,255],
	teal:		[0,128,128],
	aqua:		[0,255,255]
};


dojo.extend(dojo.Color, {
	r: 255, g: 255, b: 255, a: 1,
	_set: function(r, g, b, a){
		var t = this; t.r = r; t.g = g; t.b = b; t.a = a;
	},
	setColor: function(/*Array|String|Object*/ color){
		// summary:
		//		takes a named string, hex string, array of rgb or rgba values,
		//		an object with r, g, b, and a properties, or another dojo.Color object
		var d = dojo;
		if(d.isString(color)){
			d.colorFromString(color, this);
		}else if(d.isArray(color)){
			d.colorFromArray(color, this);
		}else{
			this._set(color.r, color.g, color.b, color.a);
			if(!(color instanceof d.Color)){ this.sanitize(); }
		}
		return this;	// dojo.Color
	},
	sanitize: function(){
		// summary:
		//		makes sure that the object has correct attributes
		// description: 
		//		the default implementation does nothing, include dojo.colors to
		//		augment it to real checks
		return this;	// dojo.Color
	},
	toRgb: function(){
		// summary: returns 3 component array of rgb values
		var t = this;
		return [t.r, t.g, t.b];	// Array
	},
	toRgba: function(){
		// summary: returns a 4 component array of rgba values
		var t = this;
		return [t.r, t.g, t.b, t.a];	// Array
	},
	toHex: function(){
		// summary: returns a css color string in hexadecimal representation
		var arr = dojo.map(["r", "g", "b"], function(x){
			var s = this[x].toString(16);
			return s.length < 2 ? "0" + s : s;
		}, this);
		return "#" + arr.join("");	// String
	},
	toCss: function(/*Boolean?*/ includeAlpha){
		// summary: returns a css color string in rgb(a) representation
		var t = this, rgb = t.r + ", " + t.g + ", " + t.b;
		return (includeAlpha ? "rgba(" + rgb + ", " + t.a : "rgb(" + rgb) + ")";	// String
	},
	toString: function(){
		// summary: returns a visual representation of the color
		return this.toCss(true); // String
	}
});

dojo.blendColors = function(
	/*dojo.Color*/ start, 
	/*dojo.Color*/ end, 
	/*Number*/ weight,
	/*dojo.Color?*/ obj
){
	// summary: 
	//		blend colors end and start with weight from 0 to 1, 0.5 being a 50/50 blend,
	//		can reuse a previously allocated dojo.Color object for the result
	var d = dojo, t = obj || new dojo.Color();
	d.forEach(["r", "g", "b", "a"], function(x){
		t[x] = start[x] + (end[x] - start[x]) * weight;
		if(x != "a"){ t[x] = Math.round(t[x]); }
	});
	return t.sanitize();	// dojo.Color
};

dojo.colorFromRgb = function(/*String*/ color, /*dojo.Color?*/ obj){
	// summary: get rgb(a) array from css-style color declarations
	var m = color.toLowerCase().match(/^rgba?\(([\s\.,0-9]+)\)/);
	return m && dojo.colorFromArray(m[1].split(/\s*,\s*/), obj);	// dojo.Color
};

dojo.colorFromHex = function(/*String*/ color, /*dojo.Color?*/ obj){
	// summary: converts a hex string with a '#' prefix to a color object.
	//	Supports 12-bit #rgb shorthand.
	var d = dojo, t = obj || new d.Color(),
		bits = (color.length == 4) ? 4 : 8,
		mask = (1 << bits) - 1;
	color = Number("0x" + color.substr(1));
	if(isNaN(color)){
		return null; // dojo.Color
	}
	d.forEach(["b", "g", "r"], function(x){
		var c = color & mask;
		color >>= bits;
		t[x] = bits == 4 ? 17 * c : c;
	});
	t.a = 1;
	return t;	// dojo.Color
};

dojo.colorFromArray = function(/*Array*/ a, /*dojo.Color?*/ obj){
	// summary: builds a color from 1, 2, 3, or 4 element array
	var t = obj || new dojo.Color();
	t._set(Number(a[0]), Number(a[1]), Number(a[2]), Number(a[3]));
	if(isNaN(t.a)){ t.a = 1; }
	return t.sanitize();	// dojo.Color
};

dojo.colorFromString = function(/*String*/ str, /*dojo.Color?*/ obj){
	//	summary:
	//		parses str for a color value.
	//	description:
	//		Acceptable input values for str may include arrays of any form
	//		accepted by dojo.colorFromArray, hex strings such as "#aaaaaa", or
	//		rgb or rgba strings such as "rgb(133, 200, 16)" or "rgba(10, 10,
	//		10, 50)"
	//	returns:
	//		a dojo.Color object. If obj is passed, it will be the return value.
	var a = dojo.Color.named[str];
	return a && dojo.colorFromArray(a, obj) || dojo.colorFromRgb(str, obj) || dojo.colorFromHex(str, obj);
};

}
