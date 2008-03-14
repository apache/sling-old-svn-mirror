if(!dojo._hasResource["dojox.gfx.silverlight"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.gfx.silverlight"] = true;
dojo.provide("dojox.gfx.silverlight");

dojo.require("dojox.gfx._base");
dojo.require("dojox.gfx.shape");
dojo.require("dojox.gfx.path");

dojo.experimental("dojox.gfx.silverlight");

dojox.gfx.silverlight.dasharray = {
	solid:				"none",
	shortdash:			[4, 1],
	shortdot:			[1, 1],
	shortdashdot:		[4, 1, 1, 1],
	shortdashdotdot:	[4, 1, 1, 1, 1, 1],
	dot:				[1, 3],
	dash:				[4, 3],
	longdash:			[8, 3],
	dashdot:			[4, 3, 1, 3],
	longdashdot:		[8, 3, 1, 3],
	longdashdotdot:		[8, 3, 1, 3, 1, 3]
};

dojox.gfx.silverlight.fontweight = {
	normal: 400,
	bold:   700
};

dojox.gfx.silverlight.caps  = {butt: "Flat", round: "Round", square: "Square"};
dojox.gfx.silverlight.joins = {bevel: "Bevel", round: "Round"};

dojox.gfx.silverlight.fonts = {
	serif: "Times New Roman",
	times: "Times New Roman",
	"sans-serif": "Arial",
	helvetica: "Arial",
	monotone: "Courier New",
	courier: "Courier New"
};

dojox.gfx.silverlight.hexColor = function(/*String|Array|dojo.Color*/ color){
	// summary: converts a color object to a Silverlight hex color string (#aarrggbb)
	var c = dojox.gfx.normalizeColor(color),
		t = c.toHex(), a = Math.round(c.a * 255);
	a = (a < 0 ? 0 : a > 255 ? 255 : a).toString(16);
	return "#" + (a.length < 2 ? "0" + a : a) + t.slice(1);	// String
};

dojo.extend(dojox.gfx.Shape, {
	// summary: Silverlight-specific implementation of dojox.gfx.Shape methods
	
	setFill: function(fill){
		// summary: sets a fill object (Silverlight)
		// fill: Object: a fill object
		//	(see dojox.gfx.defaultLinearGradient, 
		//	dojox.gfx.defaultRadialGradient, 
		//	dojox.gfx.defaultPattern, 
		//	or dojo.Color)

		var p = this.rawNode.getHost().content, r = this.rawNode, f;
		if(!fill){
			// don't fill
			this.fillStyle = null;
			this._setFillAttr(null);
			return this;	// self
		}
		if(typeof(fill) == "object" && "type" in fill){
			// gradient
			switch(fill.type){
				case "linear":
					this.fillStyle = f = dojox.gfx.makeParameters(dojox.gfx.defaultLinearGradient, fill);
					var lgb = p.createFromXaml("<LinearGradientBrush/>");
					lgb.mappingMode = "Absolute";
					lgb.startPoint = f.x1 + "," + f.y1;
					lgb.endPoint = f.x2 + "," + f.y2;
					dojo.forEach(f.colors, function(c){
						var t = p.createFromXaml("<GradientStop/>");
						t.offset = c.offset;
						t.color = dojox.gfx.silverlight.hexColor(c.color);
						lgb.gradientStops.add(t);
					});
					this._setFillAttr(lgb);
					break;
				case "radial":
					this.fillStyle = f = dojox.gfx.makeParameters(dojox.gfx.defaultRadialGradient, fill);
					var rgb = p.createFromXaml("<RadialGradientBrush/>"), w = r.width, h = r.height,
						l = this.rawNode["Canvas.Left"], t = this.rawNode["Canvas.Top"];
					rgb.center = (f.cx - l) / w + "," + (f.cy - t) / h;
					rgb.radiusX = f.r / w;
					rgb.radiusY = f.r / h;
					dojo.forEach(f.colors, function(c){
						var t = p.createFromXaml("<GradientStop/>");
						t.offset = c.offset;
						t.color = dojox.gfx.silverlight.hexColor(c.color);
						rgb.gradientStops.add(t);
					});
					this._setFillAttr(rgb);
					break;
				case "pattern":
					// don't fill: Silverlight doesn't define TileBrush for some reason
					this.fillStyle = null;
					this._setFillAttr(null);
					break;
			}
			return this;	// self
		}
		// color object
		this.fillStyle = f = dojox.gfx.normalizeColor(fill);
		var scb = p.createFromXaml("<SolidColorBrush/>");
		scb.color = f.toHex();
		scb.opacity = f.a;
		this._setFillAttr(scb);
		return this;	// self
	},
	_setFillAttr: function(f){
		this.rawNode.fill = f;
	},

	setStroke: function(stroke){
		// summary: sets a stroke object (Silverlight)
		// stroke: Object: a stroke object
		//	(see dojox.gfx.defaultStroke) 
	
		var p = this.rawNode.getHost().content, r = this.rawNode;
		if(!stroke){
			// don't stroke
			this.strokeStyle = null;
			r.stroke = null;
			return this;
		}
		// normalize the stroke
		if(typeof stroke == "string"){
			stroke = {color: stroke};
		}
		var s = this.strokeStyle = dojox.gfx.makeParameters(dojox.gfx.defaultStroke, stroke);
		s.color = dojox.gfx.normalizeColor(s.color);
		// generate attributes
		if(s){
			var scb = p.createFromXaml("<SolidColorBrush/>");
			scb.color = s.color.toHex();
			scb.opacity = s.color.a;
			r.stroke = scb;
			r.strokeThickness = s.width;
			r.strokeStartLineCap = r.strokeEndLineCap = r.strokeDashCap = 
				dojox.gfx.silverlight.caps[s.cap];
			if(typeof s.join == "number"){
				r.strokeLineJoin = "Miter";
				r.strokeMiterLimit = s.join;
			}else{
				r.strokeLineJoin = dojox.gfx.silverlight.joins[s.join];
			}
			var da = s.style.toLowerCase();
			if(da in dojox.gfx.silverlight.dasharray){ da = dojox.gfx.silverlight.dasharray[da]; }
			if(da instanceof Array){
				da = dojo.clone(da);
				/*
				for(var i = 0; i < da.length; ++i){
					da[i] *= s.width;
				}
				*/
				if(s.cap != "butt"){
					for(var i = 0; i < da.length; i += 2){
						//da[i] -= s.width;
						--da[i]
						if(da[i] < 1){ da[i] = 1; }
					}
					for(var i = 1; i < da.length; i += 2){
						//da[i] += s.width;
						++da[i];
					}
				}
				r.strokeDashArray = da.join(",");
			}else{
				r.strokeDashArray = null;
			}
		}
		return this;	// self
	},
	
	_getParentSurface: function(){
		var surface = this.parent;
		for(; surface && !(surface instanceof dojox.gfx.Surface); surface = surface.parent);
		return surface;
	},
	
	_applyTransform: function() {
		var tm = this.matrix, r = this.rawNode;
		if(tm){
			var p = this.rawNode.getHost().content,
				m = p.createFromXaml("<MatrixTransform/>"),
				mm = p.createFromXaml("<Matrix/>");
			mm.m11 = tm.xx;
			mm.m21 = tm.xy;
			mm.m12 = tm.yx;
			mm.m22 = tm.yy;
			mm.offsetX = tm.dx;
			mm.offsetY = tm.dy;
			m.matrix = mm;
			r.renderTransform = m;
		}else{
			r.renderTransform = null;
		}
		return this;
	},

	setRawNode: function(rawNode){
		// summary:
		//	assigns and clears the underlying node that will represent this
		//	shape. Once set, transforms, gradients, etc, can be applied.
		//	(no fill & stroke by default)
		rawNode.fill = null;
		rawNode.stroke = null;
		this.rawNode = rawNode;
	},
	
	// move family

	_moveToFront: function(){
		// summary: moves a shape to front of its parent's list of shapes (Silverlight)
		var c = this.parent.rawNode.children, r = this.rawNode;
		c.remove(r);
		c.add(r);
		return this;	// self
	},
	_moveToBack: function(){
		// summary: moves a shape to back of its parent's list of shapes (Silverlight)
		var c = this.parent.rawNode.children, r = this.rawNode;
		c.remove(r);
		c.insert(0, r);
		return this;	// self
	}
});

dojo.declare("dojox.gfx.Group", dojox.gfx.Shape, {
	// summary: a group shape (Silverlight), which can be used 
	//	to logically group shapes (e.g, to propagate matricies)
	constructor: function(){
		dojox.gfx.silverlight.Container._init.call(this);
	},
	setRawNode: function(rawNode){
		// summary: sets a raw Silverlight node to be used by this shape
		// rawNode: Node: an Silverlight node
		this.rawNode = rawNode;
	}
});
dojox.gfx.Group.nodeType = "Canvas";

dojo.declare("dojox.gfx.Rect", dojox.gfx.shape.Rect, {
	// summary: a rectangle shape (Silverlight)
	setShape: function(newShape){
		// summary: sets a rectangle shape object (Silverlight)
		// newShape: Object: a rectangle shape object
		this.shape = dojox.gfx.makeParameters(this.shape, newShape);
		this.bbox = null;
		var r = this.rawNode, n = this.shape;
		r["Canvas.Left"] = n.x;
		r["Canvas.Top"]  = n.y;
		r.width   = n.width;
		r.height  = n.height;
		r.radiusX = r.radiusY = n.r;
		return this;	// self
	}
});
dojox.gfx.Rect.nodeType = "Rectangle";

dojo.declare("dojox.gfx.Ellipse", dojox.gfx.shape.Ellipse, {
	// summary: an ellipse shape (Silverlight)
	setShape: function(newShape){
		// summary: sets an ellipse shape object (Silverlight)
		// newShape: Object: an ellipse shape object
		this.shape = dojox.gfx.makeParameters(this.shape, newShape);
		this.bbox = null;
		var r = this.rawNode, n = this.shape;
		r["Canvas.Left"] = n.cx - n.rx;
		r["Canvas.Top"]  = n.cy - n.ry;
		r.width  = 2 * n.rx;
		r.height = 2 * n.ry;
		return this;	// self
	}
});
dojox.gfx.Ellipse.nodeType = "Ellipse";

dojo.declare("dojox.gfx.Circle", dojox.gfx.shape.Circle, {
	// summary: a circle shape (Silverlight)
	setShape: function(newShape){
		// summary: sets a circle shape object (Silverlight)
		// newShape: Object: a circle shape object
		this.shape = dojox.gfx.makeParameters(this.shape, newShape);
		this.bbox = null;
		var r = this.rawNode, n = this.shape;
		r["Canvas.Left"] = n.cx - n.r;
		r["Canvas.Top"]  = n.cy - n.r;
		r.width = r.height = 2 * n.r;
		return this;	// self
	}
});
dojox.gfx.Circle.nodeType = "Ellipse";

dojo.declare("dojox.gfx.Line", dojox.gfx.shape.Line, {
	// summary: a line shape (Silverlight)
	setShape: function(newShape){
		// summary: sets a line shape object (Silverlight)
		// newShape: Object: a line shape object
		this.shape = dojox.gfx.makeParameters(this.shape, newShape);
		this.bbox = null;
		var r = this.rawNode, n = this.shape;
		r.x1 = n.x1; r.y1 = n.y1; r.x2 = n.x2; r.y2 = n.y2;
		return this;	// self
	}
});
dojox.gfx.Line.nodeType = "Line";

dojo.declare("dojox.gfx.Polyline", dojox.gfx.shape.Polyline, {
	// summary: a polyline/polygon shape (Silverlight)
	setShape: function(points, closed){
		// summary: sets a polyline/polygon shape object (Silverlight)
		// points: Object: a polyline/polygon shape object
		if(points && points instanceof Array){
			// branch
			// points: Array: an array of points
			this.shape = dojox.gfx.makeParameters(this.shape, {points: points});
			if(closed && this.shape.points.length){ 
				this.shape.points.push(this.shape.points[0]);
			}
		}else{
			this.shape = dojox.gfx.makeParameters(this.shape, points);
		}
		this.box = null;
		var p = this.shape.points, rp = [];
		for(var i = 0; i < p.length; ++i){
			if(typeof p[i] == "number"){
				rp.push(p[i], p[++i]);
			}else{
				rp.push(p[i].x, p[i].y);
			}
		}
		this.rawNode.points = rp.join(",");
		return this;	// self
	}
});
dojox.gfx.Polyline.nodeType = "Polyline";

dojo.declare("dojox.gfx.Image", dojox.gfx.shape.Image, {
	// summary: an image (Silverlight)
	setShape: function(newShape){
		// summary: sets an image shape object (Silverlight)
		// newShape: Object: an image shape object
		this.shape = dojox.gfx.makeParameters(this.shape, newShape);
		this.bbox = null;
		var r = this.rawNode, n = this.shape;
		r["Canvas.Left"] = n.x;
		r["Canvas.Top"]  = n.y;
		r.width  = n.width;
		r.height = n.height;
		r.source = n.src;
		return this;	// self
	},
	setRawNode: function(rawNode){
		// summary:
		//	assigns and clears the underlying node that will represent this
		//	shape. Once set, transforms, gradients, etc, can be applied.
		//	(no fill & stroke by default)
		this.rawNode = rawNode;
	}
});
dojox.gfx.Image.nodeType = "Image";

dojo.declare("dojox.gfx.Text", dojox.gfx.shape.Text, {
	// summary: an anchored text (Silverlight)
	setShape: function(newShape){
		// summary: sets a text shape object (Silverlight)
		// newShape: Object: a text shape object
		this.shape = dojox.gfx.makeParameters(this.shape, newShape);
		this.bbox = null;
		var r = this.rawNode, s = this.shape;
		r.text = s.text;
		r.textDecorations = s.decoration == "underline" ? "Underline" : "None";
		r["Canvas.Left"] = -10000;
		r["Canvas.Top"]  = -10000;
		window.setTimeout(dojo.hitch(this, "_delayAlignment"), 0);
		return this;	// self
	},
	_delayAlignment: function(){
		// handle alignment
		var r = this.rawNode, s = this.shape,
			w = r.actualWidth, h = r.actualHeight, x = s.x, y = s.y - h * 0.75;
		switch(s.align){
			case "middle":
				x -= w / 2;
				break;
			case "end":
				x -= w;
				break;
		}
		var a = this.matrix ? dojox.gfx.matrix.multiplyPoint(this.matrix, x, y) : {x: x, y: y};
		r["Canvas.Left"] = a.x;
		r["Canvas.Top"]  = a.y;
	},
	setStroke: function(){
		// summary: ignore setting a stroke style
		return this;	// self
	},
	_setFillAttr: function(f){
		this.rawNode.foreground = f;
	},
	setRawNode: function(rawNode){
		// summary:
		//	assigns and clears the underlying node that will represent this
		//	shape. Once set, transforms, gradients, etc, can be applied.
		//	(no fill & stroke by default)
		this.rawNode = rawNode;
	},
	_applyTransform: function() {
		var tm = this.matrix, r = this.rawNode;
		if(tm){
			// the next line is pure magic :-(
			tm = dojox.gfx.matrix.normalize([1/100, tm, 100]);
			var p = this.rawNode.getHost().content,
				m = p.createFromXaml("<MatrixTransform/>"),
				mm = p.createFromXaml("<Matrix/>");
			mm.m11 = tm.xx;
			mm.m21 = tm.xy;
			mm.m12 = tm.yx;
			mm.m22 = tm.yy;
			mm.offsetX = tm.dx;
			mm.offsetY = tm.dy;
			m.matrix = mm;
			r.renderTransform = m;
		}else{
			r.renderTransform = null;
		}
		return this;
	},
	getTextWidth: function(){ 
		// summary: get the text width in pixels 
		return this.rawNode.actualWidth; 
	} 
});
dojox.gfx.Text.nodeType = "TextBlock";

dojo.declare("dojox.gfx.Path", dojox.gfx.path.Path, {
	// summary: a path shape (Silverlight)
	_updateWithSegment: function(segment){
		// summary: updates the bounding box of path with new segment
		// segment: Object: a segment
		dojox.gfx.Path.superclass._updateWithSegment.apply(this, arguments);
		var p = this.shape.path;
		if(typeof(p) == "string"){
			this.rawNode.data = p ? p : null;
		}
	},
	setShape: function(newShape){
		// summary: forms a path using a shape (Silverlight)
		// newShape: Object: an SVG path string or a path object (see dojox.gfx.defaultPath)
		dojox.gfx.Path.superclass.setShape.apply(this, arguments);
		var p = this.shape.path;
		this.rawNode.data = p ? p : null;
		return this;	// self
	}
});
dojox.gfx.Path.nodeType = "Path";

dojo.declare("dojox.gfx.TextPath", dojox.gfx.path.TextPath, {
	// summary: a textpath shape (Silverlight)
	_updateWithSegment: function(segment){
		// summary: updates the bounding box of path with new segment
		// segment: Object: a segment
	},
	setShape: function(newShape){
		// summary: forms a path using a shape (Silverlight)
		// newShape: Object: an SVG path string or a path object (see dojox.gfx.defaultPath)
	},
	_setText: function(){
	}
});
dojox.gfx.TextPath.nodeType = "text";

dojo.declare("dojox.gfx.Surface", dojox.gfx.shape.Surface, {
	// summary: a surface object to be used for drawings (Silverlight)
	constructor: function(){
		dojox.gfx.silverlight.Container._init.call(this);
	},
	setDimensions: function(width, height){
		// summary: sets the width and height of the rawNode
		// width: String: width of surface, e.g., "100px"
		// height: String: height of surface, e.g., "100px"
		this.width  = dojox.gfx.normalizedLength(width);	// in pixels
		this.height = dojox.gfx.normalizedLength(height);	// in pixels
		var p = this.rawNode && this.rawNode.getHost();
		if(p){
			p.width = width;
			p.height = height;
		}
		return this;	// self
	},
	getDimensions: function(){
		// summary: returns an object with properties "width" and "height"
		var p = this.rawNode && this.rawNode.getHost();
		var t = p ? {width: p.content.actualWidth, height: p.content.actualHeight} : null;
		if(t.width  <= 0){ t.width  = this.width; }
		if(t.height <= 0){ t.height = this.height; }
		return t;	// Object
	}
});

dojox.gfx.silverlight.surfaces = {};

dojox.gfx.createSurface = function(parentNode, width, height){
	// summary: creates a surface (Silverlight)
	// parentNode: Node: a parent node
	// width: String: width of surface, e.g., "100px"
	// height: String: height of surface, e.g., "100px"

	var s = new dojox.gfx.Surface();
	parentNode = dojo.byId(parentNode);
	// create an empty canvas
	var t = parentNode.ownerDocument.createElement("script");
	t.type = "text/xaml";
	t.id = dojox.gfx._base._getUniqueId();
	t.text = "<Canvas xmlns='http://schemas.microsoft.com/client/2007' Name='" + dojox.gfx._base._getUniqueId() + "'/>";
	document.body.appendChild(t);
	// create a plugin
	var pluginName = dojox.gfx._base._getUniqueId();
	Silverlight.createObject(
		"#" + t.id,	// none
		parentNode,
		pluginName,
		{										// Plugin properties.
			width:	String(width),				// Width of rectangular region of plugin in pixels.
			height:	String(height),				// Height of rectangular region of plugin in pixels.
			inplaceInstallPrompt:	"false",	// Determines whether to display in-place install prompt if invalid version detected.
			//background:		"white",		// Background color of plugin.
			//isWindowless:	"false",			// Determines whether to display plugin in Windowless mode.
			background:		"transparent",		// Background color of plugin.
			isWindowless:	"true",				// Determines whether to display plugin in Windowless mode.
			framerate:		"24",				// MaxFrameRate property value.
			version:		"1.0"				// Silverlight version.
		},
		{},
		null,
		null
	);
	s.rawNode = dojo.byId(pluginName).content.root;
	// register the plugin with its parent node
	dojox.gfx.silverlight.surfaces[s.rawNode.name] = parentNode;
	s.width  = dojox.gfx.normalizedLength(width);	// in pixels
	s.height = dojox.gfx.normalizedLength(height);	// in pixels
	return s;	// dojox.gfx.Surface
};

// Extenders

dojox.gfx.silverlight.Font = {
	_setFont: function(){
		// summary: sets a font object (Silverlight)
		var f = this.fontStyle, r = this.rawNode,
			fw = dojox.gfx.silverlight.fontweight,
			fo = dojox.gfx.silverlight.fonts, t = f.family.toLowerCase();
		r.fontStyle = f.style == "italic" ? "Italic" : "Normal";
		r.fontWeight = f.weight in fw ? fw[f.weight] : f.weight;
		r.fontSize = dojox.gfx.normalizedLength(f.size);
		r.fontFamily = t in fo ? fo[t] : f.family;
	}
};

dojox.gfx.silverlight.Container = {
	_init: function(){
		dojox.gfx.shape.Container._init.call(this);
	},
	add: function(shape){
		// summary: adds a shape to a group/surface
		// shape: dojox.gfx.Shape: an VML shape object
		if(this != shape.getParent()){
			//dojox.gfx.Group.superclass.add.apply(this, arguments);
			//this.inherited(arguments);
			dojox.gfx.shape.Container.add.apply(this, arguments);
			this.rawNode.children.add(shape.rawNode);
		}
		return this;	// self
	},
	remove: function(shape, silently){
		// summary: remove a shape from a group/surface
		// shape: dojox.gfx.Shape: an VML shape object
		// silently: Boolean?: if true, regenerate a picture
		if(this == shape.getParent()){
			var parent = shape.rawNode.getParent();
			if(parent){
				parent.children.remove(shape.rawNode);
			}
			//dojox.gfx.Group.superclass.remove.apply(this, arguments);
			//this.inherited(arguments);
			dojox.gfx.shape.Container.remove.apply(this, arguments);
		}
		return this;	// self
	},
	clear: function(){
		// summary: removes all shapes from a group/surface
		this.rawNode.children.clear();
		//return this.inherited(arguments);	// self
		return dojox.gfx.shape.Container.clear.apply(this, arguments);
	},
	_moveChildToFront: dojox.gfx.shape.Container._moveChildToFront,
	_moveChildToBack:  dojox.gfx.shape.Container._moveChildToBack
};

dojo.mixin(dojox.gfx.shape.Creator, {
	createObject: function(shapeType, rawShape){
		// summary: creates an instance of the passed shapeType class
		// shapeType: Function: a class constructor to create an instance of
		// rawShape: Object: properties to be passed in to the classes "setShape" method
		if(!this.rawNode){ return null; }
		var shape = new shapeType();
		var node = this.rawNode.getHost().content.createFromXaml("<" + shapeType.nodeType + "/>");
		shape.setRawNode(node);
		shape.setShape(rawShape);
		this.add(shape);
		return shape;	// dojox.gfx.Shape
	}
});

dojo.extend(dojox.gfx.Text, dojox.gfx.silverlight.Font);
//dojo.extend(dojox.gfx.TextPath, dojox.gfx.silverlight.Font);

dojo.extend(dojox.gfx.Group, dojox.gfx.silverlight.Container);
dojo.extend(dojox.gfx.Group, dojox.gfx.shape.Creator);

dojo.extend(dojox.gfx.Surface, dojox.gfx.silverlight.Container);
dojo.extend(dojox.gfx.Surface, dojox.gfx.shape.Creator);

(function(){
	var surfaces = dojox.gfx.silverlight.surfaces;
	var mouseFix = function(s, a){
		var ev = {target: s, currentTarget: s, 
			preventDefault: function(){}, stopPropagation: function(){}};
		if(a){
			ev.ctrlKey = a.ctrl;
			ev.shiftKey = a.shift;
			var p = a.getPosition(null);
			ev.x = ev.offsetX = ev.layerX = p.x;
			ev.y = ev.offsetY = ev.layerY = p.y;
			// calculate clientX and clientY
			var parent = surfaces[s.getHost().content.root.name];
			var t = dojo._abs(parent);
			ev.clientX = t.x + p.x;
			ev.clientY = t.y + p.y;
		}
		return ev;
	};
	var keyFix = function(s, a){
		var ev = {
			keyCode:  a.platformKeyCode,
			ctrlKey:  a.ctrl,
			shiftKey: a.shift
		};
		return ev;
	};
	var eventNames = {
		onclick:		{name: "MouseLeftButtonUp", fix: mouseFix},
		onmouseenter:	{name: "MouseEnter", fix: mouseFix},
		onmouseleave:	{name: "MouseLeave", fix: mouseFix},
		onmousedown:	{name: "MouseLeftButtonDown", fix: mouseFix},
		onmouseup:		{name: "MouseLeftButtonUp", fix: mouseFix},
		onmousemove:	{name: "MouseMove", fix: mouseFix},
		onkeydown:		{name: "KeyDown", fix: keyFix},
		onkeyup:		{name: "KeyUp", fix: keyFix}
	};
	var eventsProcessing = {
		connect: function(name, object, method){
			var token, n = name in eventNames ? eventNames[name] : 
				{name: name, fix: function(){ return {}; }};
			if(arguments.length > 2){
				token = this.getEventSource().addEventListener(n.name, 
					function(s, a){ dojo.hitch(object, method)(n.fix(s, a)); });
			}else{
				token = this.getEventSource().addEventListener(n.name, 
					function(s, a){ object(n.fix(s, a)); });
			}
			return {name: n.name, token: token};
		},
		disconnect: function(token){
			this.getEventSource().removeEventListener(token.name, token.token);
		}
	};
	dojo.extend(dojox.gfx.Shape, eventsProcessing);
	dojo.extend(dojox.gfx.Surface, eventsProcessing);
	dojox.gfx.equalSources = function(a, b){
		return a && b && a.equals(b);
	}

})();

}
