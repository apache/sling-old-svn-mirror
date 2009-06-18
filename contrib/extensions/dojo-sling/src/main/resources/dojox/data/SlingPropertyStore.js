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
if(!dojo._hasResource["dojox.data.SlingPropertyStore"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.data.SlingPropertyStore"] = true;

dojo.require("dojo.data.util.simpleFetch");
dojo.require("dojo.data.util.filter");
dojo.provide("dojox.data.SlingPropertyStore");

dojo.declare("dojox.data.SlingPropertyStore", null, {
  url:"",
  query:null,
  
	/* summary:
	 *   The SlingPropertyStore implements the dojo.data.api.Read API. 
   *   It will return one data item for every property found
	 */
	constructor: function(/* Object */ keywordParameters){ console.log("constructor");
	    this.uri = keywordParameters.url;
	    if(keywordParameters.query) {
	    	this.query = keywordParameters.query;
	    }
	},
  setUrl: function(/* String */ url) {
		this.uri = url;
  },
  setQuery: function(/* Object */ query) {
	  this.query = query;
  },
  getValue: function(	/* item */ item,  /* attribute-name-string */ attribute,  /* value? */ defaultValue){
    //console.log("getValue " + attribute + " " + item.name);
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    if (!dojo.isString(attribute)) {
      throw new Error(attribute + " is not a string");
    }
    return item[attribute];
	},
  
  getValues: function(/* item */ item, /* attribute-name-string */ attribute){ console.log("getValues");
    //console.log("getValues");
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    if (!dojo.isString(attribute)) {
      throw new Error(attribute + " is not a string");
    }
    if (item[attribute]) {
      if (dojo.isArray(item[attribute])) {
        return item[attribute]
      } else {
        return [item[attribute]];
      }
    } else {
      var array = [];
      return array; // an array that may contain literals and items
    }
  },
  
  getAttributes: function(/* item */ item){ //console.log("getAttributes");
    //console.log("getAttributes");
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
		var array = [];
		for (var property in item) {
      array.push(property);
    }
    
    return array; // array
	},
  
  hasAttribute: function(	/* item */ item, /* attribute-name-string */ attribute){ console.log("hasAttribute");
    //console.log("hasAttribute " + attribute);
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
    if (!dojo.isString(attribute)) {
      throw new Error(attribute + " is not a string");
    }
    return item[attribute];
  },
  
  containsValue: function(/* item */ item, /* attribute-name-string */ attribute,  /* anything */ value){ console.log("containsValue");
    //console.log("containsValue");
    if (!this.isItem(item)) {
      throw new Error(item + " is not an item");
    }
  
    //TODO: handle child nodes
    for (var property in item) {
      if (item[property]==value) {
        return true;
      }
    }
    return false;
  },
  
  /*
   var anitem = {uri: "http://localhost:8888/1.json", node: null};
  
  */
  isItem: function(/* anything */ something) {
    //console.log("isItem " + something.uri);
    if (something.uri) {
      return true;
    }
    return false;
  },
  
  isItemLoaded: function(something) { console.log("isItemLoaded");
    return true;
  },
  
  loadItem: function(/* object */ keywordArgs) { console.log("loadItem");
    return;
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
    
    var depth = 0;
    
    var url = this.uri + "."+depth+".json";
    var that = this;
    
    // mixin store query with caller-provided query
    // won't work well if two queries share an attribute
    if(this.query) {
    	if(!query) {
    		query = {};
    	}
    	dojo.mixin(query,this.query);
    }
    
    xhr = dojo.xhrGet({
        url: url,
        handleAs: "json-comment-optional",
        load: function(response, ioargs) {
          var items = [];
          for (var property in response) {
            if (dojo.isArray(response[property]) || !dojo.isObject(response[property])) {
              //console.debug(property);
            	var checkitem = { uri: that.uri, name: property, value: response[property]};
            	if(that.accept(checkitem,query)) {
            		items.push(checkitem);
            	}
            }
          }
          findCallback(items, keywordArgs);
        }
    });
    
  },
  
  accept: function(item, query) {
	  // TODO: handle querying for arrays
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
      if (item[property]) {
    	var checkItem = item;
        if (dojo.isArray(query[property])) {
          //console.log("multiple values possible");
          var onematch = false;
			
          for (var value in query[property]) {
            //console.log("checking value " + query[property][value]);
        	  if (!this._containsValue(checkItem, property, checkItem[property], regexpList[property])){
        		  onematch = false;
  			  }
          }
          if (!onematch) {
            //console.log("required property " + property + " has wrong value "+ item.node[property]);
            return false;
          }
        } else {
        	 if (!this._containsValue(checkItem, property, checkItem[property], regexpList[property])){
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
  
  getFeatures: function() { console.log("getFeatures");
    console.log("getFeatures");
    return {
      "dojo.data.api.Read": true,
      'dojo.data.api.Write': true,
      'dojo.data.api.Identity': true,
      'dojo.data.api.Notification': true
    };
  },
  
  close: function(/*dojo.data.api.Request || keywordArgs || null */ request) { console.log("close");
    //nothing to do here
  },
  
  getLabel: function(/* item */ item) { console.log("getLabel");
    if (item.uri.length==1) {
      return "/"
    } else {
      return item.uri.replace(/.*\//,"")
    }
  },
  
  getLabelAttributes: function(/* item */ item) { console.log("getLabelAttributes");
    //TODO: use item.node["title"] or item.node["jcr:title"]
    return ["uri"];
  },
  
    //dojo.data.api.Identity functions
  
  getIdentity: function(/* item */ item) { //console.log("getIdentity");
    //console.log("getIdentity");
    if (!this.isItem(item)) {
      console.log("error: not an item");
      throw new Error(item + " is not an item");
    }
    return this.uri + "[" + item.name + "]";
  },
  
  getIdentityAttributes: function(/* item */ item) { console.log("getIdentityAttributes");
    //console.log("getIdentityAttributes");
    //identity depends on the URI, not the representation
    return ["uri", "name"];
  },
  
  fetchItemByIdentity: function(/* object */ keywordArgs) { console.log("fetchItemByIdentity");
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
    
  	var itemId = keywordArgs.identity;
  	var idParts = itemId.split('['); 
  	var itemUri = idParts[0];
  	var itemName = idParts[1].substring(0,idParts[1].length-1);
  	var url = itemUri + "/" + itemName + ".0.json";
	 
	 var that = this;
	    
	    xhr = dojo.xhrGet({
	        url: url,
	        handleAs: "json-comment-optional",
	        load: function(response, ioargs) {
	          
	          if (request.onItem) {
	        	  for (var property in response) {
	                  if (dojo.isArray(response[property]) || !dojo.isObject(response[property])) {
	                  	var checkitem = { uri: itemUri, name: property, value: response[property]};
	                  	request.onItem(checkitem);
	                  	break;
	                  }
	        	  }
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
    item.uri = this.uri;
    item.dirty = true;
    
    content = {};
    content[item.name] = item.value;
    
    var that = this;
    
    var xhr =  xhr = dojo.xhrPost({
        url: item.uri,
        content: content,
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
    
    var oldvalue = item.value;
    
    item.value = value;
    item.dirty = true;
    
    content = {};
    content[item.name] = item.value;
    
    var that = this;
    
    var xhr =  xhr = dojo.xhrPost({
        url: item.uri,
        content: content,
        load: function(response, ioargs) {
          item.dirty = false;
          that.onSet(item, attribute, oldvalue, value);
        }
    });
    
  },
  
  setValues: function(item, attribute, values) { console.log("setValues");
    console.log("setValues");
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
    if (item.dirty) {
      return item.dirty;
    }
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

dojo.extend(dojox.data.SlingPropertyStore,dojo.data.util.simpleFetch);

}

