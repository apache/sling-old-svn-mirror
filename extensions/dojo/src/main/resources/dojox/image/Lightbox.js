if(!dojo._hasResource["dojox.image.Lightbox"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.image.Lightbox"] = true;
dojo.provide("dojox.image.Lightbox");
dojo.experimental("dojox.image.Lightbox");

dojo.require("dijit.Dialog"); 
dojo.require("dojox.fx");

dojo.declare("dojox.image.Lightbox",
	dijit._Widget,{
	// summary:
	//	a dojo-based Lightbox implementation. 
	//
	// description:
	//	an Elegant, keyboard accessible, markup and store capable Lightbox widget to show images
	//	in a modal dialog-esque format. Can show individual images as Modal dialog, or can group
	//	images with multiple entry points, all using a single "master" Dialog for visualization
	//
	// examples:
	//	
	//	<a href="image1.jpg" dojoType="dojox.image.Lightbox">show lightbox</a>
	//	<a href="image2.jpg" dojoType="dojox.image.Lightbox" group="one">show group lightbox</a>
	//	<a href="image3.jpg" dojoType="dojox.image.Lightbox" group="one">show group lightbox</a>
	//
	//	FIXME: not implemented fully yet, though works with basic datastore access. need to manually call
	//	widget._attachedDialog.addImage(item,"fromStore") for each item in a store result set.
	//	<div dojoType="dojox.image.Lightbox" group="fromStore" store="storeName"></div>

	// group: String
	//	grouping images in a page with similar tags will provide a 'slideshow' like grouping of images
	group: "",

	// title: String 
	//	A string of text to be shown in the Lightbox beneath the image (empty if using a store)
	title: "",

	// href; String
	//	link to image to use for this Lightbox node (empty if using a store).
	href: "",

	// duration: Integer
	//	generic time in MS to adjust the feel of widget. could possibly add various 
	//	durations for the various actions (dialog fadein, sizeing, img fadein ...) 
	duration: 500,

	// _allowPassthru: Boolean
	//	privately set this to disable/enable natural link of anchor tags
	_allowPassthru: false,
	_attachedDialog: null, // try to share a single underlay per page?

	startup: function(){
		this.inherited("startup", arguments);
		// setup an attachment to the masterDialog (or create the masterDialog)
		var tmp = dijit.byId('dojoxLightboxDialog');
		if(tmp){
			this._attachedDialog = tmp;
		}else{
			// this is the first instance to start, so we make the masterDialog
			this._attachedDialog = new dojox.image._LightboxDialog({ id: "dojoxLightboxDialog" });
			this._attachedDialog.startup();
		}
		if(!this.store){
			// FIXME: full store support lacking, have to manually call this._attachedDialog.addImage(imgage,group) as it stands
			this._addSelf();
			this.connect(this.domNode, "onclick", "_handleClick");
		}
	},

	_addSelf: function(){
		this._attachedDialog.addImage({
			href: this.href,
			title: this.title
		},this.group||null);
	},

	_handleClick: function(/* Event */e){
		// summary: handle the click on the link 
		if(!this._allowPassthru){ e.preventDefault(); }
		else{ return; }
		this.show();
	},

	show: function(){
		this._attachedDialog.show(this);
	},

	disable: function(){
		// summary, disables event clobbering and dialog, and follows natural link
		this._allowPassthru = true;
	},

	enable: function(){
		// summary: enables the dialog (prevents default link)
		this._allowPassthru = false; 
	}

});

dojo.declare("dojox.image._LightboxDialog",
	dijit.Dialog,{
	//
	// Description:
	//	
	//	a widget that intercepts anchor links (typically around images) 	
	//	and displays a modal Dialog. this is the actual Popup, and should 
	//	not be created directly. 
	//
	//	there will only be one of these on a page, so all dojox.image.Lightbox's will us it
	//	(the first instance of a Lightbox to be show()'n will create me If i do not exist)
	// 
	//	note: the could be the ImagePane i was talking about?

	// title: String
	// 	the current title 
	title: "",

	// FIXME: implement titleTemplate

	// inGroup: Array
	//	Array of objects. this is populated by from the JSON object _groups, and
	//	should not be populate manually. it is a placeholder for the currently 
	//	showing group of images in this master dialog
	inGroup: null,

	// imgUrl: String
	//	the src="" attrib of our imageNode (can be null at statup)
	imgUrl: "",

	// an array of objects, each object being a unique 'group'
	_groups: { XnoGroupX: [] },
	_imageReady: false,

	templateString:"<div class=\"dojoxLightbox\" dojoAttachPoint=\"containerNode\">\n\t<div style=\"position:relative\">\n\t\t<div dojoAttachPoint=\"imageContainer\" class=\"dojoxLightboxContainer\">\n\t\t\t<img dojoAttachPoint=\"imgNode\" src=\"${imgUrl}\" class=\"dojoxLightboxImage\" alt=\"${title}\">\n\t\t\t<div class=\"dojoxLightboxFooter\" dojoAttachPoint=\"titleNode\">\n\t\t\t\t<div class=\"dijitInline LightboxClose\" dojoAttachPoint=\"closeNode\"></div>\n\t\t\t\t<div class=\"dijitInline LightboxNext\" dojoAttachPoint=\"nextNode\"></div>\t\n\t\t\t\t<div class=\"dijitInline LightboxPrev\" dojoAttachPoint=\"prevNode\"></div>\n\n\t\t\t\t<div class=\"dojoxLightboxText\"><span dojoAttachPoint=\"textNode\">${title}</span><span dojoAttachPoint=\"groupCount\" class=\"dojoxLightboxGroupText\"></span></div>\n\t\t\t</div>\n\t\t</div>\t\n\t\t\n\t</div>\n</div>\n",

	startup: function(){
		// summary: add some extra event handlers, and startup our superclass.
		this.inherited("startup", arguments);

		// FIXME: these are supposed to be available in dijit.Dialog already,
		// but aren't making it over.
		dojo.connect(document.documentElement,"onkeypress",this,"_handleKey");
		this.connect(window,"onresize","_position"); 

		this.connect(this.nextNode, "onclick", "_nextImage");
		this.connect(this.prevNode, "onclick", "_prevImage");
		this.connect(this.closeNode, "onclick", "hide");
		
	},

	show: function(/* Object */groupData){
		// summary: starts the chain of events to show an image in the dialog, including showing the dialog 
		//	if it is not already visible

		dojo.style(this.imgNode,"opacity","0"); 
		dojo.style(this.titleNode,"opacity","0");

		// we only need to call dijit.Dialog.show() if we're not already open.
		if(!this.open){ this.inherited("show", arguments); }
	
		this._imageReady = false; 
		
		this.imgNode.src = groupData.href;
		if((groupData.group && !(groupData == "XnoGroupX")) || this.inGroup){ 
			if(!this.inGroup){ 
				this.inGroup = this._groups[(groupData.group)];
				var i = 0;
				// determine where we were or are in the show 
				dojo.forEach(this.inGroup,function(g){
					if (g.href == groupData.href){
						this._positionIndex = i;
					}
					i++; 
				},this);
			}
			if(!this._positionIndex){ this._positionIndex=0; this.imgNode.src = this.inGroup[this._positionIndex].href; }
			this.groupCount.innerHTML = " (" +(this._positionIndex+1) +" of "+this.inGroup.length+")";
			this.prevNode.style.visibility = "visible";
			this.nextNode.style.visibility = "visible";
		}else{
			this.groupCount.innerHTML = "";
			this.prevNode.style.visibility = "hidden";
			this.nextNode.style.visibility = "hidden";
		}
		this.textNode.innerHTML = groupData.title;
	
		if(!this._imageReady || this.imgNode.complete === true){ 
			this._imgConnect = dojo.connect(this.imgNode,"onload", this, function(){
				this._imageReady = true;
				this.resizeTo({ w: this.imgNode.width, h:this.imgNode.height, duration:this.duration });
				dojo.disconnect(this._imgConnect);
			});
			// onload doesn't fire in IE if you connect before you set the src. 
			// hack to re-set the src after onload connection made:
			if(dojo.isIE){ this.imgNode.src = this.imgNode.src; }
		}else{
			// do it quickly. kind of a hack, but image is ready now
			this.resizeTo({ w: this.imgNode.width, h:this.imgNode.height, duration:1 });
		}
	},

	_nextImage: function(){
		// summary: load next image in group
		if(this._positionIndex+1<this.inGroup.length){
			this._positionIndex++;
		}else{
			this._positionIndex = 0;
		}
		this._loadImage();
	},

	_prevImage: function(){
		// summary: load previous image in group
		if(this._positionIndex==0){
			this._positionIndex = this.inGroup.length-1;
		}else{
			this._positionIndex--;
		}
		this._loadImage();
	},

	_loadImage: function(){
		// summary: do the prep work before we can show another image 
		var _loading = dojo.fx.combine([
			dojo.fadeOut({ node:this.imgNode, duration:(this.duration/2) }),
			dojo.fadeOut({ node:this.titleNode, duration:(this.duration/2) })
		]);
		this.connect(_loading,"onEnd","_prepNodes");
		_loading.play(10);
	},

	_prepNodes: function(){
		// summary: a localized hook to accompany _loadImage
		this._imageReady = false; 
		this.show({
			href: this.inGroup[this._positionIndex].href,
			title: this.inGroup[this._positionIndex].title
		});
	},

	resizeTo: function(/* Object */size){
		// summary: resize our dialog container, and fire _showImage
		var _sizeAnim = dojox.fx.sizeTo({ 
			node: this.containerNode,
			duration:size.duration||this.duration,
			width: size.w, 
			height:size.h+30
		});
		this.connect(_sizeAnim,"onEnd","_showImage");
		_sizeAnim.play(this.duration);
	},

	_showImage: function(){
		// summary: fade in the image, and fire showNav
		dojo.fadeIn({ node: this.imgNode, duration:this.duration,
			onEnd: dojo.hitch(this,"_showNav")
		}).play(75);
	},

	_showNav: function(){
		// summary: fade in the footer, and setup our connections.
		dojo.fadeIn({ node: this.titleNode, duration:200 }).play(25);
	},

	hide: function(){
		// summary: close the Lightbox
		dojo.fadeOut({node:this.titleNode, duration:200 }).play(25); 
		this.inherited("hide", arguments);
		this.inGroup = null;
		this._positionIndex = null;
	},

	addImage: function(/* object */child,/* String? */group){
		// summary: add an image to this master dialog
		// 
		// child.href: String - link to image (required)
		// child.title: String - title to display
		//
		// group: String - attach to group of similar tag
		//	or null for individual image instance

		var g = group;
		if(!child.href){ return; }
		if(g){ 	
			if(this._groups[(g)]){
				this._groups[(g)].push(child); 
			}else{
				this._groups[(g)] = [(child)];
			}
		}else{ this._groups["XnoGroupX"].push(child); }
	},

	_handleKey: function(/* Event */e){
		// summary: handle keyboard navigation
		if(!this.open){ return; }
		var key = (e.charCode == dojo.keys.SPACE ? dojo.keys.SPACE : e.keyCode);
		switch(key){
			case dojo.keys.ESCAPE: this.hide(); break;

			case dojo.keys.DOWN_ARROW:
			case dojo.keys.RIGHT_ARROW:
			case 78: // key "n"
				this._nextImage(); break;

			case dojo.keys.UP_ARROW:
			case dojo.keys.LEFT_ARROW:
			case 80: // key "p" 
				this._prevImage(); break;
		}
	}
});

}
