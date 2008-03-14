if(!dojo._hasResource["dojox.layout.BorderContainer"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.layout.BorderContainer"] = true;
dojo.provide("dojox.layout.BorderContainer");

dojo.require("dijit.layout._LayoutWidget");

dojo.experimental("dojox.layout.BorderContainer");

dojo.declare(
	"dojox.layout.BorderContainer",
//	[dijit._Widget, dijit._Container, dijit._Contained],
	dijit.layout._LayoutWidget,
{
	// summary
	//	Provides layout in 5 regions, a center and borders along its 4 sides.
	//
	// details
	//	A BorderContainer is a box with a specified size (like style="width: 500px; height: 500px;"),
	//	that contains children widgets marked with "position" of "top", "bottom", "left", "right", "center".
	//	It takes it's children marked as top/bottom/left/right, and lays them out along the edges of the center box,
	//	with "top" and "bottom" extending the full width of the container.
	//
	// usage
	//	<style>
	//		html, body{ height: 100%; width: 100%; }
	//	</style>
	//	<div dojoType="BorderContainer" style="width: 100%; height: 100%">
	//		<div dojoType="ContentPane" position="top">header text</div>
	//		<div dojoType="ContentPane" position="right" style="width: 200px;">table of contents</div>
	//		<div dojoType="ContentPane" position="center">client area</div>
	//	</div>
	
	top: {},
	bottom: {},
	left: {}, // inside?
	right: {}, // outside?
	center: {},

	layout: function(){
		this._layoutChildren(this.domNode, this._contentBox, this.getChildren());
	},

	addChild: function(/*Widget*/ child, /*Integer?*/ insertIndex){
		dijit._Container.prototype.addChild.apply(this, arguments);
		if(this._started){
			this._layoutChildren(this.domNode, this._contentBox, this.getChildren());
		}
	},

	removeChild: function(/*Widget*/ widget){
		dijit._Container.prototype.removeChild.apply(this, arguments);
		if(this._started){
			this._layoutChildren(this.domNode, this._contentBox, this.getChildren());
		}
	},

	_layoutChildren: function(/*DomNode*/ container, /*Object*/ dim, /*Object[]*/ children){
		/**
		 * summary
		 *		Layout a bunch of child dom nodes within a parent dom node
		 * container:
		 *		parent node
		 * dim:
		 *		{l, t, w, h} object specifying dimensions of container into which to place children
		 * children:
		 *		an array like [ {domNode: foo, position: "bottom" }, {domNode: bar, position: "client"} ]
		 */

//TODO: what is dim and why doesn't it look right?
		// copy dim because we are going to modify it
//		dim = dojo.mixin({}, dim);

		this.domNode.style.position = "relative";

		//FIXME: do this once? somewhere else?
		dojo.addClass(container, "dijitBorderContainer");
		dojo.forEach(children, function(child){
			var style = child.domNode.style;
			style.position = "absolute";
			if(child.position){
				this[child.position] = child.domNode;
			}
		}, this);

		var topStyle = this.top.style;
		var rightStyle = this.right.style;
		var leftStyle = this.left.style;
		var centerStyle = this.center.style;
		var bottomStyle = this.bottom.style;
		var rightCoords = dojo.coords(this.right);
		var leftCoords = dojo.coords(this.left);
		var centerCoords = dojo.coords(this.center);
		var bottomCoords = dojo.coords(this.bottom);
		var topCoords = dojo.coords(this.top);
		rightStyle.top = leftStyle.top = centerStyle.top = topCoords.h + "px";
		topStyle.top = topStyle.left = topStyle.right = "0px";
		bottomStyle.left = bottomStyle.bottom = bottomStyle.right = "0px";
		leftStyle.left = rightStyle.right = "0px";
		centerStyle.left = leftCoords.w + "px";
		centerStyle.right =  rightCoords.w + "px";
		rightStyle.bottom = leftStyle.bottom = centerStyle.bottom = bottomCoords.h + "px";
	},

	resize: function(args){
		this.layout();
	}
});

// This argument can be specified for the children of a BorderContainer.
// Since any widget can be specified as a LayoutContainer child, mix it
// into the base widget class.  (This is a hack, but it's effective.)
dojo.extend(dijit._Widget, {
	// position: String
	//		"top", "bottom", "left", "right", "center".
	//		See the BorderContainer description for details on this parameter.
	position: 'none'
});

}
