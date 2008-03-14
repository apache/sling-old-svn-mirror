if(!dojo._hasResource["dojox.dtl.render.html"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.render.html"] = true;
dojo.provide("dojox.dtl.render.html");

dojox.dtl.render.html.sensitivity = {
	// summary:
	//		Set conditions under which to buffer changes
	// description:
	//		Necessary if you make a lot of changes to your template.
	//		What happens is that the entire node, from the attached DOM Node
	//		down gets swapped with a clone, and until the entire rendering
	//		is complete, we don't replace the clone again. In this way, renders are
	//		"batched".
	//
	//		But, if we're only changing a small number of nodes, we might no want to buffer at all.
	//		The higher numbers mean that even small changes will result in buffering.
	//		Each higher level includes the lower levels.
	NODE: 1, // If a node changes, implement buffering
	ATTRIBUTE: 2, // If an attribute or node changes, implement buffering
	TEXT: 3 // If any text at all changes, implement buffering
}
dojox.dtl.render.html.Render = function(/*DOMNode?*/ attachPoint, /*dojox.dtl.HtmlTemplate?*/ tpl){
	this._tpl = tpl;
	this._node = attachPoint;
	this._swap = dojo.hitch(this, function(){
		// summary: Swaps the node out the first time the DOM is changed
		// description: Gets swapped back it at end of render
		if(this._node === this._tpl.getRootNode()){
			var frag = this._node;
			this._node = this._node.cloneNode(true);
			frag.parentNode.replaceChild(this._node, frag);
		}
	});
}
dojo.extend(dojox.dtl.render.html.Render, {
	sensitivity: dojox.dtl.render.html.sensitivity,
	setAttachPoint: function(/*Node*/ node){
		this._node = node;
	},
	render: function(/*dojox.dtl.HtmlTemplate*/ tpl, /*Object*/ context, /*dojox.dtl.HtmlBuffer?*/ buffer){
		if(!this._node){
			throw new Error("You cannot use the Render object without specifying where you want to render it");
		}

		buffer = buffer || tpl.getBuffer();

		if(context.getThis() && context.getThis().buffer == this.sensitivity.NODE){
			var onAddNode = dojo.connect(buffer, "onAddNode", this, "_swap");
			var onRemoveNode = dojo.connect(buffer, "onRemoveNode", this, "_swap");
		}

		if(this._tpl && this._tpl !== tpl){
			this._tpl.unrender(context, buffer);
		}
		this._tpl = tpl;

		var frag = tpl.render(context, buffer).getParent();

		dojo.disconnect(onAddNode);
		dojo.disconnect(onRemoveNode);

		if(this._node !== frag){
			this._node.parentNode.replaceChild(frag, this._node);
			dojo._destroyElement(this._node);
			this._node = frag;
		}
	}
});

}
