if(!dojo._hasResource["dojo.data.ItemFileWriteStore"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojo.data.ItemFileWriteStore"] = true;
dojo.provide("dojo.data.ItemFileWriteStore");
dojo.require("dojo.data.ItemFileReadStore");

dojo.declare("dojo.data.ItemFileWriteStore", dojo.data.ItemFileReadStore, {
	constructor: function(/* object */ keywordParameters){
		//	keywordParameters: {typeMap: object)
		//		The structure of the typeMap object is as follows:
		//		{
		//			type0: function || object,
		//			type1: function || object,
		//			...
		//			typeN: function || object
		//		}
		//		Where if it is a function, it is assumed to be an object constructor that takes the 
		//		value of _value as the initialization parameters.  It is serialized assuming object.toString()
		//		serialization.  If it is an object, then it is assumed
		//		to be an object of general form:
		//		{
		//			type: function, //constructor.
		//			deserialize:	function(value) //The function that parses the value and constructs the object defined by type appropriately.
		//			serialize:	function(object) //The function that converts the object back into the proper file format form.
		//		}

		// ItemFileWriteStore extends ItemFileReadStore to implement these additional dojo.data APIs
		this._features['dojo.data.api.Write'] = true;
		this._features['dojo.data.api.Notification'] = true;
		
		// For keeping track of changes so that we can implement isDirty and revert
		this._pending = {
			_newItems:{}, 
			_modifiedItems:{}, 
			_deletedItems:{}
		};

		if(!this._datatypeMap['Date'].serialize){
			this._datatypeMap['Date'].serialize = function(obj){
				return dojo.date.stamp.toISOString(obj, {zulu:true});
			}
		}
		
		// this._saveInProgress is set to true, briefly, from when save() is first called to when it completes
		this._saveInProgress = false;
	}, 
	
	_assert: function(/* boolean */ condition){
		if(!condition) {
			throw new Error("assertion failed in ItemFileWriteStore");
		}
	},

	_getIdentifierAttribute: function(){
		var identifierAttribute = this.getFeatures()['dojo.data.api.Identity'];
		// this._assert((identifierAttribute === Number) || (dojo.isString(identifierAttribute)));
		return identifierAttribute;
	},
	
	
/* dojo.data.api.Write */

	newItem: function(/* Object? */ keywordArgs, /* Object? */ parentInfo){
		// summary: See dojo.data.api.Write.newItem()

		this._assert(!this._saveInProgress);

		if (!this._loadFinished){
			// We need to do this here so that we'll be able to find out what
			// identifierAttribute was specified in the data file.
			this._forceLoad();
		}

		if(typeof keywordArgs != "object" && typeof keywordArgs != "undefined"){
			throw new Error("newItem() was passed something other than an object");
		}
		var newIdentity = null;
		var identifierAttribute = this._getIdentifierAttribute();
		if(identifierAttribute === Number){
			newIdentity = this._arrayOfAllItems.length;
		}else{
			newIdentity = keywordArgs[identifierAttribute];
			if (typeof newIdentity === "undefined"){
				throw new Error("newItem() was not passed an identity for the new item");
			}
			if (dojo.isArray(newIdentity)){
				throw new Error("newItem() was not passed an single-valued identity");
			}
		}
		
		// make sure this identity is not already in use by another item, if identifiers were 
		// defined in the file.  Otherwise it would be the item count, 
		// which should always be unique in this case.
		if(this._itemsByIdentity){
			this._assert(typeof this._itemsByIdentity[newIdentity] === "undefined");
		}
		this._assert(typeof this._pending._newItems[newIdentity] === "undefined");
		this._assert(typeof this._pending._deletedItems[newIdentity] === "undefined");
		
		var newItem = {};
		newItem[this._storeRefPropName] = this;		
		newItem[this._itemNumPropName] = this._arrayOfAllItems.length;
		if(this._itemsByIdentity){
			this._itemsByIdentity[newIdentity] = newItem;
		}
		this._arrayOfAllItems.push(newItem);

		//We need to construct some data for the onNew call too...
		var pInfo = null;
		
		// Now we need to check to see where we want to assign this thingm if any.
		if(parentInfo && parentInfo.parent && parentInfo.attribute){
			pInfo = {
				item: parentInfo.parent,
				attribute: parentInfo.attribute,
				oldValue: undefined
			};

			//See if it is multi-valued or not and handle appropriately
			//Generally, all attributes are multi-valued for this store
			//So, we only need to append if there are already values present.
			var values = this.getValues(parentInfo.parent, parentInfo.attribute);
			if(values && values.length > 0){
				var tempValues = values.slice(0, values.length);
				if(values.length === 1){
					pInfo.oldValue = values[0];
				}else{
					pInfo.oldValue = values.slice(0, values.length);
				}
				tempValues.push(newItem);
				this._setValueOrValues(parentInfo.parent, parentInfo.attribute, tempValues, false);
				pInfo.newValue = this.getValues(parentInfo.parent, parentInfo.attribute);
			}else{
				this._setValueOrValues(parentInfo.parent, parentInfo.attribute, newItem, false);
				pInfo.newValue = newItem;
			}
		}else{
			//Toplevel item, add to both top list as well as all list.
			newItem[this._rootItemPropName]=true;
			this._arrayOfTopLevelItems.push(newItem);
		}
		
		this._pending._newItems[newIdentity] = newItem;
		
		//Clone over the properties to the new item
		for(var key in keywordArgs){
			if(key === this._storeRefPropName || key === this._itemNumPropName){
				// Bummer, the user is trying to do something like
				// newItem({_S:"foo"}).  Unfortunately, our superclass,
				// ItemFileReadStore, is already using _S in each of our items
				// to hold private info.  To avoid a naming collision, we 
				// need to move all our private info to some other property 
				// of all the items/objects.  So, we need to iterate over all
				// the items and do something like: 
				//    item.__S = item._S;
				//    item._S = undefined;
				// But first we have to make sure the new "__S" variable is 
				// not in use, which means we have to iterate over all the 
				// items checking for that.
				throw new Error("encountered bug in ItemFileWriteStore.newItem");
			}
			var value = keywordArgs[key];
			if(!dojo.isArray(value)){
				value = [value];
			}
			newItem[key] = value;
		}
		this.onNew(newItem, pInfo); // dojo.data.api.Notification call
		return newItem; // item
	},
	
	_removeArrayElement: function(/* Array */ array, /* anything */ element){
		var index = dojo.indexOf(array, element);
		if (index != -1){
			array.splice(index, 1);
			return true;
		}
		return false;
	},
	
	deleteItem: function(/* item */ item){
		// summary: See dojo.data.api.Write.deleteItem()
		this._assert(!this._saveInProgress);
		this._assertIsItem(item);

		// remove this item from the _arrayOfAllItems, but leave a null value in place
		// of the item, so as not to change the length of the array, so that in newItem() 
		// we can still safely do: newIdentity = this._arrayOfAllItems.length;
		var indexInArrayOfAllItems = item[this._itemNumPropName];
		this._arrayOfAllItems[indexInArrayOfAllItems] = null;
		
		var identity = this.getIdentity(item);
		item[this._storeRefPropName] = null;
		if(this._itemsByIdentity){
			delete this._itemsByIdentity[identity];
		}
		this._pending._deletedItems[identity] = item;
		
		//Remove from the toplevel items, if necessary...
		if(item[this._rootItemPropName]){
			this._removeArrayElement(this._arrayOfTopLevelItems, item);
		}
		this.onDelete(item); // dojo.data.api.Notification call
		return true;
	},

	setValue: function(/* item */ item, /* attribute-name-string */ attribute, /* almost anything */ value){
		// summary: See dojo.data.api.Write.set()
		return this._setValueOrValues(item, attribute, value, true); // boolean
	},
	
	setValues: function(/* item */ item, /* attribute-name-string */ attribute, /* array */ values){
		// summary: See dojo.data.api.Write.setValues()
		return this._setValueOrValues(item, attribute, values, true); // boolean
	},
	
	unsetAttribute: function(/* item */ item, /* attribute-name-string */ attribute){
		// summary: See dojo.data.api.Write.unsetAttribute()
		return this._setValueOrValues(item, attribute, [], true);
	},
	
	_setValueOrValues: function(/* item */ item, /* attribute-name-string */ attribute, /* anything */ newValueOrValues, /*boolean?*/ callOnSet){
		this._assert(!this._saveInProgress);
		
		// Check for valid arguments
		this._assertIsItem(item);
		this._assert(dojo.isString(attribute));
		this._assert(typeof newValueOrValues !== "undefined");

		// Make sure the user isn't trying to change the item's identity
		var identifierAttribute = this._getIdentifierAttribute();
		if(attribute == identifierAttribute){
			throw new Error("ItemFileWriteStore does not have support for changing the value of an item's identifier.");
		}

		// To implement the Notification API, we need to make a note of what
		// the old attribute value was, so that we can pass that info when
		// we call the onSet method.
		var oldValueOrValues = this._getValueOrValues(item, attribute);

		var identity = this.getIdentity(item);
		if(!this._pending._modifiedItems[identity]){
			// Before we actually change the item, we make a copy of it to 
			// record the original state, so that we'll be able to revert if 
			// the revert method gets called.  If the item has already been
			// modified then there's no need to do this now, since we already
			// have a record of the original state.
			var copyOfItemState = {};
			for(var key in item){
				if((key === this._storeRefPropName) || (key === this._itemNumPropName) || (key === this._rootItemPropName)){
					copyOfItemState[key] = item[key];
				}else{
					var valueArray = item[key];
					var copyOfValueArray = [];
					for(var i = 0; i < valueArray.length; ++i){
						copyOfValueArray.push(valueArray[i]);
					}
					copyOfItemState[key] = copyOfValueArray;
				}
			}
			// Now mark the item as dirty, and save the copy of the original state
			this._pending._modifiedItems[identity] = copyOfItemState;
		}
		
		// Okay, now we can actually change this attribute on the item
		var success = false;
		if(dojo.isArray(newValueOrValues) && newValueOrValues.length === 0){
			// If we were passed an empty array as the value, that counts
			// as "unsetting" the attribute, so we need to remove this 
			// attribute from the item.
			success = delete item[attribute];
			newValueOrValues = undefined; // used in the onSet Notification call below
		}else{
			var newValueArray = [];
			if(dojo.isArray(newValueOrValues)){
				var newValues = newValueOrValues;
				// Unforunately, it's not safe to just do this:
				//    newValueArray = newValues;
				// Instead, we need to take each value in the values array and copy 
				// it into the new array, so that our internal data structure won't  
				// get corrupted if the user mucks with the values array *after*
				// calling setValues().
				for(var j = 0; j < newValues.length; ++j){
					newValueArray.push(newValues[j]);
				}
			}else{
				var newValue = newValueOrValues;
				newValueArray.push(newValue);
			}
			item[attribute] = newValueArray;
			success = true;
		}

		// Now we make the dojo.data.api.Notification call
		if(callOnSet){
			this.onSet(item, attribute, oldValueOrValues, newValueOrValues); 
		}
		return success; // boolean
	},

	_getValueOrValues: function(/* item */ item, /* attribute-name-string */ attribute){
		var valueOrValues = undefined;
		if(this.hasAttribute(item, attribute)){
			var valueArray = this.getValues(item, attribute);
			if(valueArray.length == 1){
				valueOrValues = valueArray[0];
			}else{
				valueOrValues = valueArray;
			}
		}
		return valueOrValues;
	},
	
	_flatten: function(/* anything */ value){
		if(this.isItem(value)){
			var item = value;
			// Given an item, return an serializable object that provides a 
			// reference to the item.
			// For example, given kermit:
			//    var kermit = store.newItem({id:2, name:"Kermit"});
			// we want to return
			//    {_reference:2}
			var identity = this.getIdentity(item);
			var referenceObject = {_reference: identity};
			return referenceObject;
		}else{
			if(typeof value === "object"){
				for(type in this._datatypeMap){
					var typeMap = this._datatypeMap[type];
					if (dojo.isObject(typeMap) && !dojo.isFunction(typeMap)){
						if(value instanceof typeMap.type){
							if(!typeMap.serialize){
								throw new Error("ItemFileWriteStore:  No serializer defined for type mapping: [" + type + "]");
							}
							return {_type: type, _value: typeMap.serialize(value)};
						}
					} else if(value instanceof typeMap){
						//SImple mapping, therefore, return as a toString serialization.
						return {_type: type, _value: value.toString()};
					}
				}
			}
			return value;
		}
	},
	
	_getNewFileContentString: function(){
		// summary: 
		//		Generate a string that can be saved to a file.
		//		The result should look similar to:
		//		http://trac.dojotoolkit.org/browser/dojo/trunk/tests/data/countries.json
		var serializableStructure = {};
		
		var identifierAttribute = this._getIdentifierAttribute();
		if(identifierAttribute !== Number){
			serializableStructure.identifier = identifierAttribute;
		}
		if(this._labelAttr){
			serializableStructure.label = this._labelAttr;
		}
		serializableStructure.items = [];
		for(var i = 0; i < this._arrayOfAllItems.length; ++i){
			var item = this._arrayOfAllItems[i];
			if(item !== null){
				serializableItem = {};
				for(var key in item){
					if(key !== this._storeRefPropName && key !== this._itemNumPropName){
						var attribute = key;
						var valueArray = this.getValues(item, attribute);
						if(valueArray.length == 1){
							serializableItem[attribute] = this._flatten(valueArray[0]);
						}else{
							var serializableArray = [];
							for(var j = 0; j < valueArray.length; ++j){
								serializableArray.push(this._flatten(valueArray[j]));
								serializableItem[attribute] = serializableArray;
							}
						}
					}
				}
				serializableStructure.items.push(serializableItem);
			}
		}
		var prettyPrint = true;
		return dojo.toJson(serializableStructure, prettyPrint);
	},
	
	save: function(/* object */ keywordArgs){
		// summary: See dojo.data.api.Write.save()
		this._assert(!this._saveInProgress);
		
		// this._saveInProgress is set to true, briefly, from when save is first called to when it completes
		this._saveInProgress = true;
		
		var self = this;
		var saveCompleteCallback = function(){
			self._pending = {
				_newItems:{}, 
				_modifiedItems:{},
				_deletedItems:{}
			};
			self._saveInProgress = false; // must come after this._pending is cleared, but before any callbacks
			if(keywordArgs && keywordArgs.onComplete){
				var scope = keywordArgs.scope || dojo.global;
				keywordArgs.onComplete.call(scope);
			}
		};
		var saveFailedCallback = function(){
			self._saveInProgress = false;
			if(keywordArgs && keywordArgs.onError){
				var scope = keywordArgs.scope || dojo.global;
				keywordArgs.onError.call(scope);
			}
		};
		
		if(this._saveEverything){
			var newFileContentString = this._getNewFileContentString();
			this._saveEverything(saveCompleteCallback, saveFailedCallback, newFileContentString);
		}
		if(this._saveCustom){
			this._saveCustom(saveCompleteCallback, saveFailedCallback);
		}
		if(!this._saveEverything && !this._saveCustom){
			// Looks like there is no user-defined save-handler function.
			// That's fine, it just means the datastore is acting as a "mock-write"
			// store -- changes get saved in memory but don't get saved to disk.
			saveCompleteCallback();
		}
	},
	
	revert: function(){
		// summary: See dojo.data.api.Write.revert()
		this._assert(!this._saveInProgress);

		var identity;
		for(identity in this._pending._newItems){
			var newItem = this._pending._newItems[identity];
			newItem[this._storeRefPropName] = null;
			// null out the new item, but don't change the array index so
			// so we can keep using _arrayOfAllItems.length.
			this._arrayOfAllItems[newItem[this._itemNumPropName]] = null;
			if(newItem[this._rootItemPropName]){
				this._removeArrayElement(this._arrayOfTopLevelItems, newItem);
			}
			if(this._itemsByIdentity){
				delete this._itemsByIdentity[identity];
			}
		}
		for(identity in this._pending._modifiedItems){
			// find the original item and the modified item that replaced it
			var originalItem = this._pending._modifiedItems[identity];
			var modifiedItem = null;
			if(this._itemsByIdentity){
				modifiedItem = this._itemsByIdentity[identity];
			}else{
				modifiedItem = this._arrayOfAllItems[identity];
			}
			
			// make the original item into a full-fledged item again
			originalItem[this._storeRefPropName] = this;
			modifiedItem[this._storeRefPropName] = null;

			// replace the modified item with the original one
			var arrayIndex = modifiedItem[this._itemNumPropName];
			this._arrayOfAllItems[arrayIndex] = originalItem;
			
			if(modifiedItem[this._rootItemPropName]){
				arrayIndex = modifiedItem[this._itemNumPropName];
				this._arrayOfTopLevelItems[arrayIndex] = originalItem;
			}
			if(this._itemsByIdentity){
				this._itemsByIdentity[identity] = originalItem;
			}			
		}
		for(identity in this._pending._deletedItems){
			var deletedItem = this._pending._deletedItems[identity];
			deletedItem[this._storeRefPropName] = this;
			var index = deletedItem[this._itemNumPropName];
			this._arrayOfAllItems[index] = deletedItem;
			if (this._itemsByIdentity) {
				this._itemsByIdentity[identity] = deletedItem;
			}
			if(deletedItem[this._rootItemPropName]){
				this._arrayOfTopLevelItems.push(deletedItem);
			}
		}
		this._pending = {
			_newItems:{}, 
			_modifiedItems:{}, 
			_deletedItems:{}
		};
		return true; // boolean
	},
	
	isDirty: function(/* item? */ item){
		// summary: See dojo.data.api.Write.isDirty()
		if(item){
			// return true if the item is dirty
			var identity = this.getIdentity(item);
			return new Boolean(this._pending._newItems[identity] || 
				this._pending._modifiedItems[identity] ||
				this._pending._deletedItems[identity]); // boolean
		}else{
			// return true if the store is dirty -- which means return true
			// if there are any new items, dirty items, or modified items
			var key;
			for(key in this._pending._newItems){
				return true;
			}
			for(key in this._pending._modifiedItems){
				return true;
			}
			for(key in this._pending._deletedItems){
				return true;
			}
			return false; // boolean
		}
	},

/* dojo.data.api.Notification */

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

}
