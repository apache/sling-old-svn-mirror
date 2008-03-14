if(!dojo._hasResource["dojox.dtl.tag.event"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.tag.event"] = true;
dojo.provide("dojox.dtl.tag.event");

dojo.require("dojox.dtl._base");

dojox.dtl.tag.event.EventNode = function(type, fn){
	this._type = type;
	this.contents = fn;
}
dojo.extend(dojox.dtl.tag.event.EventNode, {
	render: function(context, buffer){
		if(!this._clear){
			buffer.getParent()[this._type] = null;
			this._clear = true;
		}
		if(this.contents && !this._rendered){
			if(!context.getThis()) throw new Error("You must use Context.setObject(instance)");
			this._rendered = dojo.connect(buffer.getParent(), this._type, context.getThis(), this.contents);
		}
		return buffer;
	},
	unrender: function(context, buffer){
		if(this._rendered){
			dojo.disconnect(this._rendered);
			this._rendered = false;
		}
		return buffer;
	},
	clone: function(){
		return new dojox.dtl.tag.event.EventNode(this._type, this.contents);
	},
	toString: function(){ return "dojox.dtl.tag.event." + this._type; }
});

dojox.dtl.tag.event.on = function(parser, text){
	// summary: Associates an event type to a function (on the current widget) by name
	var parts = text.split(" ");
	return new dojox.dtl.tag.event.EventNode(parts[0], parts[1]);
}

}
