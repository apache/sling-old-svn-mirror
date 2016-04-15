/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// creating the namespace
var org = org || {};
org.apache = org.apache || {};
org.apache.sling = org.apache.sling || {};
org.apache.sling.jcr = org.apache.sling.jcr || {};
org.apache.sling.jcr.js = org.apache.sling.jcr.js || {};
org.apache.sling.jcr.js.nodetypes = org.apache.sling.jcr.js.nodetypes || {};

/*
 JSNodeTypes - The JavaScript Node Types library for Apache Sling

 The documentation of the library can be found at:
 http://www.jcrbrowser.org/sling/libs/jsnodetypes/content/documentation.html
 
*/

//defining the module
org.apache.sling.jcr.js.nodetypes.NodeTypeManager = (function() {

	function NodeTypeManager(settingsParameter){
		// copies the setting parameters to the object scope and configures the defaults
		
		var noSettingsProvided = typeof settingsParameter === 'undefined' || settingsParameter == null;
		var contextPath = (noSettingsProvided || typeof settingsParameter.contextPath === 'undefined') ? '' : settingsParameter.contextPath;
		var defaultNTJsonURL = (noSettingsProvided || typeof settingsParameter.defaultNTJsonURL === 'undefined') ? contextPath+'/libs/jsnodetypes/js/defaultNT/defaultNT.json' : settingsParameter.defaultNTJsonURL;
		this.defaultNTJson = getJson(defaultNTJsonURL);
		this.nodeTypesJson = (noSettingsProvided || typeof settingsParameter.nodeTypesJson === 'undefined') ? getJson(contextPath+'/libs/jsnodetypes/content/nodetypes.json') : settingsParameter.nodeTypesJson;
		initializeNodeTypes(this);
	};
	
	function getJson(url){
		var result;
		var xhr = null;
	    if (window.XMLHttpRequest) {
	    	xhr = new XMLHttpRequest();
	    } else if (window.ActiveXObject) { // Older IE.
	    	xhr = new ActiveXObject("MSXML2.XMLHTTP.3.0");
	    }
		xhr.open("GET", url, false/*not async*/);
		if (typeof xhr.overrideMimeType != "undefined"){
			xhr.overrideMimeType("application/json");
		}
		xhr.onload = function (e) {
		  if (xhr.readyState === 4) {
		    if (xhr.status === 200) {
		    	result = JSON.parse(xhr.responseText);
		    } else {
		    	console.error(xhr.statusText);
		    }
		  }
		};
		xhr.onerror = function (e) {
		  console.error(xhr.statusText);
		};
		xhr.send(null);
		return result;
	}
	
	/* adding an indexOf function if it's not available */
	if (!Array.prototype.indexOf) {
	    Array.prototype.indexOf = function (searchElement /*, fromIndex */ ) {
	        "use strict";
	        if (this == null) {
	            throw new TypeError();
	        }
	        var t = Object(this);
	        var len = t.length >>> 0;
	        if (len === 0) {
	            return -1;
	        }
	        var n = 0;
	        if (arguments.length > 1) {
	            n = Number(arguments[1]);
	            if (n != n) { // shortcut for verifying if it's NaN
	                n = 0;
	            } else if (n != 0 && n != Infinity && n != -Infinity) {
	                n = (n > 0 || -1) * Math.floor(Math.abs(n));
	            }
	        }
	        if (n >= len) {
	            return -1;
	        }
	        var k = n >= 0 ? n : Math.max(len - Math.abs(n), 0);
	        for (; k < len; k++) {
	            if (k in t && t[k] === searchElement) {
	                return k;
	            }
	        }
	        return -1;
	    }
	}
	
	/*
	 * Adds Object.keys if its not available.
	 * See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
	 */
	if (!Object.keys) {
		 Object.keys = (function() {
			 'use strict';
			 var hasOwnProperty = Object.prototype.hasOwnProperty,
			    hasDontEnumBug = !({ toString: null }).propertyIsEnumerable('toString'),
			    dontEnums = [
			      'toString',
			      'toLocaleString',
			      'valueOf',
			      'hasOwnProperty',
			      'isPrototypeOf',
			      'propertyIsEnumerable',
			      'constructor'
			    ],
			    dontEnumsLength = dontEnums.length;

			return function(obj) {
			  if (typeof obj !== 'object' && (typeof obj !== 'function' || obj === null)) {
			    throw new TypeError('Object.keys called on non-object');
			  }

			  var result = [], prop, i;

			  for (prop in obj) {
			    if (hasOwnProperty.call(obj, prop)) {
			      result.push(prop);
			    }
			  }

			  if (hasDontEnumBug) {
			    for (i = 0; i < dontEnumsLength; i++) {
			      if (hasOwnProperty.call(obj, dontEnums[i])) {
			        result.push(dontEnums[i]);
			      }
			    }
			  }
			  return result;
			};
		}()); 
	}

	/*
	 * This function walks recursively through all parent node types and calls the processing function with the current node type
	 *  
	 * currentNodeType - the node type to retrieve the property defs from in this call 
	 * processingFunction - the function to call on every node type
	 * processedNodeTypes - is used to avoid cycles by checking if a node type has been processed already
	 * iterationProperty - the property of the nodeType that should be used for iteration e.g. 'declaredSupertypes'
	 */
	function processNodeTypeGraph (currentNodeType, iterationProperty, processingFunction, processedNodeTypes){
		if (currentNodeType == null || iterationProperty == null || iterationProperty==="" || processingFunction == null ) return;
		var initialCall = typeof processedNodeTypes === 'undefined';
		if (initialCall){
			processedNodeTypes = [];
		}
		
		processingFunction(currentNodeType);

		processedNodeTypes.push(currentNodeType.name);
		
		for (var supertypeIndex in currentNodeType[iterationProperty]) {
			newNodeTypeName = currentNodeType[iterationProperty][supertypeIndex];
			
			newNodeType = this.getNodeType(newNodeTypeName);
			
			/* 
			 * skip the processing of node types that have already been processed
			 */
			var notProcessedYet = processedNodeTypes.indexOf(newNodeTypeName) < 0;
			if (notProcessedYet){
				processNodeTypeGraph.call(this, newNodeType, iterationProperty, processingFunction, processedNodeTypes);
			}
		}
	};
	
	/*
	 * Sets the value of all properties of defaultNT.json to the corresponding undefined properties of the specified node type.
	 * E.g. if nt.declaredChildNodeDefinitions[2].allowsSameNameSiblings is undefined it is set to testNodeType.declaredChildNodeDefinitions[0].allowsSameNameSiblings
	 */
	function setDefaults(nt){

		if(typeof nt["declaredSupertypes"] === "undefined" && "nt:base" != nt.name){
			nt["declaredSupertypes"] = this.defaultNTJson["declaredSupertypes"]; 
		}
		
		// node type defaults
		for(var propName in this.defaultNTJson){
			if (propName != "declaredPropertyDefinitions" && propName != "declaredChildNodeDefinitions"){
				setDefaultNTProps.call(this, propName);
			}
		}
		
		// property definition defaults
		for(var propName in this.defaultNTJson.declaredPropertyDefinitions[0]){
			/*
			 * Sets the default values from all this.defaultNTJson.declaredPropertyDefinitions[0] properties
			 * too all properties of all declaredPropertyDefinitions of 'nt'.
			 */
			for (var propDefIndex in nt.declaredPropertyDefinitions){
				setDefaultPropDefProps.call(this, propDefIndex, propName);
			}
		}
		// child node definition defaults	
		for(var propName in this.defaultNTJson.declaredChildNodeDefinitions[0]){
			/*
			 * Sets the default values from all this.defaultNTJson.declaredChildNodeDefinitions[0] properties
			 * too all properties of all declaredChildNodeDefinitions of 'nt'.
			 */
			for (var childNodeDefIndex in nt.declaredChildNodeDefinitions){
				setDefaultChildNodeDefProps.call(this, childNodeDefIndex, propName);
			}
		}
		
		function setDefaultNTProps(propName){
			if(typeof nt[propName] === "undefined") nt[propName] = this.defaultNTJson[propName]; 
		}
		
		function setDefaultPropDefProps(index, propName){
			if(typeof nt.declaredPropertyDefinitions[index][propName] === "undefined") nt.declaredPropertyDefinitions[index][propName] = this.defaultNTJson.declaredPropertyDefinitions[0][propName]; 
		}
		
		function setDefaultChildNodeDefProps(index, propName){
			if(typeof nt.declaredChildNodeDefinitions[index][propName] === "undefined") nt.declaredChildNodeDefinitions[index][propName] = this.defaultNTJson.declaredChildNodeDefinitions[0][propName]; 
		}
		
	};
	
	NodeTypeManager.prototype.internalGetDefaultNodeType = function() {
		return this.defaultNTJson;
	};
	
	NodeTypeManager.prototype.getNodeTypeNames = function(name) {
		var ntNames = [];
		for (var ntJson in this.nodeTypesJson) {
			ntNames.push(ntJson);
		}
		return ntNames;
	}
		
	NodeTypeManager.prototype.getNodeType = function(name) {
		return this.nodeTypesJson[name];
	}
	
	function initializeNodeTypes(that){
		try {
			for (var ntIndex in Object.keys(that.nodeTypesJson)){
				var nodeTypeName = Object.keys(that.nodeTypesJson)[ntIndex];
				
				if (typeof that.nodeTypesJson[nodeTypeName] != "undefined") {
					that.nodeTypesJson[nodeTypeName].name = nodeTypeName;
		
					/*
					 * Returns the child node definitions of the node type and those of all inherited node types.
					 * Definitions with the same child node name are returned if they differ in any other attribute.
					 */
					that.nodeTypesJson[nodeTypeName].getAllChildNodeDefinitions = function(){
						var allCollectedChildNodeDefs = [];
						var allCollectedChildNodeDefHashes = [];
						processNodeTypeGraph.call(that, this, 'declaredSupertypes', function(currentNodeType){
							if (currentNodeType.declaredChildNodeDefinitions == null) return;
							for (var childNodeDefIndex in currentNodeType.declaredChildNodeDefinitions) {
								var childNodeDef = currentNodeType.declaredChildNodeDefinitions[childNodeDefIndex];
								var childNodeDefName = childNodeDef.name;
								var hashCode = childNodeDef.hashCode();
								// in case the child has the same child node definition as its parent (supertype)
								var processed = allCollectedChildNodeDefHashes.indexOf(hashCode) >= 0;
								if (!processed){
									allCollectedChildNodeDefHashes.push(hashCode);
									allCollectedChildNodeDefs.push(childNodeDef);
								}
							}
						}); 
						return allCollectedChildNodeDefs;
					};
					/*
					 * Returns the property definitions of the node type and those of all inherited node types.
					 * Definitions with the same property name are returned if they differ in any other attribute. 
					 */
					that.nodeTypesJson[nodeTypeName].getAllPropertyDefinitions = function(){
						var allCollectedPropertyDefs = [];
						var allCollectedPropertyDefHashes = [];
						processNodeTypeGraph.call(that, this, 'declaredSupertypes', function(currentNodeType){
							if (currentNodeType.declaredPropertyDefinitions == null) return;
							for (var propertyDefIndex in currentNodeType.declaredPropertyDefinitions) {
								var propertyDef = currentNodeType.declaredPropertyDefinitions[propertyDefIndex];
								var propertyDefName = propertyDef.name;
								var hashCode = propertyDef.hashCode();
								// in case the child has the same property definition as its parent (supertype)
								var processed = allCollectedPropertyDefHashes.indexOf(hashCode) >= 0;
								if (!processed){
									allCollectedPropertyDefHashes.push(hashCode);
									allCollectedPropertyDefs.push(propertyDef);
								}
							}
						});
						return allCollectedPropertyDefs;
					};
		
					/*
					 * Returns `true` if a node with the specified node name and node type can be added as a child node of the current node type. 
					 * 
					 * The first parameter is the string of the node name and 
					 * the second parameter is a node type object (not a string).
					 */
					that.nodeTypesJson[nodeTypeName].canAddChildNode = function(nodeName, nodeTypeToAdd){
						if (nodeName==null || nodeTypeToAdd==null) return false;
						var allChildNodeDefNames = [];
						processApplicableChildNodeTypes(that, this, function(cnDef, nodeTypeName){
							if (cnDef.name === nodeName) {
								allChildNodeDefNames.push(nodeName);
							}
						});
						var canAddChildNode = false;
						processApplicableChildNodeTypes(that, this, function(cnDef, nodeTypeName){
							var noNonRisidualWithThatName = allChildNodeDefNames.indexOf(nodeName)<0;
							var nodeNameMatches = (nodeName === cnDef.name) || ("*" === cnDef.name && noNonRisidualWithThatName); 
							var canAddToCurrentCnDef = !cnDef.protected && nodeNameMatches && nodeTypeToAdd.name === nodeTypeName;
							canAddChildNode = canAddChildNode || canAddToCurrentCnDef; 
						});
						return canAddChildNode;
					}
		
					/*
					 * Returns `true` if a property with the specified name and type can be to the current node type. 
					 * 
					 * The first parameter is the string of the property name and 
					 * the second parameter is the property type (case insensitive).
					 */
					that.nodeTypesJson[nodeTypeName].canAddProperty = function(propertyName, propertyType){
						if (propertyName == null || propertyType == null) return false;
						var allPropertyDefNames = [];
						processNodeTypeGraph.call(that, this, 'declaredSupertypes', function(currentNodeType){
							if (currentNodeType.declaredPropertyDefinitions == null) return;
						    for (var propDefIndex in currentNodeType.declaredPropertyDefinitions) {
								var propDef = currentNodeType.declaredPropertyDefinitions[propDefIndex];
								allPropertyDefNames.push(propDef.name);
						    }
						}); 
						var canAddProperty = false;
						processNodeTypeGraph.call(that, this, 'declaredSupertypes', function(currentNodeType){
							if (currentNodeType.declaredPropertyDefinitions == null) return;
						    for (var propDefIndex=0; canAddProperty === false && propDefIndex < currentNodeType.declaredPropertyDefinitions.length; propDefIndex++) {
								var propDef = currentNodeType.declaredPropertyDefinitions[propDefIndex];
								var noNonRisidualWithThatName = allPropertyDefNames.indexOf(propertyName)<0;
								var namesMatch = propDef.name === propertyName || ("*" === propDef.name && noNonRisidualWithThatName);
								var typesMatch = propDef.requiredType.toLowerCase() === propertyType.toLowerCase() || "undefined" === propDef.requiredType;
								var isNotProtected = !propDef.protected;
								canAddProperty = namesMatch && typesMatch && isNotProtected; 
						    }
						}); 
						return canAddProperty;
					};
					
					/*
					 * Returns all node types that can be used for child nodes of this node type and its super types.
					 * If a child node definition specifies multiple required primary types an applicable node type has
					 * to be a subtype of all of them. 
					 * The parameter is a boolean that specifies if mixins should be included or not. If no parameter is passed 'true' is assumed and mixins are
					 * returned as well.
					 */
					that.nodeTypesJson[nodeTypeName].getApplicableCnTypesPerCnDef = function(includeMixins){
						var allApplChildNodeTypes = {};
						processApplicableChildNodeTypes(that, this, function(cnDef, nodeTypeName){
							var nodeType = that.getNodeType(nodeTypeName);
							if (typeof allApplChildNodeTypes[cnDef.name] === "undefined") {
								allApplChildNodeTypes[cnDef.name] = {};
							}
							var includeAlsoMixins = typeof includeMixins === "undefined" || includeMixins;
							if (nodeType.mixin === true && includeAlsoMixins || !nodeType.mixin){
								allApplChildNodeTypes[cnDef.name][nodeTypeName] = that.getNodeType(nodeTypeName);
							}
						});
						return allApplChildNodeTypes;
					}
				};
				
				setDefaults.call(that, that.nodeTypesJson[nodeTypeName]);
				initializeChildNodeDefs.call(that);
				initializePropertyDefs.call(that);
			}
		} catch (e){
			console.log("Error, the node types JSON is not an object"); 
		};
		function itemHashCode(item){
			var hash = "";
			hash += item.name;
			hash += item.autoCreated;
			hash += item.mandatory;
			hash += item.protected;
			hash += item.onParentVersion;
			return hash;
		}

		function initializeChildNodeDefs(){
			for (var childNodeDefIndex in that.nodeTypesJson[nodeTypeName].declaredChildNodeDefinitions) {
				var childNodeDef = that.nodeTypesJson[nodeTypeName].declaredChildNodeDefinitions[childNodeDefIndex];
				
				childNodeDef.hashCode = function (){
					var hashCode = itemHashCode(this);
					hashCode += this.allowsSameNameSiblings;
					hashCode += this.defaultPrimaryType;
					for (var reqPtIndex in this.requiredPrimaryTypes) {
						hashCode += this.requiredPrimaryTypes[reqPtIndex];
					}
					return hashCode;
				}
			}
		}

		function initializePropertyDefs(){
			for (var propertyIndex in that.nodeTypesJson[nodeTypeName].declaredPropertyDefinitions) {
				var propertyDef = that.nodeTypesJson[nodeTypeName	].declaredPropertyDefinitions[propertyIndex];
				
				propertyDef.hashCode = function (){
					var hashCode = itemHashCode(this);
					for (var defaultValueIndex in this.defaultValues) {
						hashCode += this.defaultValues[defaultValueIndex];
					}
					for (var valueConstraintIndex in this.valueConstraints) {
						hashCode += this.valueConstraints[valueConstraintIndex];
					}
					hashCode += this.requiredType;
					hashCode += this.multiple;
					return hashCode;
				}
			}
		}
		
		/*
		 * Navigates to every node types' supertype and sets the subtype property there.
		 */
		function addSubtypeRelation(nodeTypesJson){
			for (var nodeTypeName in nodeTypesJson) {
				nodeTypeJson = nodeTypesJson[nodeTypeName];
				for (var supertypeIndex in nodeTypeJson.declaredSupertypes) {
					var supertypeName = nodeTypeJson.declaredSupertypes[supertypeIndex];
					var supertype = this.getNodeType(supertypeName);
					if (typeof supertype.subtypes === "undefined") {
						supertype.subtypes = [];
					}
					supertype.subtypes.push(nodeTypeName);
				}
			}
			return nodeTypesJson;
		}
		addSubtypeRelation.call(that, that.nodeTypesJson);
	}

	
	function processApplicableChildNodeTypes(ntManager, nodeType, functionToCall){
		var cnDefs = nodeType.getAllChildNodeDefinitions();
		for (var cnDefIndex in cnDefs) {
			var cnDef = cnDefs[cnDefIndex];
			var nodeTypesPerChildNodeDef = {};
			var reqChildNodeTypes = cnDef.requiredPrimaryTypes;
			for (var reqChildNodeTypeIndex in reqChildNodeTypes) {
				var childNodeTypeName = reqChildNodeTypes[reqChildNodeTypeIndex];
				var childNodeType = ntManager.getNodeType(childNodeTypeName);
				/* 
				 * calls the function for every subtype of 'childNodeType' but skips
				 * node types that have already been processed (e.g. because of cycles)
				 */
				processNodeTypeGraph.call(ntManager, childNodeType, 'subtypes', function(currentNodeType){
					if (currentNodeType != null) {
						// if 'true' the type has not yet been found in _one_of_the_ required primary type's subtype tree
						// of the child node definition
						var cnDefWithTypeNotYetProcessed = typeof nodeTypesPerChildNodeDef[currentNodeType.name] === "undefined";
						// This increments the occurency count of a node type in the subtype hierarchy of a required primary type. 
						nodeTypesPerChildNodeDef[currentNodeType.name] = cnDefWithTypeNotYetProcessed ? 1 : nodeTypesPerChildNodeDef[currentNodeType.name]+1; 
					}
				}); 
				
			}
			for (var keyIndex in Object.keys(nodeTypesPerChildNodeDef)){
				var nodeTypeName = Object.keys(nodeTypesPerChildNodeDef)[keyIndex];
				/* 
				 * If the type has been found in all iterations of the required primary types it means it is a subtype
				 * of all of them and can be used for the child node definition.
				 */
				var nodeTypeCountInThisChildNodeDef = nodeTypesPerChildNodeDef[nodeTypeName];
				var subtypeOfAllReqPrimaryTypes = nodeTypeCountInThisChildNodeDef === cnDef.requiredPrimaryTypes.length;
				if (subtypeOfAllReqPrimaryTypes){
					functionToCall(cnDef, nodeTypeName);
				}
			}
		}
	}
	
	return NodeTypeManager;
}());