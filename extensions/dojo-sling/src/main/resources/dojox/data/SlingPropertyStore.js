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
    return item[attribute];
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
      errorCallback.("XHR aborted", keywordArgs)
    };
    
    var depth = 1;
    
    var url = this.uri + "."+depth+".json";
    var that = this;
    
    var query = keywordArgs.query;
  
    xhr = dojo.xhrGet({
        url: url,
        handleAs: "json-comment-optional",
        load: function(response, ioargs) {
          var items = [];
          for (var property in response) {
            if (!dojo.isObject(response[property])) {
              //console.debug(property);
              items.push({ uri: that.uri, name: property, value: response[property]});
            }
          }
          findCallback(items, keywordArgs);
        }
    });
    
  },
  
  _nofetch: function(/* object */ keywordArgs) { console.log("fetch");
    var request = keywordArgs;
    
    var xhr;
    
    request.abort = function() {
      if (xhr.abort) {
        xhr.abort();
      }
    };
    
    var depth = 1;
    
    /*
    if (keywordArgs.count) {
      depth = keywordArgs.count;
    }
    */
    
    var url = this.uri + "."+depth+".json";
    var that = this;
    
    var query = keywordArgs.query;
    
    xhr = dojo.xhrGet({
        url: url,
        handleAs: "json-comment-optional",
        load: function(response, ioargs) { console.log("load");
          //console.dir(response);
          var items = [];
          
          for (var property in response) {
            if (!dojo.isObject(response[property])) {
              //console.debug(property);
              items.push({ uri: that.uri, name: property, value: response[property]});
            }
          }
          
          if (request.onComplete) {
            if (request.scope) {
              request.onComplete.call(request.scope, item. request)
            } else {
              request.onComplete(items, request);
            }
            //request.onComplete(items, request);
          }
        }
    });
    //TODO: implement
    //alert(this.url);
    return request;
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
    return item.uri + "[" + item.name + "]";
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
    
    //TODO: implement
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
    var oldvalues = item.values;
    item.value = values;
    item.dirty = true;
    
    this.onSet(item, attribute, oldvalues, values);
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

