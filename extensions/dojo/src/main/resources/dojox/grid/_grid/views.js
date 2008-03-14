if(!dojo._hasResource["dojox.grid._grid.views"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.grid._grid.views"] = true;
dojo.provide("dojox.grid._grid.views");

dojo.declare('dojox.grid.views', null, {
	// summary:
	//	A collection of grid views. Owned by grid and used internally for managing grid views.
	//	Grid creates views automatically based on grid's layout structure.
	//	Users should typically not need to access individual views or the views collection directly.
	constructor: function(inGrid){
		this.grid = inGrid;
	},
	defaultWidth: 200,
	views: [],
	// operations
	resize: function(){
		this.onEach("resize");
	},
	render: function(){
		this.onEach("render");
		this.normalizeHeaderNodeHeight();
	},
	// views
	addView: function(inView){
		inView.idx = this.views.length;
		this.views.push(inView);
	},
	destroyViews: function(){
		for (var i=0, v; v=this.views[i]; i++)
			v.destroy();
		this.views = [];
	},
	getContentNodes: function(){
		var nodes = [];
		for(var i=0, v; v=this.views[i]; i++){
			nodes.push(v.contentNode);
		}
		return nodes;
	},
	forEach: function(inCallback){
		for(var i=0, v; v=this.views[i]; i++){
			inCallback(v, i);
		}
	},
	onEach: function(inMethod, inArgs){
		inArgs = inArgs || [];
		for(var i=0, v; v=this.views[i]; i++){
			if(inMethod in v){
				v[inMethod].apply(v, inArgs);
			}
		}
	},
	// layout
	normalizeHeaderNodeHeight: function(){
		var rowNodes = [];
		for(var i=0, v; (v=this.views[i]); i++){
			if(v.headerContentNode.firstChild){
				rowNodes.push(v.headerContentNode)
			};
		}
		this.normalizeRowNodeHeights(rowNodes);
	},
	normalizeRowNodeHeights: function(inRowNodes){
		var h = 0; 
		for(var i=0, n, o; (n=inRowNodes[i]); i++){
			h = Math.max(h, (n.firstChild.clientHeight)||(n.firstChild.offsetHeight));
		}
		h = (h >= 0 ? h : 0);
		//
		var hpx = h + 'px';
		for(var i=0, n; (n=inRowNodes[i]); i++){
			if(n.firstChild.clientHeight!=h){
				n.firstChild.style.height = hpx;
			}
		}
		//
		//console.log('normalizeRowNodeHeights ', h);
		//
		// querying the height here seems to help scroller measure the page on IE
		if(inRowNodes&&inRowNodes[0]){
			inRowNodes[0].parentNode.offsetHeight;
		}
	},
	renormalizeRow: function(inRowIndex){
		var rowNodes = [];
		for(var i=0, v, n; (v=this.views[i])&&(n=v.getRowNode(inRowIndex)); i++){
			n.firstChild.style.height = '';
			rowNodes.push(n);
		}
		this.normalizeRowNodeHeights(rowNodes);
	},
	getViewWidth: function(inIndex){
		return this.views[inIndex].getWidth() || this.defaultWidth;
	},
	measureHeader: function(){
		this.forEach(function(inView){
			inView.headerContentNode.style.height = '';
		});
		var h = 0;
		this.forEach(function(inView){
			//console.log('headerContentNode', inView.headerContentNode.offsetHeight, inView.headerContentNode.offsetWidth);
			h = Math.max(inView.headerNode.offsetHeight, h);
		});
		return h;
	},
	measureContent: function(){
		var h = 0;
		this.forEach(function(inView) {
			h = Math.max(inView.domNode.offsetHeight, h);
		});
		return h;
	},
	findClient: function(inAutoWidth){
		// try to use user defined client
		var c = this.grid.elasticView || -1;
		// attempt to find implicit client
		if(c < 0){
			for(var i=1, v; (v=this.views[i]); i++){
				if(v.viewWidth){
					for(i=1; (v=this.views[i]); i++){
						if(!v.viewWidth){
							c = i;
							break;
						}
					}
					break;
				}
			}
		}
		// client is in the middle by default
		if(c < 0){
			c = Math.floor(this.views.length / 2);
		}
		return c;
	},
	_arrange: function(l, t, w, h){
		var i, v, vw, len = this.views.length;
		// find the client
		var c = (w <= 0 ? len : this.findClient());
		// layout views
		var setPosition = function(v, l, t){
			with(v.domNode.style){
				left = l + 'px';
				top = t + 'px';
			}
			with(v.headerNode.style){
				left = l + 'px';
				top = 0;
			}
		}
		// for views left of the client
		for(i=0; (v=this.views[i])&&(i<c); i++){
			// get width
			vw = this.getViewWidth(i);
			// process boxes
			v.setSize(vw, h);
			setPosition(v, l, t);
			vw = v.domNode.offsetWidth;
			// update position
			l += vw;
		}
		// next view (is the client, i++ == c) 
		i++;
		// start from the right edge
		var r = w;
		// for views right of the client (iterated from the right)
		for(var j=len-1; (v=this.views[j])&&(i<=j); j--){
			// get width
			vw = this.getViewWidth(j);
			// set size
			v.setSize(vw, h);
			// measure in pixels
			vw = v.domNode.offsetWidth;
			// update position
			r -= vw;
			// set position
			setPosition(v, r, t);
		}
		if(c<len){
			v = this.views[c];
			// position the client box between left and right boxes	
			vw = Math.max(1, r-l);
			// set size
			v.setSize(vw + 'px', h);
			setPosition(v, l, t);
		}
		return l;
	},
	arrange: function(l, t, w, h){
		var w = this._arrange(l, t, w, h);
		this.resize();
		return w;
	},
	// rendering
	renderRow: function(inRowIndex, inNodes){
		var rowNodes = [];
		for(var i=0, v, n, rowNode; (v=this.views[i])&&(n=inNodes[i]); i++){
			rowNode = v.renderRow(inRowIndex);
			n.appendChild(rowNode);
			rowNodes.push(rowNode);
		}
		this.normalizeRowNodeHeights(rowNodes);
	},
	rowRemoved: function(inRowIndex){
		this.onEach("rowRemoved", [ inRowIndex ]);
	},
	// updating
	updateRow: function(inRowIndex, inHeight){
		for(var i=0, v; v=this.views[i]; i++){
			v.updateRow(inRowIndex, inHeight);
		}
		this.renormalizeRow(inRowIndex);
	},
	updateRowStyles: function(inRowIndex){
		this.onEach("updateRowStyles", [ inRowIndex ]);
	},
	// scrolling
	setScrollTop: function(inTop){
		var top = inTop;
		for(var i=0, v; v=this.views[i]; i++){
			top = v.setScrollTop(inTop);
		}
		return top;
		//this.onEach("setScrollTop", [ inTop ]);
	},
	getFirstScrollingView: function(){
		for(var i=0, v; (v=this.views[i]); i++){
			if(v.hasScrollbar()){
				return v;
			}
		}
	}
});

}
