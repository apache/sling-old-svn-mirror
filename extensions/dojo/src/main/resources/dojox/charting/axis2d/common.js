if(!dojo._hasResource["dojox.charting.axis2d.common"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.charting.axis2d.common"] = true;
dojo.provide("dojox.charting.axis2d.common");

dojo.require("dojox.gfx");

(function(){
	var g = dojox.gfx;
	
	dojo.mixin(dojox.charting.axis2d.common, {
		createText: {
			gfx: function(chart, creator, x, y, align, text, font, fontColor){
				return creator.createText({
					x: x, y: y, text: text, align: align
				}).setFont(font).setFill(fontColor);
			},
			html: function(chart, creator, x, y, align, text, font, fontColor){
				var p = dojo.doc.createElement("div"), s = p.style;
				s.marginLeft   = "0px";
				s.marginTop    = "0px";
				s.marginRight  = "0px";
				s.marginBottom = "0px";
				s.paddingLeft   = "0px";
				s.paddingTop    = "0px";
				s.paddingRight  = "0px";
				s.paddingBottom = "0px";
				s.borderLeftWidth   = "0px";
				s.borderTopWidth    = "0px";
				s.borderRightWidth  = "0px";
				s.borderBottomWidth = "0px";
				s.position = "absolute";
				s.font = font;
				p.innerHTML = text;
				s.color = fontColor;
				chart.node.appendChild(p);
				var parent = chart.getCoords(), 
					box = dojo.marginBox(p),
					size = g.normalizedLength(g.splitFontString(font).size),
					top = parent.y + Math.floor(y - size);
				switch(align){
					case "middle":
						dojo.marginBox(p, {l: parent.x + Math.floor(x - box.w / 2), t: top});
						break;
					case "end":
						dojo.marginBox(p, {l: parent.x + Math.floor(x - box.w), t: top});
						break;
					//case "start":
					default:
						dojo.marginBox(p, {l: parent.x + Math.floor(x), t: top});
						break;
				}
				return p;
			}
		}
	});
})();

}
