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

describe('The Resource Editor', function() {
	MockNodeTypeManager = (function() {
		function MockNodeTypeManager(settings, ntManager){
			this.settings = settings;
			this.ntManager = ntManager;
			this.nodeTypesJson =  {
				"nt:unstructured": {
				    "declaredChildNodeDefinitions": [{
				        "name": "*",
				        "onParentVersion": "VERSION",
				        "allowsSameNameSiblings": true,
				        "defaultPrimaryType": "nt:unstructured"
				        }],
				      "declaredPropertyDefinitions": [
				        {
				        "name": "*",
				        "requiredType": "undefined",
				        "multiple": true
				        },
				        {
				        "name": "*",
				        "requiredType": "undefined"
				        }
				      ],
				      "orderableChildNodes": true
				},
				"nt:folder": {
		    	    "declaredChildNodeDefinitions": [{
		    	      "name": "*",
		    	      "onParentVersion": "VERSION",
		    	      "requiredPrimaryTypes": ["nt:hierarchyNode"]
		    	      }],
		    	    "declaredSupertypes": ["nt:hierarchyNode"]
		    	},
		    	"nt:hierarchyNode": {"declaredSupertypes": [
	                "mix:created",
	                "nt:base"
                ]},
		    	"nt:base": {
		    	}
			}
		}
	
		MockNodeTypeManager.prototype.getNodeTypeNames = function() {
			return ["nt:unstructured", "nt:folder", "nt:hierarchyNode", "nt:base"];
		}
		
		MockNodeTypeManager.prototype.getNodeType = function(name) {
			var nt = this.nodeTypesJson[name];
			if (typeof nt.name === "undefined"){
				nt.name = name;
				nt.canAddProperty = function(propertyName, propertyType){
					return true;
				};
			}
			return nt;
		}
		
		return MockNodeTypeManager;
	}());
	
	var mockNtManager = new MockNodeTypeManager();

	var mainControllerSettings = {
		contextPath: "/",
		nodeTypes: mockNtManager.getNodeTypeNames() 
	};
	
	describe('\'s MainController', function() {
		var mainController;

		beforeEach(function() {
			mainController = new org.apache.sling.reseditor.MainController(mainControllerSettings, mockNtManager);
		});

		it('can encode a URL', function() {
			var urlToEncode = "/reseditor/testnode/$&?äöß<> test.html";
			expect(mainController.encodeURL(urlToEncode)).toEqual("/reseditor/testnode/%24%26%3F%C3%A4%C3%B6%C3%9F%3C%3E%20test.html");
		});

		it('can encode HTML', function() {
			expect(mainController.encodeToHTML("a<>b")).toEqual("a&lt;&gt;b");
		});

		it('can dencode HTML', function() {
			expect(mainController.decodeFromHTML("a&lt;&gt;b")).toEqual("a<>b");
		});
		
	});
	
	describe('\'s TreeController', function() {
		var treeControllerSettings = {
			contextPath: "/",
			nodeTypes: mockNtManager.getNodeTypeNames() 
		};

		var mainController;
		var treeController;

		beforeEach(function() {
			mainController = new org.apache.sling.reseditor.MainController(mainControllerSettings, mockNtManager);
			treeController = new org.apache.sling.reseditor.TreeController(treeControllerSettings, mainController);
		});
		  
		it("can rename nodes", function() {
			var htmlEncodedNewName = "newNodeName";
			var data = {
					text: htmlEncodedNewName,
					old: "oldNodeName",
					node: {id: "li_id"}
			};
			spyOn(treeController, "getPathFromLi").and.returnValue("/testnode/oldNodeName");
			spyOn($, "ajax");
			treeController.renameNode(null, data);
			
			expect($.ajax.calls.mostRecent().args[0]["url"]).toEqual("/testnode/oldNodeName");
			expect($.ajax.calls.mostRecent().args[0]["data"][":dest"]).toEqual("/testnode/newNodeName");
			expect($.ajax.calls.mostRecent().args[0]["data"][":operation"]).toEqual("move");
			expect($.ajax.calls.mostRecent().args[0]["data"]["_charset_"]).toEqual("utf-8");
		});
		
		it("can split the URL into path elements for deep links", function(){
			var pathElements = treeController.getPathElements("/");
			expect(pathElements.length).toBe(1);
			expect(pathElements).toContain("");
			
			pathElements = treeController.getPathElements("/testnodes");
			expect(pathElements.length).toBe(1);
			expect(pathElements).toContain("testnodes");
			
			pathElements = treeController.getPathElements("/testnodes/level2/level3");
			expect(pathElements.length).toBe(3);
			expect(pathElements[0]).toBe("testnodes");
			expect(pathElements[1]).toBe("level2");
			expect(pathElements[2]).toBe("level3");
			
			// Sometimes the path contains the ".html" suffix. Check that it still works 
			pathElements = treeController.getPathElements("/.html");
			expect(pathElements.length).toBe(1);
			expect(pathElements).toContain("");
			
			pathElements = treeController.getPathElements("/testnodes.html");
			expect(pathElements.length).toBe(1);
			expect(pathElements).toContain("testnodes");
			
			pathElements = treeController.getPathElements("/testnodes/level2/level3.html");
			expect(pathElements.length).toBe(3);
			expect(pathElements[0]).toBe("testnodes");
			expect(pathElements[1]).toBe("level2");
			expect(pathElements[2]).toBe("level3");
		});

		it("can delete nodes", function(){
			spyOn(treeController, "getURLEncodedPathFromLi").and.returnValue("/testnode");
			spyOn($.fn, "jstree").and.returnValue(["j1_18", "j1_20"]);
			var getPathFromLiCount = 0;
			spyOn(treeController,"getPathFromLi").and.callFake(function(){
				getPathFromLiCount++;
				switch(getPathFromLiCount) {
				    case 1:
				        return "/testnode/node1_to_delete";
				    case 2:
				        return "/testnode/node3_to_delete";
				}
			});
			
			spyOn(bootbox, "confirm").and.callFake(function(){
				var result = true;
				/*
				 * The confirm dialog cannot be clicked in this test. We spy on this dialog, retrieve the function 
				 * that gets called after the user clicked ok and then call this function to check our expectations.
				 */
				// calling 'sendDeletePost()'
				bootbox.confirm.calls.argsFor(0)[1](result);
			});
			
			spyOn($, "ajax");
			
			treeController.deleteNodes({});
			
			expect(treeController.getPathFromLi.calls.count()).toBe(2);

			expect($.ajax.calls.mostRecent().args[0]["url"]).toEqual("/testnode");
			expect($.ajax.calls.mostRecent().args[0]["data"][":operation"]).toEqual("delete");
			expect($.ajax.calls.mostRecent().args[0]["data"]["_charset_"]).toEqual("utf-8");
			expect($.ajax.calls.mostRecent().args[0]["data"][":applyTo"][0]).toEqual("/testnode/node1_to_delete");
			expect($.ajax.calls.mostRecent().args[0]["data"][":applyTo"][1]).toEqual("/testnode/node3_to_delete");
		});
		
		it("can delete a single node", function(){
			spyOn(treeController,"getPathFromLi").and.returnValue("/testnode/node2delete");

			spyOn(bootbox, "confirm").and.callFake(function(){
				var result = true;
				/*
				 * The confirm dialog cannot be clicked in this test. We spy on this dialog, retrieve the function 
				 * that gets called after the user clicked ok and then call this function to check our expectations.
				 */
				// calling 'sendDeletePost()'
				bootbox.confirm.calls.argsFor(0)[1](result);
			});

			spyOn($, "ajax");
			
			treeController.deleteSingleNode({});

			expect($.ajax.calls.mostRecent().args[0]["url"]).toEqual("/testnode/node2delete");
			expect($.ajax.calls.mostRecent().args[0]["data"][":operation"]).toEqual("delete");
		});
	});
	
	describe('\'s AddNodeController', function() {
		var mainController;
		var addNodeController;

		beforeEach(function() {
			mainController = new org.apache.sling.reseditor.MainController(mainControllerSettings, mockNtManager);
			addNodeController = new org.apache.sling.reseditor.AddNodeController({}, mainController);
		});
		
		it("can add a node", function(){
			spyOn(mainController, "encodeURL").and.returnValue("/testnode");

			spyOn($.fn, "select2").and.returnValue("nt:unstructured");

			spyOn($, 'ajax').and.callFake(function (req) {
			    var d = $.Deferred();
			    d.resolve({});
			    return d.promise();
			});
			
			spyOn(mainController,"redirectTo").and.returnValue(null);
			addNodeController.nodeNameSubmitable=true;
			addNodeController.resourceTypeSubmitable=true;

			addNodeController.addNode();
			
			expect($.ajax.calls.mostRecent().args[0]["url"]).toEqual("/testnode");
			expect($.ajax.calls.mostRecent().args[0]["data"]["_charset_"]).toEqual("utf-8");
			expect($.ajax.calls.mostRecent().args[0]["data"]["jcr:primaryType"]).toEqual("nt:unstructured");
		});
	});
		
		
});