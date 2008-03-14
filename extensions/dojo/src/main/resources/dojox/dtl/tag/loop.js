if(!dojo._hasResource["dojox.dtl.tag.loop"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.dtl.tag.loop"] = true;
dojo.provide("dojox.dtl.tag.loop");

dojo.require("dojox.dtl._base");

dojox.dtl.tag.loop.CycleNode = function(cyclevars, name, VarNode){
	this._cyclevars = cyclevars;
	this._counter = -1
	this._name = name;
	this._map = {};
	this._VarNode = VarNode;
}
dojo.extend(dojox.dtl.tag.loop.CycleNode, {
	render: function(context, buffer){
		if(context.forloop && !context.forloop.counter0){
			this._counter = -1;
		}

		++this._counter;
		var value = this._cyclevars[this._counter % this._cyclevars.length];
		if(this._name){
			context[this._name] = value;
		}
		if(!this._map[value]){
			this._map[value] = {};
		}
		var node = this._map[value][this._counter] = new this._VarNode(value);

		return node.render(context, buffer, this);
	},
	unrender: function(context, buffer){
		return buffer;
	},
	clone: function(){
		return new this.constructor(this._cyclevars, this._name);
	},
	_onEnd: function(){
		this._counter = -1;
	},
	toString: function(){ return "dojox.dtl.tag.loop.CycleNode"; }
});

dojox.dtl.tag.loop.cycle = function(parser, text){
	// summary: Cycle among the given strings each time this tag is encountered
	var args = text.split(" ");

	if(args.length < 2){
		throw new Error("'cycle' tag requires at least two arguments");
	}

	if(args[1].indexOf(",") != -1){
		var vars = args[1].split(",");
		args = [args[0]];
		for(var i = 0; i < vars.length; i++){
			args.push('"' + vars[i] + '"');
		}
	}

	if(args.length == 2){
		var name = args[args.length - 1];

		if(!parser._namedCycleNodes){
			throw new Error("No named cycles in template: '" + name + "' is not defined");
		}
		if(!parser._namedCycleNodes[name]){
			throw new Error("Named cycle '" + name + "' does not exist");
		}

        return parser._namedCycleNodes[name];
	}

	if(args.length > 4 && args[args.length - 2] == "as"){
		var name = args[args.length - 1];

		var node = new dojox.dtl.tag.loop.CycleNode(args.slice(1, args.length - 2), name, parser.getVarNode());

		if(!parser._namedCycleNodes){
			parser._namedCycleNodes = {};
		}
		parser._namedCycleNodes[name] = node;
	}else{
		node = new dojox.dtl.tag.loop.CycleNode(args.slice(1), null, parser.getVarNode());
	}

	return node;
}

}
