/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
if(!dojo._hasResource["dojox.data.SlingNodeStore"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.data.SlingNodeStore"] = true;

dojo.require("dojo.data.util.simpleFetch");
dojo.require("dojo.data.util.filter");
dojo.provide("dojox.data.SlingNodeStore");

dojo.declare("dojox.data.SlingNodeStore", null, {
  url:"",
  statement:"",
  query:null,
  searchprops:null,
  overrideDeep:null,
  overrideDepth:null,
	/* summary:
	 *   The SlingNodeStore implements the dojo.data.api.Read API.  
	 */
	constructor: function(/* Object */ keywordParameters){
	    this.uri = keywordParameters.url;
	    this.statement = keywordParameters.statement;
	    
	    if(keywordParameters.query) {
	    	this.query = keywordParameters.query;
	    }
	    
    	if (dojo.isArray(keywordParameters.searchprops)) {
			this.searchprops = keywordParameters.searchprops;
		}else{
			if (dojo.isString(keywordParameters.searchprops)) {
				this.searchprops = keywordParameters.searchprops.split(",");
			}
		}
	    
	    if(keywordParameters.overrideDeep) {
	    	this.overrideDeep = keywordParameters.overrideDeep;
	    }
	    
	    if(keywordParameters.overrideDepth) {
	    	this.overrideDepth = keywordParameters.overrideDepth;
	    }
	},
  
  setUrl: function(/* String */ url) {
	this.uri = url;
  },
  setStatement: function(/* String */ statement) {
	this.statement = statement;
  },
  setSearchProps: function(/* Array|String */ props) {
	  if (dojo.isArray(props)) {
			this.searchprops = props;
		}else{
			if (dojo.isString(props)) {
				this.searchprops = props.split(",");
			}
		}
  },
  setOverrideDeep: function(/* Object */ overrideDeep) {
	  this.overrideDeep = overrideDeep;
  },
  setOverrideDepth: function(/* Object */ overrideDepth) {
	  this.overrideDepth = overrideDepth;
  },
  setQuery: function(/* Object */ query) {
	  this.query = query;
  },
  getValue: function(	/* item */ item, 
						/* attribute-name-string */ attribute, 
						/* value? */ defaultValue){
    //console.log("getValue " + attribute);
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    if (!dojo.isString(attribute)) {
      throw new Error(attribute + " is not a string");
    }
    if (attribute=="sling:uri") {
      return item.uri;
    }
    //TODO: handle child nodes and multi-value-properties
    if (item.node[attribute]) {
      return item.node[attribute]
    } else if (defaultValue) {
      return defaultValue;
    }
    return null;
	},
  
  getValues: function(/* item */ item,
						/* attribute-name-string */ attribute){
    //console.log("getValues");
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    if (!dojo.isString(attribute)) {
      throw new Error(attribute + " is not a string");
    }
    if (attribute=="sling:uri") {
    	return [item.uri];
    }
    if (attribute=="children"&&this._hasChildren(item)) {
      return this._getChildren(item);
    }
    if (item.node[attribute]) {
      if (dojo.isArray(item.node[attribute])) {
        return item.node[attribute]
      } else {
        return [item.node[attribute]];
      }
    } else {
      var array = [];
      return array; // an array that may contain literals and items
    }
	},
  
  _getChildren: function(item) {
    var children = [];
    
    for (var property in item.node) {
      if (dojo.isObject(item.node[property])) {
        if (item.node[property]["jcr:primaryType"]) {
        	var childuri = item.uri=="/" ? "/" + property : item.uri + "/" + property;
            var checkChild = {uri: childuri, node: item.node[property], depth: item.depth+1};
            
        	if(this.accept(checkChild, item.query)) {
        		var child = {uri: childuri, query: item.query, depth: item.depth+1 };
        		children.push(child);
        	}
        }
      }
    }
    //console.log("children: ");
    //console.dir(children);
    return children;
  },
  
  getAttributes: function(/* item */ item){
    //console.log("getAttributes");
    if (!this.isItem(item)) {
      return [];
    }
		var array = [];
		var children = false;
    
    for (var property in item.node) {
      if (!dojo.isObject(item.node[property])) {
        array.push(property);
      } else {
        children = true;
      }
    }
    if (children) {
      array.push("children");
    }
    array.push("sling:uri");
    
    return array; // array
	},
  
  hasAttribute: function(	/* item */ item,
							/* attribute-name-string */ attribute){
    //console.log("hasAttribute " + attribute);
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    if (!dojo.isString(attribute)) {
      throw new Error(attribute + " is not a string");
    }
    if (attribute=="children") {
      return this._hasChildren(item);
    }
    if (attribute=="sling:uri") {
    	return true;
    }
    if (item.node[attribute]) {
      return true;
    }
    return false;
  },
  
  _hasChildren: function(/* item */ item) {
    //children are all properties of a node that are objects
	if(this.overrideDepth && this.overrideDepth.treeDepth && item.depth && item.depth >= this.overrideDepth.treeDepth  ) {
		return false;
	}
    for (var property in item.node) {
      if (dojo.isObject(item.node[property])) {
        if (item.node[property]["jcr:primaryType"]) {
        	var childuri = item.uri=="/" ? "/" + property : item.uri + "/" + property;
            var checkChild = {uri: childuri, node: item.node[property], depth: item.depth+1};
            
        	if(this.accept(checkChild, item.query)) {
	          	//console.log("there are children");
	          	return true;
			}
        }
      }
    }
    //console.log("there are no children");
    return false;
  },
  
  containsValue: function(/* item */ item,
							/* attribute-name-string */ attribute, 
							/* anything */ value){
    //console.log("containsValue");
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
  
    //TODO: handle child nodes
    for (var property in item.node) {
      if (item.node[property]==value) {
        return true;
      }
    }
    return false;
  },
  
  /*
   var anitem = {uri: "http://localhost:8888/1.json", node: null};
  
  */
  isItem: function(/* anything */ something) {
    if (!something) {
      return false;
    }
    if (something.uri) {
      return true;
    }
    return false;
  },
  
  isItemLoaded: function(something) {
    //console.log("isItemLoaded");
    if (!this.isItem(something)) {
      return false;
    }
    if (something.node) {
      return true;
    }
    return false;
  },
  
  loadItem: function(/* object */ keywordArgs) {
    //console.log("loadItem");
    if (this.isItem(keywordArgs.item)) {
      //console.log("loading " + keywordArgs.item.uri);
      var url = keywordArgs.item.uri + ".1.json";
      
      var xhr = dojo.xhrGet({
          url: url,
          handleAs: "json-comment-optional",
          load: function(response, ioargs) {
            //console.dir(response);
            
            var item = keywordArgs.item;
            item.node = response;
            //console.log("filtering...");

            if (keywordArgs.onItem) {
              if (keywordArgs.scope) {
                keywordArgs.onItem.call(keywordArgs.scope, item)
              } else {
                keywordArgs.onItem(item);
              }
            }
            
          },
          error: function(error) {
            if (keywordArgs.onError) {
              if (keywordArgs.scope) {
                keywordArgs.onError.call(keywordArgs.scope, error)
              } else {
                keywordArgs.onError(error);
              }
            }
          }
      });
    } else {
      if (keywordArgs.onError) {
        //todo: respect scope
        if (keywordArgs.scope) {
          keywordArgs.onError.call(keywordArgs.scope, "no item to load");
        } else {
          keywordArgs.onError("no item to load");
        }
        
      }
    }
  },
  
  _fetchItems: function(	/* Object */ keywordArgs, 
							/* Function */ findCallback, 
							/* Function */ errorCallback) {
    var request = keywordArgs;
    
    var xhr;
    
    request.abort = function() {
      if (xhr.abort) {
        xhr.abort();
      }
    };
    
    var query = keywordArgs.query;
    var queryOptions = keywordArgs.queryOptions;
    //var startVal = keywordArgs.start;
    //var countVal = keywordArgs.count;
    	
    var depth = 0;
    var isDeep = false;
    var isSearch = false;
    
    // is this a deep query? (store setting overrides queryOption)
    if (this.overrideDeep) {
    	isDeep = this.overrideDeep.deep;
    } else if(queryOptions&&queryOptions.deep) {
   		isDeep = queryOptions.deep; 
    }
    
    // query depth/level specified? (store setting overrides queryOption)
    // deep = true & no depth => depth is infinite
    if(this.overrideDepth&&this.overrideDepth.depth) {
    	depth = this.overrideDepth.depth;
    } else if (queryOptions&&queryOptions.depth) {
    	depth = queryOptions.depth;
    } else if (isDeep){
    	depth = "infinity";
    }
    
    // mixin store query with caller-provided query
    // won't work well if two queries share an attribute
    if(this.query) {
    	if(!query) {
    		query = {};
    	}
    	dojo.mixin(query,this.query);
    }
    
    var url = this.uri + "."+(depth == "infinity" ? depth : depth+1)+".json";
    
    // was a statement provided? if so, doing search
    if(this.statement) {
    	isSearch = true;
    	
    	// to make descend behave properly
    	depth = "infinity";
    	isDeep = true;
    	
    	// construct search URL
    	url = this.uri + ".query.json?statement=" + this.statement;
    	
    	/*
    	if(startVal != 0){
			url = url + "&offset=" + startVal;
		}
		if(countVal && typeof countVal == "number"){
			if(isFinite(countVal)) {
				url = url + "&rows=" + countVal;
			}
		}
		*/
    	
		if(dojo.isArray(this.searchprops)) {
			dojo.forEach(this.searchprops,function(prop)  {
				url = url + "&property=" + prop;
			});
		}
    }
    
    var that = this;
    
    xhr = dojo.xhrGet({
        url: url,
        handleAs: "json-comment-optional",
        load: function(response, ioargs) {
          var item = { node: response, uri: that.uri, depth: 0};
          
          var items = [];
          if (request.onComplete) {
        	if (isSearch || dojo.isArray(response)) {
        		var checkitems = [];
        		dojo.forEach(response, function(item,index,array) {
        			var anItem = {};
        			anItem.node = item;
        			anItem.uri = item['jcr:path'];
        			anItem.depth = 0;
        			checkitems.push(anItem);
        		});
        		
        		items = that.descend(checkitems, query, depth, isDeep);
        	} else if (depth == "infinity" || depth > 0) {
              //console.log("hehe. got a level");
              var checkitems = [ { node: item.node, uri: that.uri, depth: 0} ];
              items = that.descend(checkitems, query, depth, isDeep );
            } else {
              if (that.accept(item, query)) {
                item.query = query;
                items.push(item);
              }
            }
            findCallback(items, keywordArgs);
          }
        }
    });
  },
  
  descend: function(items, query, level, isDeep) {
    var allitems = [];
    //console.log(level);
    if (level==0 || isDeep) {
    	 for (var i=0;i<items.length;i++) {
    		 var checkItem = items[i];
    		 if(this.accept(checkItem,query)) {
    			 checkItem.query = query;
    			 allitems.push(checkItem);
    		 }
    	 }
      //allitems = allitems.concat(items);
    } 
    
    var objFound = false;
    
    if(level != 0) {
      var newitems = [];
      for (var i=0;i<items.length;i++) {
        var item = items[i];
                
        for (var property in item.node) {
          if (!dojo.isArray(item.node[property])&&dojo.isObject(item.node[property])) {
        	objFound = true;
            var newitem = { node: item.node[property], uri: ( item.uri == "/" ? "" : item.uri ) + "/" + property, depth: item.depth+1};
            newitems.push(newitem);
          }
        }
      }
      
      if(level == "infinity" && objFound) {
   		  allitems = allitems.concat(this.descend(newitems, query, "infinity", isDeep));
      } else if (level >= 1) {
    	  allitems = allitems.concat(this.descend(newitems, query, level - 1, isDeep));
      }
    }
    //console.dir(allitems);
    return allitems;
  },
   
  accept: function(item, query) {
    //TODO: handle querying for arrays
    if (!query) {
      return true;
    }
       
    var ignoreCase = true;

	//See if there are any string values that can be regexp parsed first to avoid multiple regexp gens on the
	//same value for each item examined.  Much more efficient.
	var regexpList = {};
	for(var key in query){
		var value = query[key];
		if(typeof value === "string"){
			regexpList[key] = dojo.data.util.filter.patternToRegExp(value, ignoreCase);
		}
	}

	for (var property in query) {
      //console.log(property);
      if (item.node[property]) {
    	var checkItem = item;
        if (dojo.isArray(query[property])) {
          //console.log("multiple values possible");
          var onematch = false;
			
          for (var value in query[property]) {
            //console.log("checking value " + query[property][value]);
        	  if (this._containsValue(checkItem, property, query[property][value], regexpList[property])){
        		  onematch = true;
  			  }
          }
          if (!onematch) {
            //console.log("required property " + property + " has wrong value "+ item.node[property]);
            return false;
          }
        } else {
        	 if (!this._containsValue(checkItem, property, checkItem.node[property], regexpList[property])){
        		//console.log("required property " + property + " has wrong value "+ item.node[property]);
       		  	return false;
 			  }
        }
      } else {
        //console.log("required property " + property + " missing");
        return false;
      }
    }
    return true;
  },
  
  _containsValue: function(	/* item */ item, 
			/* attribute-name-string */ attribute, 
			/* anything */ value,
			/* RegExp?*/ regexp){
	//	summary: 
	//		Internal function for looking at the values contained by the item.
	//	description: 
	//		Internal function for looking at the values contained by the item.  This 
	//		function allows for denoting if the comparison should be case sensitive for
	//		strings or not (for handling filtering cases where string case should not matter)
	//	
	//	item:
	//		The data item to examine for attribute values.
	//	attribute:
	//		The attribute to inspect.
	//	value:	
	//		The value to match.
	//	regexp:
	//		Optional regular expression generated off value if value was of string type to handle wildcarding.
	//		If present and attribute values are string, then it can be used for comparison instead of 'value'
	  return dojo.some(this.getValues(item, attribute), function(possibleValue){
		if(possibleValue !== null && !dojo.isObject(possibleValue) && regexp){
			if(possibleValue.toString().match(regexp)){
				return true; // Boolean
			}
		}else if(value === possibleValue){
			return true; // Boolean
		}
	  });
	},

  getFeatures: function() {
    return {
      "dojo.data.api.Read": true,
      'dojo.data.api.Write': true,
      'dojo.data.api.Identity': true,
      'dojo.data.api.Notification': true
    };
  },
  
  close: function(/*dojo.data.api.Request || keywordArgs || null */ request) {
    //nothing to do here
  },
  
  getLabel: function(/* item */ item) {
    //TODO: use item.node["title"] or item.node["jcr:title"]
    if (item["jcr:title"]) {
      return item["jcr:title"];
    }
    if (item["title"]) {
      return item["title"];
    }
    if (item.uri.length==1) {
      return "/"
    } else {
      return item.uri.replace(/.*\//,"")
    }
  },
  
  getLabelAttributes: function(/* item */ item) {
    //TODO: use item.node["title"] or item.node["jcr:title"]
    return [];
  },
  
  
  //dojo.data.api.Identity functions
  
  getIdentity: function(/* item */ item) {
    //console.log("getIdentity");
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    return item.uri;
  },
  
  getIdentityAttributes: function(/* item */ item) {
    //console.log("getIdentityAttributes");
    //identity depends on the URI, not the representation
    return ["sling:uri"];
  },
  
  fetchItemByIdentity: function(/* object */ keywordArgs) {
		// 	keywordArgs:
		//		An anonymous object that defines the item to locate and callbacks to invoke when the 
		//		item has been located and load has completed.  The format of the object is as follows:
		//		{
		//			identity: string|object,
		//			onItem: Function,
		//			onError: Function,
		//			scope: object
		//		}
    
	 var request = keywordArgs;
	 
	 var url = keywordArgs.identity + ".1.json";
	 
	 var that = this;
	    
	    xhr = dojo.xhrGet({
	        url: url,
	        handleAs: "json-comment-optional",
	        load: function(response, ioargs) {
	          var item = { node: response, uri: request.identity};
	          
	          if (request.onItem) {
	        	request.onItem(item);
	          }
	        },
	        error: function(response, ioargs) {
	        	if(request.onError) {
	        		request.onError(response);
	        	}
	        }
	    });
  },
  
  
    //dojo.data.api.Write functions
  
  newItem: function(/* Object? */ keywordArgs, /*Object?*/ parentInfo) {
    console.log("newItem");
    var item = keywordArgs;
    var that = this;
    
    var xhr =  xhr = dojo.xhrPost({
        url: item.uri,
        content: item,
        load: function(response, ioargs) {
          item.dirty = false;
          console.log("onNew");
          that.onNew(item, null);
        }
    });
    return item;
  },
  
  deleteItem: function(/* item */ item) { console.log("deleteItem");
    throw new Error('Unimplemented API: dojo.data.api.Write.deleteItem');
    console.log("deleteItem");
		return false; // boolean
  },
  
  setValue: function(	/* item */ item, 
						/* string */ attribute,
						/* almost anything */ value) {
    console.log("setValue");
    //you can only set the value of an item, so
    var oldvalue = item[attribute];
    item[attribute] = value;
    var that = this;
    
    var xhr =  xhr = dojo.xhrPost({
        url: item.uri,
        content: item,
        load: function(response, ioargs) {
          that.onSet(item, attribute, oldvalue, value);
        }
    });
  },
  
  
  setValues: function(item, attribute, values) { console.log("setValues");
    this.setValue(item, attribute, values);
  },
  
  unsetAttribute: function(	/* item */ item, 
								/* string */ attribute){
		//	summary:
		//		Deletes all the values of an attribute on an item.
		//
		//	item:
		//		The item to modify.
		//	attribute:
		//		The attribute of the item to unset represented as a string.
		//
		//	exceptions:
		//		Throws an exception if *item* is not an item, or if *attribute*
		//		is neither an attribute object or a string.
		//	example:
		//	|	var success = store.unsetAttribute(kermit, "color");
		//	|	if (success) {assert(!store.hasAttribute(kermit, "color"));}
		throw new Error('Unimplemented API: dojo.data.api.Write.unsetAttribute');
		return false; // boolean
	},
  
  save: function(/* object */ keywordArgs) { console.log("save");
    console.log("save");
  },
  
  revert: function(){ console.log("revert");
    throw new Error('Unimplemented API: dojo.data.api.Write.revert');
		return false; // boolean
	},
  
  isDirty: function(/* item? */ item) { console.log("isDirty");
    return false;
  },
  
  // Notification API
  
  onSet: function(/* item */ item, 
					/*attribute-name-string*/ attribute, 
					/*object | array*/ oldValue,
					/*object | array*/ newValue){
		// summary: See dojo.data.api.Notification.onSet()
		
		// No need to do anything. This method is here just so that the 
		// client code can connect observers to it. 
	},

	onNew: function(/* item */ newItem, /*object?*/ parentInfo){
		// summary: See dojo.data.api.Notification.onNew()
		
		// No need to do anything. This method is here just so that the 
		// client code can connect observers to it. 
	},

	onDelete: function(/* item */ deletedItem){
		// summary: See dojo.data.api.Notification.onDelete()
		
		// No need to do anything. This method is here just so that the 
		// client code can connect observers to it. 
	}
  
});

dojo.extend(dojox.data.SlingNodeStore,dojo.data.util.simpleFetch);

}

