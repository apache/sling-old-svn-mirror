if(!dojo._hasResource["dojox.layout.FloatingPane"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.layout.FloatingPane"] = true;
dojo.provide("dojox.layout.FloatingPane");
dojo.experimental("dojox.layout.FloatingPane"); 

dojo.require("dojox.layout.ContentPane");
dojo.require("dijit._Templated"); 
dojo.require("dijit._Widget"); 
dojo.require("dojo.dnd.move");
dojo.require("dojox.layout.ResizeHandle"); 

dojo.declare("dojox.layout.FloatingPane", 
	[ dojox.layout.ContentPane, dijit._Templated ],
{
	// summary:
	//
	// Makes a dijit.ContentPane float and draggable by it's title [similar to TitlePane]
	// and over-rides onClick to onDblClick for wipeIn/Out of containerNode
	// provides minimize(dock) / show() and hide() methods, and resize [almost] 

	// closable: Boolean
	//	allow closure of this Node
	closable: true,

	// dockable: Boolean
	//	allow minimizing of pane true/false
	dockable: true,

	// resizable: Boolean
	//	allow resizing of pane true/false
	resizable: false,

	// maxable: Boolean
	//	horrible param name for "Can you maximize this floating pane"
	maxable: false,

	// resizeAxis: String
	//	x | xy | y to limit pane's sizing direction
	resizeAxis: "xy",

	// title: String
	//	title to put in titlebar
	title: "",

	// dockTo: DomNode || null
	//	if null, will create private layout.Dock that scrolls with viewport
	//	on bottom span of viewport.	
	dockTo: null,

	// duration: Integer
	//	time is MS to spend toggling in/out node
	duration: 400,

	// animation holders for toggle
	_showAnim: null,
	_hideAnim: null, 
	
	// node in the dock (if docked)
	_dockNode: null,

	// iconSrc: String
	//	[not implemented yet] will be either icon in titlepane to left
	//	of Title, and/or icon show when docked in a fisheye-like dock
	//	or maybe dockIcon would be better?
	iconSrc: null,

	contentClass: "dojoxFloatingPaneContent",
	templateString: null,
	templateString:"<div class=\"dojoxFloatingPane\" id=\"${id}\">\n\t<div tabindex=\"0\" waiRole=\"button\" class=\"dojoxFloatingPaneTitle\" dojoAttachPoint=\"focusNode\">\n\t\t<span dojoAttachPoint=\"closeNode\" dojoAttachEvent=\"onclick: close\" class=\"dojoxFloatingCloseIcon\"></span>\n\t\t<span dojoAttachPoint=\"maxNode\" dojoAttachEvent=\"onclick: maximize\" class=\"dojoxFloatingMaximizeIcon\"></span>\n\t\t<span dojoAttachPoint=\"restoreNode\" dojoAttachEvent=\"onclick: _restore\" class=\"dojoxFloatingRestoreIcon\"></span>\t\n\t\t<span dojoAttachPoint=\"dockNode\" dojoAttachEvent=\"onclick: minimize\" class=\"dojoxFloatingMinimizeIcon\"></span>\n\t\t<span dojoAttachPoint=\"titleNode\" class=\"dijitInline dijitTitleNode\"></span>\n\t</div>\n\t<div dojoAttachPoint=\"canvas\" class=\"dojoxFloatingPaneCanvas\">\n\t\t<div dojoAttachPoint=\"containerNode\" waiRole=\"region\" tabindex=\"-1\" class=\"${contentClass}\">\n\t\t</div>\n\t\t<span dojoAttachPoint=\"resizeHandle\" class=\"dojoxFloatingResizeHandle\"></span>\n\t</div>\n</div>\n",

	_restoreState: {},
	_allFPs: [],

	postCreate: function(){
		// summary: 
		this.setTitle(this.title);
		this.inherited("postCreate",arguments);
		var move = new dojo.dnd.Moveable(this.domNode,{ handle: this.focusNode });
		//this._listener = dojo.subscribe("/dnd/move/start",this,"bringToTop"); 

		if(!this.dockable){ this.dockNode.style.display = "none"; } 
		if(!this.closable){ this.closeNode.style.display = "none"; } 
		if(!this.maxable){
			this.maxNode.style.display = "none";
			this.restoreNode.style.display = "none";
		}
		if(!this.resizable){
			this.resizeHandle.style.display = "none"; 	
		}else{
			var foo = dojo.marginBox(this.domNode); 
			this.domNode.style.width = foo.w+"px"; 
		}		
		this._allFPs.push(this);
	},
	
	startup: function(){
		this.inherited("startup",arguments);

		if(this.resizable){
			if(dojo.isIE){
				this.canvas.style.overflow = "auto";
			} else {
				this.containerNode.style.overflow = "auto";
			}
			var tmp = new dojox.layout.ResizeHandle({ 
				//targetContainer: this.containerNode, 
				targetId: this.id, 
				resizeAxis: this.resizeAxis 
			},this.resizeHandle);
		}

		if(this.dockable){ 
			// FIXME: argh.
			tmpName = this.dockTo; 

			if(this.dockTo){
				this.dockTo = dijit.byId(this.dockTo); 
			}else{
				this.dockTo = dijit.byId('dojoxGlobalFloatingDock');
			}

			if(!this.dockTo){
				// we need to make our dock node, and position it against
				// .dojoxDockDefault .. this is a lot. either dockto="node"
				// and fail if node doesn't exist or make the global one
				// once, and use it on empty OR invalid dockTo="" node?
				if(tmpName){ 
					var tmpId = tmpName;
					var tmpNode = dojo.byId(tmpName); 
				}else{
					var tmpNode = document.createElement('div');
					dojo.body().appendChild(tmpNode);
					dojo.addClass(tmpNode,"dojoxFloatingDockDefault");
					var tmpId = 'dojoxGlobalFloatingDock';
				}
				this.dockTo = new dojox.layout.Dock({ id: tmpId, autoPosition: "south" },tmpNode);
				this.dockTo.startup(); 
			}
			
			if((this.domNode.style.display == "none")||(this.domNode.style.visibility == "hidden")){
				// If the FP is created dockable and non-visible, start up docked.
				this.minimize();
			} 
		} 		
		this.connect(this.focusNode,"onmousedown","bringToTop");
		this.connect(this.domNode,	"onmousedown","bringToTop");
	},

	setTitle: function(/* String */ title){
		// summary: Update the string in the titleNode
		this.titleNode.innerHTML = title; 
	},	
		
	close: function(){
		// summary: close and destroy this widget
		if(!this.closable){ return; }
		dojo.unsubscribe(this._listener); 
		this.hide(dojo.hitch(this,"destroy")); 
	},

	hide: function(/* Function? */ callback){
		// summary: close but do not destroy this widget
		dojo.fadeOut({
			node:this.domNode,
			duration:this.duration,
			onEnd: dojo.hitch(this,function() { 
				this.domNode.style.display = "none";
				this.domNode.style.visibility = "hidden"; 
				if(this.dockTo){
					this.dockTo._positionDock(null);
				}
				if(callback){
					callback();
				}
			})
		}).play();
	},

	show: function(/* Function? */callback){
		// summary: show the FloatingPane
		var anim = dojo.fadeIn({node:this.domNode, duration:this.duration,
			beforeBegin: dojo.hitch(this,function(){
				this.domNode.style.display = ""; 
				this.domNode.style.visibility = "visible";
				this.dockTo._positionDock(null);
				if (this.dockTo) { this.dockTo._positionDock(null); }
				if (typeof callback == "function") { callback(); }
				this._isDocked = false;
				if (this._dockNode) { 
					this._dockNode.destroy();
					this._dockNode = null;
				}
			})
		}).play();
		this.resize(dojo.coords(this.domNode));
	},

	minimize: function(){
		// summary: hide and dock the FloatingPane
		if(!this._isDocked){
			this.hide(dojo.hitch(this,"_dock"));
		} 
	},

	maximize: function(){
		// summary: Make this floatingpane fullscreen (viewport)	
		if(this._maximized){ return; }
		this._naturalState = dojo.coords(this.domNode);
		if(this._isDocked){
			this.show();
			setTimeout(dojo.hitch(this,"maximize"),this.duration);
		}
		dojo.addClass(this.focusNode,"floatingPaneMaximized");
		this.resize(dijit.getViewport());
		this._maximized = true;
	},

	_restore: function(){
		if(this._maximized){
			this.resize(this._naturalState);
			dojo.removeClass(this.focusNode,"floatingPaneMaximized");
			this._maximized = false;
		}		
	},

	_dock: function(){
		if(!this._isDocked){
			this._dockNode = this.dockTo.addNode(this);
			this._isDocked = true;
		}
	},
	
	resize: function(/* Object */dim){
		// summary: size the widget and place accordingly
		this._currentState = dim;
		var dns = this.domNode.style;

		dns.top = dim.t+"px";
		dns.left = dim.l+"px";

		dns.width = dim.w+"px"; 
		this.canvas.style.width = dim.w+"px";

		dns.height = dim.h+"px";
		this.canvas.style.height = (dim.h - this.focusNode.offsetHeight)+"px";
	},
	
	_startZ: 100,
	
	bringToTop: function(){
		// summary: bring this FloatingPane above all other panes
		var windows = dojo.filter(
			this._allFPs,
			function(i){
				return i !== this;
			}, 
		this);
		windows.sort(function(a, b){
			return a.domNode.style.zIndex - b.domNode.style.zIndex;
		});
		windows.push(this);
		dojo.forEach(windows, function(w, x){
			w.domNode.style.zIndex = (this._startZ + x * 2);
			dojo.removeClass(w.domNode, "dojoxFloatingPaneFg");
		}, this);
		dojo.addClass(this.domNode, "dojoxFloatingPaneFg");
	},
	
	destroy: function(){
		// summary: Destroy this FloatingPane completely
		this._allFPs.splice(dojo.indexOf(this._allFPs, this), 1);
		this.inherited("destroy", arguments);
	}
});


dojo.declare("dojox.layout.Dock", [dijit._Widget,dijit._Templated], {
	// summary:
	//	a widget that attaches to a node and keeps track of incoming / outgoing FloatingPanes
	// 	and handles layout

	templateString: '<div class="dojoxDock"><ul dojoAttachPoint="containerNode" class="dojoxDockList"></ul></div>',

	// private _docked: array of panes currently in our dock
	_docked: [],
	
	_inPositioning: false,
	
	autoPosition: false,
	
	addNode: function(refNode){
		// summary: instert a dockNode refernce into the dock
		var div = document.createElement('li');
		this.containerNode.appendChild(div);
		var node = new dojox.layout._DockNode({ title: refNode.title, paneRef: refNode },div);
		node.startup();
		return node;
	},

	startup: function(){
		// summary: attaches some event listeners 
		if (this.id == "dojoxGlobalFloatingDock" || this.isFixedDock) {
			// attach window.onScroll, and a position like in presentation/dialog
			dojo.connect(window,'onresize',this,"_positionDock");
			dojo.connect(window,'onscroll',this,"_positionDock");
			if(dojo.isIE){
				dojo.connect(this.domNode,'onresize', this,"_positionDock");
			}
		}
		this._positionDock(null);
		this.inherited("startup",arguments);
	},
	
	_positionDock: function(/* Event? */e){
		if(!this._inPositioning){	
			if(this.autoPosition == "south"){
				// Give some time for scrollbars to appear/disappear
				setTimeout(dojo.hitch(this, function() {
					this._inPositiononing = true;
					var viewport = dijit.getViewport();
					var s = this.domNode.style;
					s.left = viewport.l + "px";
					s.width = (viewport.w-2) + "px";
					s.top = (viewport.h + viewport.t) - this.domNode.offsetHeight + "px";
					this._inPositioning = false;
				}), 500);
			}
		}
	}


});

dojo.declare("dojox.layout._DockNode", [dijit._Widget,dijit._Templated], {
	// summary:
	//	dojox.layout._DockNode is a private widget used to keep track of
	//	which pane is docked.

	// title: String
	// 	shown in dock icon. should read parent iconSrc?	
	title: "",

	// paneRef: Widget
	//	reference to the FloatingPane we reprasent in any given dock
	paneRef: null,

	templateString: '<li dojoAttachEvent="onclick: restore" class="dojoxDockNode">'+
			'<span dojoAttachPoint="restoreNode" class="dojoxDockRestoreButton" dojoAttachEvent="onclick: restore"></span>'+
			'<span class="dojoxDockTitleNode" dojoAttachPoint="titleNode">${title}</span>'+
			'</li>',

	restore: function(){
		// summary: remove this dock item from parent dock, and call show() on reffed floatingpane
		this.paneRef.show();
		this.paneRef.bringToTop();
		this.destroy();
	}

});

}
