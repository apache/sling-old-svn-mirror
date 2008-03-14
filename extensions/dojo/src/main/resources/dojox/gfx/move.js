if(!dojo._hasResource["dojox.gfx.move"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.gfx.move"] = true;
dojo.provide("dojox.gfx.move");
dojo.experimental("dojox.gfx.move");

dojox.gfx.Mover = function(shape, e){
	// summary: an object, which makes a shape follow the mouse, 
	//	used as a default mover, and as a base class for custom movers
	// shape: dojox.gfx.Shape: a shape object to be moved
	// e: Event: a mouse event, which started the move;
	//	only clientX and clientY properties are used
	this.shape = shape;
	this.lastX = e.clientX
	this.lastY = e.clientY;
	var d = document, firstEvent = dojo.connect(d, "onmousemove", this, "onFirstMove");
	this.events = [
		dojo.connect(d, "onmousemove", this, "onMouseMove"),
		dojo.connect(d, "onmouseup",   this, "destroy"),
		// cancel text selection and text dragging
		dojo.connect(d, "ondragstart",   dojo, "stopEvent"),
		dojo.connect(d, "onselectstart", dojo, "stopEvent"),
		firstEvent
	];
	// set globals to indicate that move has started
	dojo.publish("/gfx/move/start", [this]);
	dojo.addClass(dojo.body(), "dojoMove");
};

dojo.extend(dojox.gfx.Mover, {
	// mouse event processors
	onMouseMove: function(e){
		// summary: event processor for onmousemove
		// e: Event: mouse event
		var x = e.clientX;
		var y = e.clientY;
		this.shape.applyLeftTransform({dx: x - this.lastX, dy: y - this.lastY});
		this.lastX = x;
		this.lastY = y;
		dojo.stopEvent(e);
	},
	// utilities
	onFirstMove: function(){
		// summary: it is meant to be called only once
		dojo.disconnect(this.events.pop());
	},
	destroy: function(){
		// summary: stops the move, deletes all references, so the object can be garbage-collected
		dojo.forEach(this.events, dojo.disconnect);
		// undo global settings
		dojo.publish("/gfx/move/stop", [this]);
		dojo.removeClass(dojo.body(), "dojoMove");
		// destroy objects
		this.events = this.shape = null;
	}
});

dojox.gfx.Moveable = function(shape, params){
	// summary: an object, which makes a shape moveable
	// shape: dojox.gfx.Shape: a shape object to be moved
	// params: Object: an optional object with additional parameters;
	//	following parameters are recognized:
	//		delay: Number: delay move by this number of pixels
	//		mover: Object: a constructor of custom Mover
	this.shape = shape;
	this.delay = (params && params.delay > 0) ? params.delay : 0;
	this.mover = (params && params.mover) ? params.mover : dojox.gfx.Mover;
	this.events = [
		this.shape.connect("onmousedown", this, "onMouseDown"),
		// cancel text selection and text dragging
		//dojo.connect(this.handle, "ondragstart",   dojo, "stopEvent"),
		//dojo.connect(this.handle, "onselectstart", dojo, "stopEvent")
	];
};

dojo.extend(dojox.gfx.Moveable, {
	// methods
	destroy: function(){
		// summary: stops watching for possible move, deletes all references, so the object can be garbage-collected
		dojo.forEach(this.events, "disconnect", this.shape);
		this.events = this.shape = null;
	},
	
	// mouse event processors
	onMouseDown: function(e){
		// summary: event processor for onmousedown, creates a Mover for the shape
		// e: Event: mouse event
		if(this.delay){
			this.events.push(this.shape.connect("onmousemove", this, "onMouseMove"));
			this.events.push(this.shape.connect("onmouseup", this, "onMouseUp"));
			this._lastX = e.clientX;
			this._lastY = e.clientY;
		}else{
			new this.mover(this.shape, e);
		}
		dojo.stopEvent(e);
	},
	onMouseMove: function(e){
		// summary: event processor for onmousemove, used only for delayed drags
		// e: Event: mouse event
		if(Math.abs(e.clientX - this._lastX) > this.delay || Math.abs(e.clientY - this._lastY) > this.delay){
			this.onMouseUp(e);
			new this.mover(this.shape, e);
		}
		dojo.stopEvent(e);
	},
	onMouseUp: function(e){
		// summary: event processor for onmouseup, used only for delayed delayed drags
		// e: Event: mouse event
		this.shape.disconnect(this.events.pop());
		this.shape.disconnect(this.events.pop());
	}
});

}
