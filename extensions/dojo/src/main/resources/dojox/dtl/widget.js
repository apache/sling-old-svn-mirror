if(!dojo._hasResource["dojox.dtl.widget"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.widget"] = true;
dojo.provide("dojox.dtl.widget");

dojo.require("dijit._Widget");
dojo.require("dijit._Container")
dojo.require("dojox.dtl.html");
dojo.require("dojox.dtl.render.html");

dojo.declare("dojox.dtl._Widget", [dijit._Widget, dijit._Contained],
	{
		buffer: 0,
		buildRendering: function(){
			this.domNode = this.srcNodeRef;

			if(this.domNode){
				var parent = this.getParent();
				if(parent){
					this.setAttachPoint(parent);
				}
			}
		},
		setAttachPoint: function(/*dojox.dtl.AttachPoint*/ attach){
			this._attach = attach;
		},
		render: function(/*dojox.dtl.HtmlTemplate*/ tpl, /*dojox.dtl.Context*/ context){
			if(!this._attach){
				throw new Error("You must use an attach point with dojox.dtl.TemplatedWidget");
			}

			context.setThis(this);
			this._attach.render(tpl, context);
		}
	}
);

dojo.declare("dojox.dtl.AttachPoint", [dijit._Widget, dijit._Container],
	{
		constructor: function(props, node){
			this._render = new dojox.dtl.render.html.Render(node);
		},
		render: function(/*dojox.dtl.HtmlTemplate*/ tpl, /*dojox.dtl.Context*/ context){
			this._render.render(tpl, context);
		}
	}
);

}
