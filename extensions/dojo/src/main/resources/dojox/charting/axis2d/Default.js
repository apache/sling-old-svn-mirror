if(!dojo._hasResource["dojox.charting.axis2d.Default"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.axis2d.Default"] = true;
dojo.provide("dojox.charting.axis2d.Default");

dojo.require("dojox.charting.scaler");
dojo.require("dojox.charting.axis2d.common");
dojo.require("dojox.charting.axis2d.Base");

dojo.require("dojo.colors");
dojo.require("dojox.gfx");
dojo.require("dojox.lang.functional");
dojo.require("dojox.lang.utils");

(function(){
	var dc = dojox.charting, 
		df = dojox.lang.functional, 
		du = dojox.lang.utils, 
		g = dojox.gfx,
		labelGap = 4,				// in pixels
		labelFudgeFactor = 0.8;		// in percents (to convert font's heigth to label width)
		
	var eq = function(/* Number */ a, /* Number */ b){
		// summary: compare two FP numbers for equality
		return Math.abs(a - b) <= 1e-6 * (Math.abs(a) + Math.abs(b));	// Boolean
	};

	dojo.declare("dojox.charting.axis2d.Default", dojox.charting.axis2d.Base, {
		 defaultParams: {
			vertical:    false,		// true for vertical axis
			fixUpper:    "none",	// align the upper on ticks: "major", "minor", "micro", "none"
			fixLower:    "none",	// align the lower on ticks: "major", "minor", "micro", "none"
			natural:     false,		// all tick marks should be made on natural numbers
			leftBottom:  true,		// position of the axis, used with "vertical"
			includeZero: false,		// 0 should be included
			fixed:       true,		// all labels are fixed numbers
			majorLabels: true,		// draw major labels
			minorTicks:  true,		// draw minor ticks
			minorLabels: true,		// draw minor labels
			microTicks:  false,		// draw micro ticks
			htmlLabels:  true		// use HTML to draw labels
		},
		optionalParams: {
			"min":           0,		// minimal value on this axis
			"max":           1,		// maximal value on this axis
			"majorTickStep": 4,		// major tick step
			"minorTickStep": 2,		// minor tick step
			"microTickStep": 1,		// micro tick step
			"labels":        [],	// array of labels for major ticks
									// with corresponding numeric values
									// ordered by values
			// theme components
			"stroke":        {},	// stroke for an axis
			"majorTick":     {},	// stroke + length for a tick
			"minorTick":     {},	// stroke + length for a tick
			"font":          "",	// font for labels
			"fontColor":     ""		// color for labels as a string
		},

		constructor: function(chart, kwArgs){
			this.opt = dojo.clone(this.defaultParams);
			du.updateWithObject(this.opt, kwArgs);
			du.updateWithPattern(this.opt, kwArgs, this.optionalParams);
		},
		dependOnData: function(){
			return !("min" in this.opt) || !("max" in this.opt);
		},
		clear: function(){
			delete this.scaler;
			this.dirty = true;
			return this;
		},
		initialized: function(){
			return "scaler" in this;
		},
		calculate: function(min, max, span, labels){
			if(this.initialized()){ return this; }
			this.labels = "labels" in this.opt ? this.opt.labels : labels;
			if("min" in this.opt){ min = this.opt.min; }
			if("max" in this.opt){ max = this.opt.max; }
			if(this.opt.includeZero){
				if(min > 0){ min = 0; }
				if(max < 0){ max = 0; }
			}
			var minMinorStep = 0, ta = this.chart.theme.axis, 
				taFont = "font" in this.opt ? this.opt.font : ta.font,
				size = taFont ? g.normalizedLength(g.splitFontString(taFont).size) : 0;
			if(this.vertical){
				if(size){
					minMinorStep = size + labelGap;
				}
			}else{
				if(size){
					var labelLength = Math.ceil(Math.log(Math.max(Math.abs(min), Math.abs(max))) / Math.LN10);
					if(min < 0 || max < 0){ ++labelLength; }
					var precision = Math.floor(Math.log(max - min) / Math.LN10);
					if(precision > 0){ labelLength += precision; }
					if(this.labels){
						labelLength = df.foldl(df.map(this.labels, "x.text.length"), "Math.max(a, b)", labelLength);
					}
					minMinorStep = Math.floor(size * labelLength * labelFudgeFactor) + labelGap;
				}
			}
			var kwArgs = {
				fixUpper: this.opt.fixUpper, 
				fixLower: this.opt.fixLower, 
				natural:  this.opt.natural
			};
			if("majorTickStep" in this.opt){ kwArgs.majorTick = this.opt.majorTickStep; }
			if("minorTickStep" in this.opt){ kwArgs.minorTick = this.opt.minorTickStep; }
			if("microTickStep" in this.opt){ kwArgs.microTick = this.opt.microTickStep; }
			this.scaler = dojox.charting.scaler(min, max, span, kwArgs);
			this.scaler.minMinorStep = minMinorStep;
			return this;
		},
		getScaler: function(){
			return this.scaler;
		},
		getOffsets: function(){
			var offsets = {l: 0, r: 0, t: 0, b: 0};
			var offset = 0, ta = this.chart.theme.axis,
				taFont = "font" in this.opt ? this.opt.font : ta.font,
				taMajorTick = "majorTick" in this.opt ? this.opt.majorTick : ta.majorTick,
				taMinorTick = "minorTick" in this.opt ? this.opt.minorTick : ta.minorTick,
				size = taFont ? g.normalizedLength(g.splitFontString(taFont).size) : 0;
			if(this.vertical){
				if(size){
					var s = this.scaler,
						a = this._getLabel(s.major.start, s.major.prec).length,
						b = this._getLabel(s.major.start + s.major.count * s.major.tick, s.major.prec).length,
						c = this._getLabel(s.minor.start, s.minor.prec).length,
						d = this._getLabel(s.minor.start + s.minor.count * s.minor.tick, s.minor.prec).length,
						labelLength = Math.max(a, b, c, d);
					if(this.labels){
						labelLength = df.foldl(df.map(this.labels, "x.text.length"), "Math.max(a, b)", labelLength);
					}
					offset = Math.floor(size * labelLength * labelFudgeFactor) + labelGap;
				}
				offset += labelGap + Math.max(taMajorTick.length, taMinorTick.length);
				offsets[this.opt.leftBottom ? "l" : "r"] = offset;
				offsets.t = offsets.b = size / 2;
			}else{
				if(size){
					offset = size + labelGap;
				}
				offset += labelGap + Math.max(taMajorTick.length, taMinorTick.length);
				offsets[this.opt.leftBottom ? "b" : "t"] = offset;
				if(size){
					var s = this.scaler,
						a = this._getLabel(s.major.start, s.major.prec).length,
						b = this._getLabel(s.major.start + s.major.count * s.major.tick, s.major.prec).length,
						c = this._getLabel(s.minor.start, s.minor.prec).length,
						d = this._getLabel(s.minor.start + s.minor.count * s.minor.tick, s.minor.prec).length,
						labelLength = Math.max(a, b, c, d);
					if(this.labels){
						labelLength = df.foldl(df.map(this.labels, "x.text.length"), "Math.max(a, b)", labelLength);
					}
					offsets.l = offsets.r = Math.floor(size * labelLength * labelFudgeFactor) / 2;
				}
			}
			return offsets;
		},
		render: function(dim, offsets){
			if(!this.dirty){ return this; }
			// prepare variable
			var start, stop, axisVector, tickVector, labelOffset, labelAlign,
				ta = this.chart.theme.axis, 
				taStroke = "stroke" in this.opt ? this.opt.stroke : ta.stroke,
				taMajorTick = "majorTick" in this.opt ? this.opt.majorTick : ta.majorTick,
				taMinorTick = "minorTick" in this.opt ? this.opt.minorTick : ta.minorTick,
				taFont = "font" in this.opt ? this.opt.font : ta.font,
				taFontColor = "fontColor" in this.opt ? this.opt.fontColor : ta.fontColor,
				tickSize = Math.max(taMajorTick.length, taMinorTick.length),
				size = taFont ? g.normalizedLength(g.splitFontString(taFont).size) : 0;
			if(this.vertical){
				start = {y: dim.height - offsets.b};
				stop  = {y: offsets.t};
				axisVector = {x: 0, y: -1};
				if(this.opt.leftBottom){
					start.x = stop.x = offsets.l;
					tickVector = {x: -1, y: 0};
					labelAlign = "end";
				}else{
					start.x = stop.x = dim.width - offsets.r;
					tickVector = {x: 1, y: 0};
					labelAlign = "start";
				}
				labelOffset = {x: tickVector.x * (tickSize + labelGap), y: size * 0.4};
			}else{
				start = {x: offsets.l};
				stop  = {x: dim.width - offsets.r};
				axisVector = {x: 1, y: 0};
				labelAlign = "middle";
				if(this.opt.leftBottom){
					start.y = stop.y = dim.height - offsets.b;
					tickVector = {x: 0, y: 1};
					labelOffset = {y: tickSize + labelGap + size};
				}else{
					start.y = stop.y = offsets.t;
					tickVector = {x: 0, y: -1};
					labelOffset = {y: -tickSize - labelGap};
				}
				labelOffset.x = 0;
			}
			
			// render shapes
			this.cleanGroup();
			var s = this.group, c = this.scaler, step, next,
				nextMajor = c.major.start, nextMinor = c.minor.start, nextMicro = c.micro.start;
			s.createLine({x1: start.x, y1: start.y, x2: stop.x, y2: stop.y}).setStroke(taStroke);
			if(this.opt.microTicks && c.micro.tick){
				step = c.micro.tick, next = nextMicro;
			}else if(this.opt.minorTicks && c.minor.tick){
				step = c.minor.tick, next = nextMinor;
			}else if(c.major.tick){
				step = c.major.tick, next = nextMajor;
			}else{
				// don't draw anything
				return this;
			}
			while(next <= c.bounds.upper + 1/c.scale){
				var offset = (next - c.bounds.lower) * c.scale,
					x = start.x + axisVector.x * offset,
					y = start.y + axisVector.y * offset;
				if(Math.abs(nextMajor - next) < step / 2){
					// major tick
					s.createLine({
						x1: x, y1: y,
						x2: x + tickVector.x * taMajorTick.length,
						y2: y + tickVector.y * taMajorTick.length
					}).setStroke(taMajorTick);
					if(this.opt.majorLabels){
						var elem = dc.axis2d.common.createText[this.opt.htmlLabels ? "html" : "gfx"]
										(this.chart, s, x + labelOffset.x, y + labelOffset.y, labelAlign,
											this._getLabel(nextMajor, c.major.prec), taFont, taFontColor);
						if(this.opt.htmlLabels){ this.htmlElements.push(elem); }
					}
					nextMajor += c.major.tick;
					nextMinor += c.minor.tick;
					nextMicro += c.micro.tick;
				}else if(Math.abs(nextMinor - next) < step / 2){
					// minor tick
					if(this.opt.minorTicks){
						s.createLine({
							x1: x, y1: y,
							x2: x + tickVector.x * taMinorTick.length,
							y2: y + tickVector.y * taMinorTick.length
						}).setStroke(taMinorTick);
						if(this.opt.minorLabels && (c.minMinorStep <= c.minor.tick * c.scale)){
							var elem = dc.axis2d.common.createText[this.opt.htmlLabels ? "html" : "gfx"]
											(this.chart, s, x + labelOffset.x, y + labelOffset.y, labelAlign,
												this._getLabel(nextMinor, c.minor.prec), taFont, taFontColor);
							if(this.opt.htmlLabels){ this.htmlElements.push(elem); }
						}
					}
					nextMinor += c.minor.tick;
					nextMicro += c.micro.tick;
				}else{
					// micro tick
					if(this.opt.microTicks){
						s.createLine({
							x1: x, y1: y,
							// use minor ticks for now
							x2: x + tickVector.x * taMinorTick.length,
							y2: y + tickVector.y * taMinorTick.length
						}).setStroke(taMinorTick);
					}
					nextMicro += c.micro.tick;
				}
				next += step;
			}
			this.dirty = false;
			return this;
		},
		
		// utilities
		_getLabel: function(number, precision){
			if(this.opt.labels){
				// classic binary search
				var l = this.opt.labels, lo = 0, hi = l.length;
				while(lo < hi){
					var mid = Math.floor((lo + hi) / 2), val = l[mid].value;
					if(val < number){
						lo = mid + 1;
					}else{
						hi = mid;
					}
				}
				// lets take into account FP errors
				if(lo < l.length && eq(l[lo].value, number)){
					return l[lo].text;
				}
				--lo;
				if(lo < l.length && eq(l[lo].value, number)){
					return l[lo].text;
				}
				lo += 2;
				if(lo < l.length && eq(l[lo].value, number)){
					return l[lo].text;
				}
				// otherwise we will produce a number
			}
			return this.opt.fixed ? number.toFixed(precision < 0 ? -precision : 0) : number.toString();
		}
	});
})();

}
