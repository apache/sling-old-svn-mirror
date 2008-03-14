if(!dojo._hasResource["dojox.dtl.tag.html"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.tag.html"] = true;
dojo.provide("dojox.dtl.tag.html");

dojo.require("dojox.dtl._base");

dojox.dtl.tag.html.HtmlNode = function(name){
	this.contents = new dojox.dtl.Filter(name);
	this._div = document.createElement("div");
	this._lasts = [];
}
dojo.extend(dojox.dtl.tag.html.HtmlNode, {
	render: function(context, buffer){
		var text = this.contents.resolve(context);
		text = text.replace(/<(\/?script)/ig, '&lt;$1').replace(/\bon[a-z]+\s*=/ig, '');
		if(this._rendered && this._last != text){
			buffer = this.unrender(context, buffer);
		}
		this._last = text;

		// This can get reset in the above tag
		if(!this._rendered){
			this._rendered = true;
			var div = this._div;
			div.innerHTML = text;
			var children = div.childNodes;
			while(children.length){
				var removed = div.removeChild(children[0]);
				this._lasts.push(removed);
				buffer = buffer.concat(removed);
			}
		}

		return buffer;
	},
	unrender: function(context, buffer){
		if(this._rendered){
			this._rendered = false;
			this._last = "";
			for(var i = 0, node; node = this._lasts[i++];){
				buffer = buffer.remove(node);
				dojo._destroyElement(node);
			}
			this._lasts = [];
		}
		return buffer;
	},
	clone: function(buffer){
		return new dojox.dtl.tag.html.HtmlNode(this.contents.contents);
	},
	toString: function(){ return "dojox.dtl.tag.html.HtmlNode"; }
});

dojox.dtl.tag.html.StyleNode = function(styles){
	this.contents = {};
	this._styles = styles;
	for(var key in styles){
		this.contents[key] = new dojox.dtl.Template(styles[key]);
	}
}
dojo.extend(dojox.dtl.tag.html.StyleNode, {
	render: function(context, buffer){
		for(var key in this.contents){
			dojo.style(buffer.getParent(), key, this.contents[key].render(context));
		}
		return buffer;
	},
	unrender: function(context, buffer){
		return buffer;
	},
	clone: function(buffer){
		return new dojox.dtl.tag.html.HtmlNode(this._styles);
	},
	toString: function(){ return "dojox.dtl.tag.html.StyleNode"; }
});

dojox.dtl.tag.html.AttachNode = function(key){
	this.contents = key;
}
dojo.extend(dojox.dtl.tag.html.AttachNode, {
	render: function(context, buffer){
		if(!this._rendered){
			this._rendered = true;
			context.getThis()[this.contents] = buffer.getParent();
		}
		return buffer;
	},
	unrender: function(context, buffer){
		if(this._rendered){
			this._rendered = false;
			if(context.getThis()[this.contents] === buffer.getParent()){
				delete context.getThis()[this.contents];
			}
		}
		return buffer;
	},
	clone: function(buffer){
		return new dojox.dtl.tag.html.HtmlNode(this._styles);
	},
	toString: function(){ return "dojox.dtl.tag.html.AttachNode"; }
});

dojox.dtl.tag.html.html = function(parser, text){
	var parts = text.split(" ", 2);
	return new dojox.dtl.tag.html.HtmlNode(parts[1]);
}

dojox.dtl.tag.html.tstyle = function(parser, text){
	var styles = {};
	text = text.replace(dojox.dtl.tag.html.tstyle._re, "");
	var rules = text.split(dojox.dtl.tag.html.tstyle._re1);
	for(var i = 0, rule; rule = rules[i]; i++){
		var parts = rule.split(dojox.dtl.tag.html.tstyle._re2);
		var key = parts[0];
		var value = parts[1];
		if(value.indexOf("{{") == 0){
			styles[key] = value;
		}
	}
	return new dojox.dtl.tag.html.StyleNode(styles);
}
dojo.mixin(dojox.dtl.tag.html.tstyle, {
	_re: /^tstyle\s+/,
	_re1: /\s*;\s*/g,
	_re2: /\s*:\s*/g
});

dojox.dtl.tag.html.attach = function(parser, text){
	var parts = text.split(dojox.dtl.tag.html.attach._re);
	return new dojox.dtl.tag.html.AttachNode(parts[1]);
}
dojo.mixin(dojox.dtl.tag.html.attach, {
	_re: /\s+/g
})

}
