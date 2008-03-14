if(!dojo._hasResource["dojox.fx._base"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.fx._base"] = true;
dojo.provide("dojox.fx._base");
// summary: add-on Animations to dojo.fx

dojo.require("dojo.fx"); 

// convenience functions: 
dojox.fx.chain = dojo.fx.chain;
dojox.fx.combine = dojo.fx.combine;
dojox.fx.wipeIn = dojo.fx.wipeIn;
dojox.fx.wipeOut = dojo.fx.wipeOut;
dojox.fx.slideTo = dojo.fx.slideTo;

dojox.fx.sizeTo = function(/* Object */args){
	// summary: Create an animation that will size a node
	// description:
	//	Returns an animation that will size "node" 
	//	defined in args Object about it's center to
	//	a width and height defined by (args.width, args.height), 
	//	supporting an optional method: chain||combine mixin
	//	(defaults to chain).	
	//
	//	- works best on absolutely or relatively positioned elements? 
	//	
	// example:
	// |	// size #myNode to 400px x 200px over 1 second
	// |	dojo.fx.sizeTo({ node:'myNode',
	// |		duration: 1000,
	// |		width: 400,
	// |		height: 200,
	// |		method: "chain"
	// |	}).play();
	//
	var node = (args.node = dojo.byId(args.node));
	var compute = dojo.getComputedStyle;

	var method = args.method || "chain"; 
	if (method=="chain"){ args.duration = Math.floor(args.duration/2); } 
	
	var top, newTop, left, newLeft, width, height = null;

	var init = (function(){
		var innerNode = node;
		return function(){
			var pos = compute(innerNode).position;
			top = (pos == 'absolute' ? node.offsetTop : parseInt(compute(node).top) || 0);
			left = (pos == 'absolute' ? node.offsetLeft : parseInt(compute(node).left) || 0);
			width = parseInt(dojo.style(node,'width'));
			height = parseInt(dojo.style(node,'height'));

			newLeft = left - Math.floor((args.width - width)/2); 
			newTop = top - Math.floor((args.height - height)/2); 

			if(pos != 'absolute' && pos != 'relative'){
				var ret = dojo.coords(innerNode, true);
				top = ret.y;
				left = ret.x;
				innerNode.style.position="absolute";
				innerNode.style.top=top+"px";
				innerNode.style.left=left+"px";
			}
		}
	})();
	init(); // hmmm, do we need to init() or just the once beforeBegin?

	var anim1 = dojo.animateProperty(dojo.mixin({
		properties: {
			height: { start: height, end: args.height || 0, unit:"px" },
			top: { start: top, end: newTop }
		}
	}, args));
	var anim2 = dojo.animateProperty(dojo.mixin({
		properties: {
			width: { start: width, end: args.width || 0, unit:"px" },
			left: { start: left, end: newLeft }
		}
	}, args));

	var anim = dojo.fx[((args.method == "combine") ? "combine" : "chain")]([anim1,anim2]);
	dojo.connect(anim, "beforeBegin", anim, init);
	return anim; // dojo._Animation
};

dojox.fx.slideBy = function(/* Object */args){
	// summary: Returns an animation to slide a node by a defined offset.
	//
	// description:
	//	Returns an animation that will slide a node (args.node) from it's
	//	current position to it's current posision plus the numbers defined
	//	in args.top and args.left. standard dojo.fx mixin's apply. 
	//	
	// example:
	// |	// slide domNode 50px down, and 22px left
	// |	dojox.fx.slideBy({ 
	// |		node: domNode, duration:400, 
	// |		top: 50, left: -22 
	// |	}).play();

	var node = (args.node = dojo.byId(args.node));	
	var compute = dojo.getComputedStyle;
	var top = null; var left = null;
	var init = (function(){
		var innerNode = node;
		return function(){
			var pos = compute(innerNode,'position');
			top = (pos == 'absolute' ? node.offsetTop : parseInt(compute(node, 'top')) || 0);
			left = (pos == 'absolute' ? node.offsetLeft : parseInt(compute(node, 'left')) || 0);
			if(pos != 'absolute' && pos != 'relative'){
				var ret = dojo.coords(innerNode, true);
				top = ret.y;
				left = ret.x;
				innerNode.style.position="absolute";
				innerNode.style.top=top+"px";
				innerNode.style.left=left+"px";
			}
		}
	})();
	init();
	var _anim = dojo.animateProperty(dojo.mixin({
		properties: {
			// FIXME: is there a way to update the _Line after creation?
			// null start values allow chaining to work, animateProperty will
			// determine them for us (except in ie6? -- ugh)
			top: {  /* start: top, */end: top+(args.top||0) },
			left: { /* start: left, */end: left+(args.left||0) }
		}
	}, args));
	dojo.connect(_anim,"beforeBegin",_anim,init);
	return _anim; // dojo._Animation
};

dojox.fx.crossFade = function(/* Object */args){
	// summary: Returns an animation cross fading two element simultaneously
	// 
	// args:
	//	args.nodes: Array - two element array of domNodes, or id's
	//
	// all other standard animation args mixins apply. args.node ignored.
	//
	if(dojo.isArray(args.nodes)){
		// simple check for which node is visible, maybe too simple?
		var node1 = args.nodes[0] = dojo.byId(args.nodes[0]);
		var op1 = dojo.style(node1,"opacity");
		var node2 = args.nodes[1] = dojo.byId(args.nodes[1]);
		var op2 = dojo.style(node2, "opacity");

		var _anim = dojo.fx.combine([
			dojo[((op1==0)?"fadeIn":"fadeOut")](dojo.mixin({
				node: node1
			},args)),
			dojo[((op1==0)?"fadeOut":"fadeIn")](dojo.mixin({
				node: node2
			},args))
		]);
		return _anim; // dojo._Animation
	}else{
		// improper syntax in args, needs Array
		return false; // Boolean
	}
};

dojox.fx.highlight = function(/*Object*/ args){
	// summary: Highlight a node
	// description:
	//	Returns an animation that sets the node background to args.color
	//	then gradually fades back the original node background color
	//	
	// example:
	//	dojox.fx.highlight({ node:"foo" }).play(); 

	var node = (args.node = dojo.byId(args.node));

	args.duration = args.duration || 400;
	// Assign default color light yellow
	var startColor = args.color || '#ffff99';
	var endColor = dojo.style(node, "backgroundColor");
	var wasTransparent = (endColor == "transparent" || endColor == "rgba(0, 0, 0, 0)");

	var anim = dojo.animateProperty(dojo.mixin({
		properties: {
			backgroundColor: { start: startColor, end: endColor }
		}
	}, args));

	dojo.connect(anim, "onEnd", anim, function(){
		if(wasTransparent){
			node.style.backgroundColor = "transparent";
		}
	});

	return anim; // dojo._Animation
};

}
