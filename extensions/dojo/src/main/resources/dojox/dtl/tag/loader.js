if(!dojo._hasResource["dojox.dtl.tag.loader"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.tag.loader"] = true;
dojo.provide("dojox.dtl.tag.loader");

dojo.require("dojox.dtl._base");

dojox.dtl.tag.loader.BlockNode = function(name, nodelist){
	this.name = name;
	this.nodelist = nodelist; // Can be overridden
}
dojo.extend(dojox.dtl.tag.loader.BlockNode, {
	render: function(context, buffer){
		if(this.override){
			buffer = this.override.render(context, buffer, this);
			this.rendered = this.override;
		}else{
			buffer =  this.nodelist.render(context, buffer, this);
			this.rendered = this.nodelist;
		}
		this.override = null;
		return buffer;
	},
	unrender: function(context, buffer){
		return this.rendered.unrender(context, buffer);
	},
	setOverride: function(nodelist){
		// summary: In a shared parent, we override, not overwrite
		if(!this.override){
			this.override = nodelist;
		}
	},
	toString: function(){ return "dojox.dtl.tag.loader.BlockNode"; }
});
dojox.dtl.tag.loader.block = function(parser, text){
	var parts = text.split(" ");
	var name = parts[1];
	
	parser._blocks = parser._blocks || {};
	parser._blocks[name] = parser._blocks[name] || [];
	parser._blocks[name].push(name);
	
	var nodelist = parser.parse(["endblock", "endblock " + name]);
	parser.next();
	return new dojox.dtl.tag.loader.BlockNode(name, nodelist);
}

dojox.dtl.tag.loader.ExtendsNode = function(getTemplate, nodelist, shared, parent, key){
	this.getTemplate = getTemplate;
	this.nodelist = nodelist;
	this.shared = shared;
	this.parent = parent;
	this.key = key;
}
dojo.extend(dojox.dtl.tag.loader.ExtendsNode, {
	parents: {},
	getParent: function(context){
		if(!this.parent){
			this.parent = context.get(this.key, false);
			if(!this.parent){
				throw new Error("extends tag used a variable that did not resolve");
			}
			if(typeof this.parent == "object"){
				if(this.parent.url){
					if(this.parent.shared){
						this.shared = true;
					}
					this.parent = this.parent.url.toString();
				}else{
					this.parent = this.parent.toString();
				}
			}
			if(this.parent && this.parent.indexOf("shared:") == 0){
				this.shared = true;
				this.parent = this.parent.substring(7, parent.length);
			}
		}
		var parent = this.parent;
		if(!parent){
			throw new Error("Invalid template name in 'extends' tag.");
		}
		if(parent.render){
			return parent;
		}
		if(this.parents[parent]){
			return this.parents[parent];
		}
		this.parent = this.getTemplate(dojox.dtl.text.getTemplateString(parent));
		if(this.shared){
			this.parents[parent] = this.parent;
		}
		return this.parent;
	},
	render: function(context, buffer){
		var st = dojox.dtl;
		var stbl = dojox.dtl.tag.loader;
		var parent = this.getParent(context);
		var isChild = parent.nodelist[0] instanceof this.constructor;
		var parentBlocks = {};
		for(var i = 0, node; node = parent.nodelist.contents[i]; i++){
			if(node instanceof stbl.BlockNode){
				parentBlocks[node.name] = node;
			}
		}
		for(var i = 0, node; node = this.nodelist.contents[i]; i++){
			if(node instanceof stbl.BlockNode){
				var block = parentBlocks[node.name];
				if(!block){
					if(isChild){
						parent.nodelist[0].nodelist.append(node);
					}
				}else{
					if(this.shared){
						block.setOverride(node.nodelist);
					}else{
						block.nodelist = node.nodelist;
					}
				}
			}
		}
		this.rendered = parent;
		return parent.render(context, buffer, this);
	},
	unrender: function(context, buffer){
		return this.rendered.unrender(context, buffer, this);
	},
	toString: function(){ return "dojox.dtl.block.ExtendsNode"; }
});
dojox.dtl.tag.loader.extends_ = function(parser, text){
	var parts = text.split(" ");
	var shared = false;
	var parent = null;
	var key = null;
	if(parts[1].charAt(0) == '"' || parts[1].charAt(0) == "'"){
		parent = parts[1].substring(1, parts[1].length - 1);
	}else{
		key = parts[1];
	}
	if(parent && parent.indexOf("shared:") == 0){
		shared = true;
		parent = parent.substring(7, parent.length);
	}
	var nodelist = parser.parse();
	return new dojox.dtl.tag.loader.ExtendsNode(parser.getTemplate, nodelist, shared, parent, key);
}

}
