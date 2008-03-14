if(!dojo._hasResource["dojox.charting.themes.PlotKit.cyan"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.themes.PlotKit.cyan"] = true;
dojo.provide("dojox.charting.themes.PlotKit.cyan");
dojo.require("dojox.charting.Theme");

(function(){
	var dxc=dojox.charting;
	dxc.themes.PlotKit.cyan=new dxc.Theme({
		chart:{
			stroke:null,
			fill: "white"
		},
		plotarea:{
			stroke:null,
			fill: "#e6f1f5"
		},
		axis:{
			stroke:{ color:"#fff",width:2 },
			line:{ color:"#fff",width:1 },
			majorTick:{ color:"#fff", width:2, length:12 },
			minorTick:{ color:"#fff", width:1, length:8 },
			font:"normal normal normal 8pt Tahoma",
			fontColor:"#999"
		},
		series:{
			outline:{ width:1, color:"#fff" },
			stroke:{ width:2, color:"#666" },
			fill:new dojo.Color([0x66, 0x66, 0x66, 0.8]),
			font:"normal normal normal 7pt Tahoma",	//	label
			fontColor:"#000"
		},
		marker:{	//	any markers on a series.
			stroke:{ width:2 },
			fill:"#333",
			font:"normal normal normal 7pt Tahoma",	//	label
			fontColor:"#000"
		},
		colors:[]
	});
	dxc.themes.PlotKit.cyan.defineColors({ hue:194, saturation:60, low:40, high:88 });
})();

}
