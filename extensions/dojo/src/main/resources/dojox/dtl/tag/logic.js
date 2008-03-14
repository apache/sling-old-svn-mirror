if(!dojo._hasResource["dojox.dtl.tag.logic"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.tag.logic"] = true;
dojo.provide("dojox.dtl.tag.logic");

dojo.require("dojox.dtl._base");

dojox.dtl.tag.logic.IfNode = function(bools, trues, falses, type){
	this.bools = bools;
	this.trues = trues;
	this.falses = falses;
	this.type = type;
}
dojo.extend(dojox.dtl.tag.logic.IfNode, {
	render: function(context, buffer){
		if(this.type == "or"){
			for(var i = 0, bool; bool = this.bools[i]; i++){
				var ifnot = bool[0];
				var filter = bool[1];
				var value = filter.resolve(context);
				if((value && !ifnot) || (ifnot && !value)){
					if(this.falses){
						buffer = this.falses.unrender(context, buffer);
					}
					return this.trues.render(context, buffer, this);
				}
				buffer = this.trues.unrender(context, buffer);
				if(this.falses)	return this.falses.render(context, buffer, this);
			}
		}else{
			for(var i = 0, bool; bool = this.bools[i]; i++){
				var ifnot = bool[0];
				var filter = bool[1];
				var value = filter.resolve(context);
				if(!((value && !ifnot) || (ifnot && !value))){
					if(this.trues){
						buffer = this.trues.unrender(context, buffer);
					}
					return this.falses.render(context, buffer, this);
				}
				buffer = this.falses.unrender(context, buffer);
				if(this.falses)	return this.trues.render(context, buffer, this);
			}
		}
		return buffer;
	},
	unrender: function(context, buffer){
		if(this.trues) buffer = this.trues.unrender(context, buffer);
		if(this.falses) buffer = this.falses.unrender(context, buffer);
		return buffer;
	},
	clone: function(buffer){
		var trues = this.trues;
		var falses = this.falses;
		if(trues){
			trues = trues.clone(buffer);
		}
		if(falses){
			falses = falses.clone(buffer);
		}
		return new this.constructor(this.bools, trues, falses, this.type);
	},
	toString: function(){ return "dojox.dtl.tag.logic.IfNode"; }
});

dojox.dtl.tag.logic.ForNode = function(assign, loop, reversed, nodelist){
	this.assign = assign;
	this.loop = loop;
	this.reversed = reversed;
	this.nodelist = nodelist;
	this.pool = [];
}
dojo.extend(dojox.dtl.tag.logic.ForNode, {
	render: function(context, buffer){
		var parentloop = {};
		if(context.forloop){
			parentloop = context.forloop;
		}
		var items = dojox.dtl.resolveVariable(this.loop, context);
		context.push();
		for(var i = items.length; i < this.pool.length; i++){
			this.pool[i].unrender(context, buffer);
		}
		if(this.reversed){
			items = items.reversed();
		}
		var j = 0;
		for(var i in items){
			var item = items[i];
			context.forloop = {
				key: i,
				counter0: j,
				counter: j + 1,
				revcounter0: items.length - j - 1,
				revcounter: items.length - j,
				first: j == 0,
				parentloop: parentloop
			};
			context[this.assign] = item;
			if(j + 1 > this.pool.length){
				this.pool.push(this.nodelist.clone(buffer));
		 	}
			buffer = this.pool[j].render(context, buffer, this);
			++j;
		}
		context.pop();
		return buffer;
	},
	unrender: function(context, buffer){
		for(var i = 0, pool; pool = this.pool[i]; i++){
			buffer = pool.unrender(context, buffer);
		}
		return buffer;
	},
	clone: function(buffer){
		return new this.constructor(this.assign, this.loop, this.reversed, this.nodelist.clone(buffer));
	},
	toString: function(){ return "dojox.dtl.tag.logic.ForNode"; }
});

dojox.dtl.tag.logic.if_ = function(parser, text){
	var parts = text.split(/\s+/g);
	var type;
	var bools = [];
	parts.shift();
	text = parts.join(" ");
	parts = text.split(" and ");
	if(parts.length == 1){
		type = "or";
		parts = text.split(" or ");
	}else{
		type = "and";
		for(var i = 0; i < parts.length; i++){
			if(parts[i] == "or"){
				throw new Error("'if' tags can't mix 'and' and 'or'");
			}
		}
	}
	for(var i = 0, part; part = parts[i]; i++){
		var not = false;
		if(part.indexOf("not ") == 0){
			part = part.substring(4);
			not = true;
		}
		bools.push([not, new dojox.dtl.Filter(part)]);
	}
	var trues = parser.parse(["else", "endif"]);
	var falses = false;
	var token = parser.next();
	if(token.text == "else"){
		var falses = parser.parse(["endif"]);
		parser.next();
	}
	return new dojox.dtl.tag.logic.IfNode(bools, trues, falses, type);
}

dojox.dtl.tag.logic.for_ = function(parser, text){
	var parts = text.split(/\s+/g);
	var reversed = parts.length == 5;
	var nodelist = parser.parse(["endfor"]);
	parser.next();
	return new dojox.dtl.tag.logic.ForNode(parts[1], parts[3], reversed, nodelist);
}

}
