if(!dojo._hasResource["dojox.image.SlideShow"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.image.SlideShow"] = true;
dojo.provide("dojox.image.SlideShow");
//
// dojox.image.SlideShow courtesy Shane O Sullivan, licensed under a Dojo CLA 
// For a sample usage, see http://www.skynet.ie/~sos/photos.php
//
// @author  Copyright 2007 Shane O Sullivan (shaneosullivan1@gmail.com)
// @license Licensed under the Academic Free License 3.0 http://www.opensource.org/licenses/afl-3.0.php
//
//	TODO: more cleanups
//
dojo.require("dojo.fx");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");

dojo.declare("dojox.image.SlideShow",
	[dijit._Widget, dijit._Templated],
	{
	// imageHeight: Number
	//	The maximum height of an image
	imageHeight: 375,
	
	// imageWidth: Number
	//	The maximum width of an image.
	imageWidth: 500,

	// title: String
	//	the initial title of the SlideShow 
	title: "",

	// titleTemplate: String
	//	a way to customize the wording in the title. supported tags to be populated are:
	//		@title = the passed title of the image
	//		@current = the current index of the image
	//		@total = the total number of images in the SlideShow
	//
	//	should add more?
	titleTemplate: '@title <span class="slideShowCounterText">(@current of @total)</span>',

	// noLink: Boolean
	//	Prevents the slideshow from putting an anchor link around the displayed image
	//	enables if true, though still will not link in absence of a url to link to
	noLink: false,

	// loop: Boolean
	//	true/false - make the slideshow loop
	loop: true,

	// hasNav: Boolean
	//	toggle to enable/disable the visual navigation controls
	hasNav: true,

	// images: Array
	// Contains the DOM nodes that individual images are stored in when loaded or loading.
	images: [],
	
	// pageSize: Number
	//	The number of images to request each time.
	pageSize: 20,
		
	// autoLoad: Boolean
	//	If true, then images are preloaded, before the user navigates to view them.
	//	If false, an image is not loaded until the user views it.
	autoLoad: true,
	
	// fixedHeight: Boolean
	// If true, the widget does not resize itself to fix the displayed image.
	fixedHeight: false,

	// imageStore: Object
	//	Implementation of the dojo.data.api.Read API, which provides data on the images
	//	to be displayed.
	imageStore: null,
		
	// linkAttr: String
	//	Defines the name of the attribute to request from the store to retrieve the
	//	URL to link to from an image, if any.
	linkAttr: "link",
	
	// imageLargeAttr: String
	//	Defines the name of the attribute to request from the store to retrieve the
	//	URL to the image.
	imageLargeAttr: "imageUrl",
	
	// titleAttr: String
	//	Defines the name of the attribute to request from the store to retrieve the
	//	title of the picture, if any.
	titleAttr: "title",

	// slideshowInterval: Number
	// Time, in seconds, between image transitions during a slideshow.
	slideshowInterval: 3,
	
	templateString:"<div dojoAttachPoint=\"outerNode\" class=\"slideShowWrapper\">\n\t<div style=\"position:relative;\" dojoAttachPoint=\"innerWrapper\">\n\t\t<div class=\"slideShowNav\" dojoAttachEvent=\"onclick: _handleClick\">\n\t\t\t<div class=\"dijitInline slideShowTitle\" dojoAttachPoint=\"titleNode\">${title}</div>\n\t\t</div>\n\t\t<div dojoAttachPoint=\"navNode\" class=\"slideShowCtrl\" dojoAttachEvent=\"onclick: _handleClick\">\n\t\t\t<span dojoAttachPoint=\"navPrev\" class=\"slideShowCtrlPrev\"></span>\n\t\t\t<span dojoAttachPoint=\"navPlay\" class=\"slideShowCtrlPlay\"></span>\n\t\t\t<span dojoAttachPoint=\"navNext\" class=\"slideShowCtrlNext\"></span>\n\t\t</div>\n\t\t<div dojoAttachPoint=\"largeNode\" class=\"slideShowImageWrapper\"></div>\t\t\n\t\t<div dojoAttachPoint=\"hiddenNode\" class=\"slideShowHidden\"></div>\n\t</div>\n</div>\n",
	
	// _tempImgPath: URL
	//	URL to the image to display when an image is not yet fully loaded.
	_tempImgPath: dojo.moduleUrl("dojox.image", "resources/images/1pixel.gif"),

	// _imageCounter: Number
	//	A counter to keep track of which index image is to be loaded next
	_imageCounter: 0,
	
	// _tmpImage: DomNode
	//	The temporary image to show when a picture is loading.
	_tmpImage: null,
	
	// _request: Object
	//	Implementation of the dojo.data.api.Request API, which defines the query 
	//	parameters for accessing the store.
	_request: null,

	postCreate: function(){
		// summary: Initilizes the widget, sets up listeners and shows the first image
		this.inherited("postCreate",arguments);
		var img = document.createElement("img");

		// FIXME: should API be to normalize an image to fit in the specified height/width?
		img.setAttribute("width", this.imageWidth);
		img.setAttribute("height", this.imageHeight);

		if(this.hasNav){
			dojo.connect(this.outerNode, "onmouseover", function(evt){
				try{_this._showNav();}
				catch(e){}			
			});		
			dojo.connect(this.outerNode, "onmouseout", function(evt){
				try{_this._hideNav(evt);}
				catch(e){}
			});
		}
		
		this.outerNode.style.width = this.imageWidth + "px";

		img.setAttribute("src", this._tempImgPath);
		var _this = this;
		
		this.largeNode.appendChild(img);
		this._tmpImage = img;
		this._currentImage = img;
		this._fitSize(true);
		
		this._loadImage(0, function(){
		     _this.showImage(0);
		});
		this._calcNavDimensions();
	},

	setDataStore: function(dataStore, request, /*optional*/paramNames){
		// summary: Sets the data store and request objects to read data from.
		// dataStore:
		//	An implementation of the dojo.data.api.Read API. This accesses the image
		//	data.
		// request:
		//	An implementation of the dojo.data.api.Request API. This specifies the
		//	query and paging information to be used by the data store
		// paramNames:
		//	An object defining the names of the item attributes to fetch from the
		//	data store.  The three attributes allowed are 'linkAttr', 'imageLargeAttr' and 'titleAttr'
		this.reset();
		var _this = this;
		this._request = {
			query: {},
			start: ((request.start) ? request.start : 0),
			count: ((request.count) ? request.count : this.pageSize),
			onBegin: function(count, request){
				_this.maxPhotos = count;
			}
		};
		if(request.query){ dojo.mixin(this._request.query, request.query); }
		if(paramNames && paramNames.imageLargeAttr){
			this.imageLargeAttr = paramNames.imageLargeAttr;
		}
	
		var _this = this;
		var _complete = function(items){
			_this.showImage(0); 
			_this._request.onComplete = null;
		};
		
		this.imageStore = dataStore;
		this._request.onComplete = _complete;
		this._request.start = 0;
		this.imageStore.fetch(this._request);
	},

	reset: function(){
		// summary: Resets the widget to its initial state
		// description: Removes all previously loaded images, and clears all caches.
		while(this.largeNode.firstChild){
			this.largeNode.removeChild(this.largeNode.firstChild);
		}
		this.largeNode.appendChild(this._tmpImage);
		while(this.hiddenNode.firstChild){
			this.hiddenNode.removeChild(this.hiddenNode.firstChild);
		}
		var img;
		for(var pos = 0; pos < this.images.length; pos++){
			img = this.images[pos];
			if(img && img.parentNode){ img.parentNode.removeChild(img); }
		}
		this.images = [];
		this.isInitialized = false;
		this._imageCounter = 0;
	},

	isImageLoaded: function(idx){
		// summary: Returns true if image at the specified index is loaded, false otherwise.
		// idx:
		//	The number index in the data store to check if it is loaded.
		return this.images && this.images.length > index && this.images[idx];
	},

	moveImageLoadingPointer: function(idx){
		// summary: If 'autoload' is true, this tells the widget to start loading
		//	images from the specified pointer.
		// idx:
		//	The number index in the data store to start loading images from.
		this._imageCounter = idx;
	},
	
	destroy: function(){
		// summary: Cleans up the widget when it is being destroyed
		if(this._slideId) { this._stop(); }
		this.inherited("destroy",arguments);
	},

	showNextImage: function(inTimer, forceLoop){
		// summary: Changes the image being displayed to the next image in the data store
		// inTimer: Boolean
		//	If true, a slideshow is active, otherwise the slideshow is inactive.
		if(inTimer && this._timerCancelled){return false;}
		
		if(this.imageIndex + 1 >= this.maxPhotos){
			if(inTimer && (this.loop || forceLoop)){ this.imageIndex = -1; }
			else{
				if(this._slideId){ this._stop(); }
				return false;
			}
		}
		var _this = this;
		this.showImage(this.imageIndex + 1, function(){
			if(inTimer){ _this._startTimer(); }
		});
		return true;
	},

	toggleSlideShow: function(){
		// summary: Switches the slideshow mode on and off.
		if(this._slideId){
			this._stop();
		}else{
			dojo.toggleClass(this.domNode,"slideShowPaused");			
			this._timerCancelled = false;
			var success = this.showNextImage(true, true);
			if(!success){
				this._stop();
			}
		}
	},

	getShowTopicName: function(){
		// summary: Returns the topic id published to when an image is shown
		// description:
		//	The information published is: index, title and url
		return (this.widgetId ? this.widgetId : this.id) + "/imageShow";
	},

	getLoadTopicName: function(){
		// summary: Returns the topic id published to when an image finishes loading.
		// description:
		//	The information published is the index position of the image loaded.
		return (this.widgetId ? this.widgetId : this.id) + "/imageLoad";
	},

	showImage: function(idx, /* Function? */callback){
		// summary: Shows the image at index 'idx'.
		// idx: Number
		//	The position of the image in the data store to display
		// callback: Function
		//	Optional callback function to call when the image has finished displaying.
		
		if(!callback && this._slideId){ this.toggleSlideShow(); }
		var _this = this;
		var current = this.largeNode.getElementsByTagName("div");
		this.imageIndex = idx;

		var showOrLoadIt = function() {
			//If the image is already loaded, then show it. 
			if(_this.images[idx]){
				while(_this.largeNode.firstChild){
					_this.largeNode.removeChild(_this.largeNode.firstChild);
				}
				_this.images[idx].style.opacity = 0;
				_this.largeNode.appendChild(_this.images[idx]);
				_this._currentImage = _this.images[idx]._img;
				_this._fitSize();
								
			    var onEnd = function(a,b,c) {
					var img = _this.images[idx].firstChild;
					if(img.tagName.toLowerCase() != "img"){img = img.firstChild;}
					title = img.getAttribute("title");
					
					if(_this._navShowing){
						_this._showNav(true);
					}
					dojo.publish(_this.getShowTopicName(), [{
						index: idx,	
						title: title,
						url: img.getAttribute("src")
					}]);
        			if(callback) { callback(a,b,c); }
					_this._setTitle(title);
        		};
				
				dojo.fadeIn({
					node: _this.images[idx],
					duration: 300,
					onEnd: onEnd
				}).play();
			}else{
				//If the image is not loaded yet, load it first, then show it.
				_this._loadImage(idx, function(){
					dojo.publish(_this.getLoadTopicName(), [idx]);
					_this.showImage(idx, callback);	
				});
			}
		};

		//If an image is currently showing, fade it out, then show
		//the new image. Otherwise, just show the new image. 	
		if(current && current.length > 0){
			dojo.fadeOut({
				node: current[0],
				duration: 300,
				onEnd: function(){
					_this.hiddenNode.appendChild(current[0]);
					showOrLoadIt();
			}
			}).play();
		}else{
			showOrLoadIt();
		}
	},
	
	_fitSize: function(force){
		// summary: Fits the widget size to the size of the image being shown,
		//	or centers the image, depending on the value of 'fixedHeight'
		// force: Boolean
		//	If true, the widget is always resized, regardless of the value of 'fixedHeight'
		if(!this.fixedHeight || force){
			var height = (this._currentImage.height + (this.hasNav ? 20:0));
			dojo.style(this.innerWrapper, "height", height + "px");
			return;
		}
		dojo.style(this.largeNode, "paddingTop", this._getTopPadding() + "px");
	},
	
	_getTopPadding: function(){
		if(!this.fixedHeight){return 0;}
		// summary: Returns the padding to place at the top of the image to center it vertically.
		return (this.imageHeight - this._currentImage.height)/2;
	},
	
	_loadNextImage: function(){
		//summary: Load the next unloaded image.
		if(!this.autoLoad){ return; }
		while(this.images.length >= this._imageCounter && this.images[this._imageCounter]){
			this._imageCounter++;
		}
		this._loadImage(this._imageCounter);
	},
	
	_loadImage: function(idx, callbackFn){
		// summary: Load image at specified index
		// description:
		//	This function loads the image at position 'idx' into the
		//	internal cache of images.  This does not cause the image to be displayed.
		// idx:
		//	The position in the data store to load an image from.
		// callbackFn:
		//	An optional function to execute when the image has finished loading.	
		if(this.images[idx] || !this._request) { return; }
		
		var pageStart = idx - (idx % this.pageSize);

		this._request.start = pageStart;		

		this._request.onComplete = function(items){
			var diff = idx - pageStart;
			if(items && items.length > diff){
				loadIt(items[diff]);
			}else{ /* Squelch - console.log("Got an empty set of items"); */ }
		}

		var _this = this;	
		var loadIt = function(item){			
			var url = _this.imageStore.getValue(item, _this.imageLargeAttr);
			var img = document.createElement("img");
			var div = document.createElement("div");
			div._img = img;

			var link = _this.imageStore.getValue(item,_this.linkAttr);
			if(!link || _this.noLink){ div.appendChild(img); 
			}else{
				var a = document.createElement("a");
				a.setAttribute("href", link);
				a.setAttribute("target","_blank");
				div.appendChild(a);
				a.appendChild(img);
			}

			div.setAttribute("id",_this.id + "_imageDiv" + idx);
			dojo.connect(img, "onload", function(){
				_this._fitImage(img);
				div.setAttribute("width",_this.imageWidth);
				div.setAttribute("height",_this.imageHeight);				
				
				dojo.publish(_this.getLoadTopicName(), [idx]);
				_this._loadNextImage();
				if(callbackFn){ callbackFn(); }
			});
			_this.hiddenNode.appendChild(div);

			var titleDiv = document.createElement("div");
			dojo.addClass(titleDiv, "slideShowTitle");
			div.appendChild(titleDiv);
		
			_this.images[idx] = div;
			img.setAttribute("src", url);
			
			var title = _this.imageStore.getValue(item,_this.titleAttr);
			if(title){ img.setAttribute("title",title); } 
		}
		this.imageStore.fetch(this._request);
	},

	_stop: function(){
		// summary: Stops a running slide show.
		if(this._slideId) { clearTimeout(this._slideId); }
		this._slideId = null;
		this._timerCancelled = true;
		dojo.removeClass(this.domNode,"slideShowPaused");
	},

	_prev: function(){
		// summary: Show the previous image.
		// FIXME: either pull code from showNext/prev, or call it here
		if(this.imageIndex < 1) { return;}
		this.showImage(this.imageIndex - 1);
	},

	_next: function(){
		// summary: Show the next image
		this.showNextImage();
	},

	_startTimer: function(){
		// summary: Starts a timeout to show the next image when a slide show is active
		this._slideId = setTimeout("dijit.byId('"+this.id +"').showNextImage(true);", this.slideshowInterval * 1000);
	},
	
	_calcNavDimensions: function() {
		// summary:
		//	Calculates the dimensions of the navigation controls
		dojo.style(this.navNode, "position", "absolute");
		
		//Place the navigation controls far off screen
		dojo.style(this.navNode, "left", "-10000px");
		
		//Make the navigation controls visible
		dojo._setOpacity(this.navNode, 99);
		
		this.navPlay._size = dojo.marginBox(this.navPlay);
		this.navPrev._size = dojo.marginBox(this.navPrev);
		this.navNext._size = dojo.marginBox(this.navNext);
		
		dojo._setOpacity(this.navNode, 0);
		dojo.style(this.navNode, "position", "");
		dojo.style(this.navNode, "left", "");		
	},

	_setTitle: function(title){
		// summary: Sets the title of the image to be displayed
		// title: String
		//	The String title of the image
		this.titleNode.innerHTML = this.titleTemplate.replace('@title',title)
			.replace('@current', String(Number(this.imageIndex) + 1)) 
			.replace('@total',String(this.maxPhotos));
	},
	
	_fitImage: function(img) {
		// summary: Ensures that the image width and height do not exceed the maximum.
		// img: Node
		//	The image DOM node to optionally resize
		var width = img.width
		var height = img.height;
		
		if(width > this.imageWidth){
			height = Math.floor(height * (this.imageWidth / width));
			img.setAttribute("height", height + "px");
			img.setAttribute("width", this.imageWidth + "px");			
		}
		if(height > this.imageHeight){
			width = Math.floor(width * (this.imageHeight / height));
			img.setAttribute("height", this.imageHeight + "px");
			img.setAttribute("width", width + "px");
		}
	},
	
	_handleClick: function(/* Event */e){
		// summary: Performs navigation on the images based on users mouse clicks
		// e:
		//	An Event object
		switch(e.target){
			case this.navNext:this._next(); break;
			case this.navPrev:this._prev(); break;
			case this.navPlay:this.toggleSlideShow(); break;
		}
	},
	
	_showNav: function(force){
		// summary:
		//	Shows the navigation controls
		// force: Boolean
		//	If true, the navigation controls are repositioned even if they are
		//	currently visible.
		if(this._navShowing && !force){return;}
		dojo.style(this.navNode, "marginTop", "0px");
		dojo.style(this.navPlay, "marginLeft", "0px");
		var wrapperSize = dojo.marginBox(this.outerNode);
		
		var margin = this._currentImage.height - this.navPlay._size.h - 10 + this._getTopPadding();
		
		if(margin > this._currentImage.height){margin += 10;}
		dojo[this.imageIndex < 1 ? "addClass":"removeClass"](this.navPrev, "slideShowCtrlHide");
		dojo[this.imageIndex + 1 >= this.maxPhotos ? "addClass":"removeClass"](this.navNext, "slideShowCtrlHide");
	
		var _this = this;
		if(this._navAnim) {
			this._navAnim.stop();
		}
		if(this._navShowing){return;}
		this._navAnim = dojo.fadeIn({node: this.navNode, duration: 300,
							onEnd: function(){_this._navAnim=null;}});
		
		this._navAnim.play();
		this._navShowing = true;
	},
	
	_hideNav: function(/* Event */e){
		// summary:	Hides the navigation controls
		// e: Event
		//	The DOM Event that triggered this function
		if(!e || !this._overElement(this.outerNode, e)) {
			var _this = this;
			if(this._navAnim) {
				this._navAnim.stop();
			}
			this._navAnim = dojo.fadeOut({node: this.navNode,duration:300,
						 onEnd: function(){_this._navAnim=null;}});
			this._navAnim.play();
			this._navShowing = false;
		}
	},
	
	_overElement: function(/*DomNode*/element, /*Event*/e){
		// summary:
		//	Returns whether the mouse is over the passed element.
		//	Element must be display:block (ie, not a <span>)
		
		//When the page is unloading, if this method runs it will throw an
		//exception.
		if(typeof(dojo)=="undefined"){return false;}
		element = dojo.byId(element);
		var m = {x: e.pageX, y: e.pageY};
		var bb = dojo._getBorderBox(element);
		var absl = dojo.coords(element, true);
		var left = absl.x;

		return (m.x >= left
			&& m.x <= (left + bb.w)
			&& m.y >= absl.y
			&& m.y <= (top + bb.h)
		);	//	boolean
	}
});

}
