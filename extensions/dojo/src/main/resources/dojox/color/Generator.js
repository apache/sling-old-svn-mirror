if(!dojo._hasResource["dojox.color.Generator"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.color.Generator"] = true;
dojo.provide("dojox.color.Generator");

dojox.color.Generator = new (function(){
	var dxc=dojox.color;

	//	common helper functions
	var prep=function(obj, fnName){
		if(!obj){
			console.warn("dojox.color.Generator::", fnName, ": no base color was passed. ", obj);
			return null;
		}
		if(!obj.toHsv){
			//	either a raw string or object, return a Color.
			obj=new dxc.Color(obj);
		}
		return obj;
	};

	var factors=function(n, high, low){
		var ret=[];
		var i, step=(high-low)/n, cur=high;
		for(i=0; i<n; i++,cur-=step){ ret.push(cur); }
		return ret;
	};

	var fill=function(color, num, factors){
		var c=factors.length-1, a=[], r, g, b;
		for(var i=0; i<num; i++){
			if(i<factors.length){
				 r=color.r+(255-color.r)*factors[i], 
				 g=color.g+(255-color.g)*factors[i], 
				 b=color.b+(255-color.b)*factors[i];
				 a.push(new dxc.Color({ r:r, g:g, b:b }));
			}
			else if(i==factors.length){
				a.push(color);
			}
			else {
				if(c<0){ c=factors.length-1; }	//	just in case.
				r=color.r*(1-factors[c]), 
				g=color.g*(1-factors[c]), 
				b=color.b*(1-factors[c--]);
				a.push(new dxc.Color({ r:r, g:g, b:b }));
			}
		}
		return a;
	};
	
	var flatten=function(matrix, limit, ord){
		//	todo: set up the ordering thing.
		var ret=[];
		for(var i=0; i<matrix[0].length; i++){
			for(var j=0; j<matrix.length; j++){
				ret.push(matrix[j][i]);
			}
		}
		ret.length=limit;
		return ret;
	};

	//	the color generator
	this.analogous= function(kwArgs){
		//	summary
		//	generates n colors based on a base color, based on a fixed hue angle delta
		//	(relative to the base hue) with slight variations in saturation.
		kwArgs=dojo.mixin({
			series:4,				//	number of analogous lines to generate
			num:32,					//	number of colors to derive
			order:"bottom up",		//	the order of the returned color array
			angle:30,				//	the angle of difference to use
			high:0.5,				//	high part of range to generate tints and shades
			low:0.15				//	low part of range to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "analogous");
		if(!base){ return []; }

		//	let start the generation.  We use series to move further away from the center.
		var num=kwArgs.num, hsv=base.toHsv();
		var rows=kwArgs.series+1, cols=Math.ceil(num/rows);
		var fs=factors(Math.floor(cols/2), kwArgs.high, kwArgs.low);

		var m=[], cur=hsv.h-(kwArgs.angle*(kwArgs.series/2));
		for(var i=0; i<rows; i++,cur+=kwArgs.angle){
			if(cur<0) { cur+=360 ; }
			if(cur>=360){ cur-=360; }
			m.push(fill(dxc.fromHsv({ h: cur, s:hsv.s, v:hsv.v }), cols, fs));
		}
		return flatten(m, num, kwArgs.order);	//	Array
	};
	
	this.monochromatic = function(kwArgs){
		//	summary
		//	generates n colors based on a base color, using alterations to the RGB model only.
		kwArgs=dojo.mixin({
			num:32,					//	number of colors to derive
			high:0.5,				//	high factor to generate tints and shades
			low:0.15				//	low factor to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "monochromatic");
		if(!base){ return []; }

		var fs=factors(Math.floor(kwArgs.num/2), kwArgs.high, kwArgs.low);
		var a=fill(base, kwArgs.num, fs);
		return a;	// Array
	};
	
	this.triadic = function(kwArgs){
		//	summary
		//	generates n colors from a base color, using the triadic rules, rough
		//	approximation from kuler.adobe.com.
		kwArgs=dojo.mixin({
			num:32,					//	number of colors to derive
			order:"bottom up",		//	the order of the returned color array
			high:0.5,				//	high factor to generate tints and shades
			low:0.15				//	low factor to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "triadic");
		if(!base){ return []; }

		var num=kwArgs.num, rows=3, cols=Math.ceil(num/rows), fs=factors(Math.floor(cols/2), kwArgs.high, kwArgs.low);
		var m=[], hsv=base.toHsv();

		//	hue calculations
		var h1=hsv.h+57, h2=hsv.h-157;
		if(h1>360){ h1-=360; }
		if(h2<0){ h2+=360; }

		//	sat calculations
		var s1=(hsv.s>=20) ? hsv.s-10 : hsv.s+10;
		var s2=(hsv.s>=95) ? hsv.s-5 : hsv.s+5;

		//	value calcs
		var v2=(hsv.v>=70) ? hsv.v-30 : hsv.v+30;
		
		m.push(fill(dojox.color.fromHsv({ h:h1, s:s1, v:hsv.v }), cols, fs));
		m.push(fill(base, cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:h2, s:s2, v:v2 }), cols, fs));
		return flatten(m, num, kwArgs.order);	//	Array
	};
	
	this.complementary = function(kwArgs){
		//	summary
		//	generates n colors from a base color, using complimentary rules.
		kwArgs=dojo.mixin({
			num:32,					//	number of colors to derive
			order:"bottom up",		//	the order of the returned color array
			high:0.5,				//	high factor to generate tints and shades
			low:0.15				//	low factor to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "complimentary");
		if(!base){ return []; }

		var num=kwArgs.num, rows=2, cols=Math.ceil(num/rows), fs=factors(Math.floor(cols/2), kwArgs.high, kwArgs.low);
		var m=[], hsv=base.toHsv();
		var compliment=(hsv.h+120)%360;
		m.push(fill(base, cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:compliment, s:hsv.s, v:hsv.v }), cols, fs));
		return flatten(m, num, kwArgs.order);	//	Array
	};
	
	this.splitComplementary = function(kwArgs){
		//	summary
		//	generates n colors from a base color, using split complimentary rules.
		kwArgs=dojo.mixin({
			num:32,					//	number of colors to derive
			order:"bottom up",		//	the order of the returned color array
			angle:30,				//	the angle of difference to use
			high:0.5,				//	high factor to generate tints and shades
			low:0.15				//	low factor to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "splitComplementary");
		if(!base){ return []; }

		var num=kwArgs.num, rows=3, cols=Math.ceil(num/rows), fs=factors(Math.floor(cols/2), kwArgs.high, kwArgs.low);
		var m=[], hsv=base.toHsv();
		var compliment=(hsv.h+120)%360;
		var comp1=compliment-kwArgs.angle, comp2=(compliment+kwArgs.angle)%360;
		if(comp1<0){ comp1+=360; }
		
		m.push(fill(base, cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:comp1, s:hsv.s, v:hsv.v }), cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:comp2, s:hsv.s, v:hsv.v }), cols, fs));
		return flatten(m, num, kwArgs.order);	//	Array
	};
	
	this.compound = function(kwArgs){
		//	summary
		//	generates n colors from a base color, using a *very* rough approximation
		//	of the Compound rules at http://kuler.adobe.com
		kwArgs=dojo.mixin({
			num:32,					//	number of colors to derive
			order:"bottom up",		//	the order of the returned color array
			angle:30,				//	the angle of difference to use
			high:0.5,				//	high factor to generate tints and shades
			low:0.15				//	low factor to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "compound");
		if(!base){ return []; }

		var num=kwArgs.num, rows=4, cols=Math.ceil(num/rows), fs=factors(Math.floor(cols/2), kwArgs.high, kwArgs.low);
		var m=[], hsv=base.toHsv();
		var comp=(hsv.h+120)%360;		//	other base angle.

		//	hue calculations
		var h1=(hsv.h+kwArgs.angle)%360, h2=comp-kwArgs.angle, h3=comp-(kwArgs.angle/2);
		if(h2<0){ h2+=360; }
		if(h3<0){ h3+=360; }

		//	saturation calculations
		var s1=(hsv.s>=90 && hsv.s<=100)? hsv.s-10 : hsv.s+10;
		var s2=(hsv.s<=35) ? hsv.s+25 : hsv.s-25;

		//	value calculations
		var v1=hsv.v-20;
		var v2=hsv.v;
		
		m.push(fill(base, cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:h1, s:s1, v:v1 }), cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:h2, s:s1, v:v1 }), cols, fs));
		m.push(fill(dojox.color.fromHsv({ h:h3, s:s2, v:v2 }), cols, fs));
		return flatten(m, num, kwArgs.order);	//	Array
	};
	
	this.shades = function(kwArgs){
		//	summary
		//	generates n colors based on a base color using only changes
		//	in value.  Similar to monochromatic but a bit more linear.
		kwArgs=dojo.mixin({
			num:32,					//	number of colors to derive
			high:1.5,				//	high factor to generate tints and shades
			low:0.5					//	low factor to generate tints and shades
		}, kwArgs||{});
		
		var base=prep(kwArgs.base, "shades");
		if(!base){ return []; }

		var num=kwArgs.num, hsv=base.toHsv();
		var step=(kwArgs.high-kwArgs.low)/num, cur=kwArgs.low;
		var a=[];
		for(var i=0; i<num; i++,cur+=step){
			a.push(dxc.fromHsv({ h:hsv.h, s:hsv.s, v:Math.min(Math.round(hsv.v*cur),100) }));
		}

		console.log("generated color list from shades: ", a);
		return a;	// Array
	};
})();

}
