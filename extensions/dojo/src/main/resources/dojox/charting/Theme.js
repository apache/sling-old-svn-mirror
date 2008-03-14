if(!dojo._hasResource["dojox.charting.Theme"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.Theme"] = true;
dojo.provide("dojox.charting.Theme");
dojo.require("dojox.charting._color");

(function(){
	var dxc=dojox.charting;
	//	TODO: Legend information
	dxc.Theme=function(/*object?*/kwArgs){
		kwArgs=kwArgs||{};
		this.chart=dojo.mixin(dojo.clone(dxc.Theme._def.chart), kwArgs.chart||{});
		this.plotarea=dojo.mixin(dojo.clone(dxc.Theme._def.plotarea), kwArgs.plotarea||{});
		this.axis=dojo.mixin(dojo.clone(dxc.Theme._def.axis), kwArgs.axis||{});
		this.series=dojo.mixin(dojo.clone(dxc.Theme._def.series), kwArgs.series||{});
		this.marker=dojo.mixin(dojo.clone(dxc.Theme._def.marker), kwArgs.marker||{});
		this.markers=dojo.mixin(dojo.clone(dxc.Theme.Markers), kwArgs.markers||{});
		this.colors=[];
		this.antiAlias=("antiAlias" in kwArgs)?kwArgs.antiAlias:true;
		this.assignColors=("assignColors" in kwArgs)?kwArgs.assignColors:true;
		this.assignMarkers=("assignMarkers" in kwArgs)?kwArgs.assignMarkers:true;
		this._colorCache=null;

		//	push the colors, use _def colors if none passed.
		kwArgs.colors=kwArgs.colors||dxc.Theme._def.colors;
		dojo.forEach(kwArgs.colors, function(item){ 
			this.colors.push(item); 
		}, this);

		//	private variables for color and marker indexing
		this._current={ color:0, marker: 0 };
		this._markers=[];
		this._buildMarkerArray();
	};

	//	"static" fields
	//	default markers.
	//	A marker is defined by an SVG path segment; it should be defined as
	//		relative motion, and with the assumption that the path segment
	//		will be moved to the value point (i.e prepend Mx,y)
	dxc.Theme.Markers={
		CIRCLE:"m-3,0 c0,-4 6,-4 6,0 m-6,0 c0,4 6,4 6,0", 
		SQUARE:"m-3,-3 l0,6 6,0 0,-6 z", 
		DIAMOND:"m0,-3 l3,3 -3,3 -3,-3 z", 
		CROSS:"m0,-3 l0,6 m-3,-3 l6,0", 
		X:"m-3,-3 l6,6 m0,-6 l-6,6", 
		TRIANGLE:"m-3,3 l3,-6 3,6 z", 
		TRIANGLE_INVERTED:"m-3,-3 l3,6 3,-6 z"
	};
	dxc.Theme._def={
		//	all objects are structs used directly in dojox.gfx
		chart:{ 
			stroke:null,
			fill: "white"
		},
		plotarea:{ 
			stroke:null,
			fill: "white"
		},
		//	TODO: label rotation on axis
		axis:{
			stroke:{ color:"#333",width:1 },							//	the axis itself
			line:{ color:"#ccc",width:1,style:"Dot",cap:"round" },		//	gridlines
			majorTick:{ color:"#666", width:1, length:6, position:"center" },	//	major ticks on axis
			minorTick:{ color:"#666", width:0.8, length:3, position:"center" },	//	minor ticks on axis
			font:"normal normal normal 7pt Tahoma",						//	labels on axis
			fontColor:"#333"											//	color of labels
		},
		series:{
			outline: {width: 2, color: "#ccc"},							//	line or outline
			stroke: {width: 2, color: "#333"},							//	line or outline
			fill: "#ccc",												//	fill, if appropriate
			font: "normal normal normal 7pt Tahoma",					//	if there's a label
			fontColor: "#000"											// 	color of labels
		},
		marker:{	//	any markers on a series.
			stroke: {width:1},											//	stroke or outline
			fill: "#333",												//	fill if needed
			font: "normal normal normal 7pt Tahoma",					//	label
			fontColor: "#000"
		},
		colors:[
			"#000","#111","#222","#333",
			"#444","#555","#666","#777",
			"#888","#999","#aaa","#bbb",
			"#ccc"
		]
	};
	
	//	prototype methods
	dojo.extend(dxc.Theme, {
		defineColors: function(obj){
			//	we can generate a set of colors based on keyword arguments
			var kwArgs=obj||{};

			//	deal with caching
			var cache=false;
			if(kwArgs.cache===undefined){ cache=true; }
			if(kwArgs.cache==true){ cache=true; }
			
			if(cache){
				this._colorCache=kwArgs;
			} else {
				var mix=this._colorCache||{};
				kwArgs=dojo.mixin(dojo.clone(mix), kwArgs);
			}

			var c=[], n=kwArgs.num||32;	//	the number of colors to generate
			if(kwArgs.colors){
				//	we have an array of colors predefined, so fix for the number of series.
				var l=kwArgs.colors.length;
				for(var i=0; i<n; i++){
					c.push(kwArgs.colors[i%l]);
				}
				this.colors=c;
			}
			else if(kwArgs.hue){
				//	single hue, generate a set based on brightness
				var s=kwArgs.saturation||100;	//	saturation
				var st=kwArgs.low||30;
				var end=kwArgs.high||90;
				var step=(end-st)/n;			//	brightness steps
				for(var i=0; i<n; i++){
					c.push(dxc._color.fromHsb(kwArgs.hue, s, st+(step*i)).toHex());
				}
				this.colors=c;
			}
			else if(kwArgs.stops){
				//	create color ranges that are either equally distributed, or
				//	(optionally) based on a passed "offset" property.  If you
				//	pass an array of Colors, it will equally distribute, if
				//	you pass an array of structs { color, offset }, it will
				//	use the offset (0.0 - 1.0) to distribute.  Note that offset
				//	values should be plotted on a line from 0.0 to 1.0--i.e.
				//	they should be additive.  For example:
				//	[ {color, offset:0}, { color, offset:0.2 }, { color, offset:0.5 }, { color, offset:1.0 } ]
				//	
				//	If you use stops for colors, you MUST have a color at 0.0 and one
				//	at 1.0.
			
				//	figure out how many stops we have
				var l=kwArgs.stops.length;
				if(l<2){
					throw new Error(
						"dojox.charting.Theme::defineColors: when using stops to "
						+ "define a color range, you MUST specify at least 2 colors."
					);
				}

				//	figure out if the distribution is equal or not.  Note that
				//	colors may not exactly match the stops you define; because
				//	color generation is linear (i.e. evenly divided on a linear
				//	axis), it's very possible that a color will land in between
				//	two stops and not exactly *at* a stop.
				//
				//	The only two colors guaranteed will be the end stops (i.e.
				//	the first and last stop), which will *always* be set as
				//	the end stops.
				if(typeof(kwArgs.stops[0].offset)=="undefined"){ 
					//	set up equal offsets
					var off=1/(l-1);
					for(var i=0; i<l; i++){
						kwArgs.stops[i]={
							color:kwArgs.stops[i],
							offset:off*i
						};
					}
				}
				//	ensure the ends.
				kwArgs.stops[0].offset=0;
				kwArgs.stops[l-1].offset=1;
				kwArgs.stops.sort(function(a,b){ return a.offset-b.offset; });

				//	create the colors.
				//	first stop.
				c.push(kwArgs.stops[0].color.toHex());

				//	TODO: calculate the blend at n/steps and set the color

				//	last stop
				c.push(kwArgs.stops[l-1].color.toHex());
				this.colors=c;
			}
		},
	
		_buildMarkerArray: function(){
			this._markers=[];
			for(var p in this.markers){ this._markers.push(this.markers[p]); }
			//	reset the position
			this._current.marker=0;
		},

		addMarker:function(/* string */name, /*string*/segment){
			//	summary
			//	Add a custom marker to this theme.
			//
			//	example
			//	myTheme.addMarker("Ellipse", foo);
			this.markers[name]=segment;
			this._buildMarkerArray();
		},
		setMarkers:function(/* object */obj){
			//	summary
			//	Set all the markers of this theme at once.  obj should be
			//	a dictionary of keys and path segments.
			//
			//	example
			//	myTheme.setMarkers({ "CIRCLE": foo });
			this.markers=obj;
			this._buildMarkerArray();
		},

		next: function(/* string? */type){
			//	summary
			//	get either the next color or the next marker, depending on what was
			//	passed.  If type is not passed, it assumes color.
			//
			//	example
			//	var color=myTheme.next();
			//	var color=myTheme.next("color");
			//	var marker=myTheme.next("marker");
			if(!type) type="color";
			if(type=="color"){
				return this.colors[this._current.color++%this.colors.length];
			}
			else if(type=="marker"){
				return this._markers[this._current.marker++%this._markers.length];
			}
		},
		clear: function(){
			this._current = {color: 0, marker: 0};
		}
	});
})();

}
