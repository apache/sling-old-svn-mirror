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

'use strict';

var assert = require('assert');

describe('A user of the Apache Sling Resource Editor', function() { 
	// see http://stackoverflow.com/questions/29370075/how-to-pass-parameters-from-the-gruntfile-js-to-the-webdriverio-spec
	var homeURL = 'http://'+process.env.SLING_SERVER+':'+process.env.SLING_PORT+'/reseditor/.html';
	var client = browser.url(homeURL);

	client.timeouts("script", 1000);
	client.timeouts("implicit", 1000);
	client.timeouts("page load", 1000);

	describe('can open the add node dialog with', function() {
		  it('the icon', function(done) {
			  client.url(homeURL).waitForExist('#last-element').
			  click('#root_anchor i.add-icon').waitForVisible('#addNodeDialog', function(err, visible) {
	    		  assert(typeof err === "undefined" || err === null);
	    		  assert(true === visible);
		      })
		      .call(done);
		  });

		  it('the shortcut', function(done) {
			   client.url(homeURL).waitForExist('#last-element')
			  .click('#root_anchor i.jstree-themeicon')
			  .keys("c").waitForVisible('#addNodeDialog', function(err, visible) {
	    		  assert(typeof err === "undefined" || err === null);
	    		  assert(true === visible);
		      })
		      .call(done);
		  });
	});

	  it('can login as admin', function(done) {
		  client.url(homeURL).waitForExist('#last-element')
		  .click('#login_tab').waitForVisible('#login_submit')
		  .setValue('#login_form input[name="j_username"]', 'admin')
		  .setValue('#login_form input[name="j_password"]', 'admin')
		  .click('#login_submit').waitForExist('#login .logout')
		  .getText('#login_tab', function(err, text) {
    		  assert(typeof err === "undefined" || err === null);
			  assert("Logout admin" === text);
	      })
	      .call(done);
	  });
	  
	  it('can add an unstructured node to the root node', function(done) {
		  client.url(homeURL).waitForExist('#last-element')
		  .click("#root i.add-icon").waitForVisible('#addNodeDialog.add-node-finished .node_name_dd_container input')
		  .setValue('.node_name_dd_container input', "aTestNode")
		  .addValue('.node_name_dd_container input', 'Return') // presses the 'return' key
		  .click('#addNodeDialog .btn.btn-primary.submit').waitForExist("#root li[nodename=\"aTestNode\"]", function(err, existed) {
			  assert(typeof err === "undefined" || err === null);
			  assert(existed === true);
	      })
	      .call(done);
	  });
	  
	 it('can add a node with an encoded name on the second level ', function(done) {
		  client.url(homeURL).waitForExist('#last-element')
		  .click("#root li[nodename=\"aTestNode\"] i.add-icon").waitForVisible('#addNodeDialog.add-node-finished .node_name_dd_container input')
		  .setValue('.node_name_dd_container input', "täßt ?<>")
		  .addValue('.node_name_dd_container input', 'Return') // presses the 'return' key
		  .click('#addNodeDialog .btn.btn-primary.submit').waitForExist("#root  li[nodename=\"aTestNode\"].opened li[nodename=\"täßt ?&lt;&gt;\"]", function(err, existed) {
			  assert(typeof err === "undefined" || err === null);
			  assert(existed === true);
	      })
	      .call(done); 
	  });
	 
	  it('can link to a node with an encoded name', function(done) {
		  var encodedNodeNameSelector = '#root li[nodename="aTestNode"].opened li[nodename="täßt ?&lt;&gt;"]';
		  var encodedNodeNameOpenSelector = encodedNodeNameSelector +' i.open-icon';
		  client.url(homeURL).waitForExist('#last-element')
		  .click("#root li[nodename=\"aTestNode\"] i.jstree-ocl").waitForExist(encodedNodeNameOpenSelector)
		  .click(encodedNodeNameOpenSelector).waitForExist(encodedNodeNameSelector+' a.jstree-clicked', function(err, existed) {
			  assert(typeof err === "undefined" || err === null);
			  assert(existed === true);
	      })
	      .call(done);
	  });
	  

	  it('can add a node with a specific node type ', function(done) {
		  client.url(homeURL);
		  client.waitForExist('#last-element').click("#root li[nodename=\"aTestNode\"] i.add-icon").waitForVisible('#addNodeDialog.add-node-finished .node_name_dd_container', 1000)
		  .setValue('.node_name_dd_container input', "aFolder")
		  .addValue('.node_name_dd_container input', 'Return')
		  .click(".form-group.node-type .select2-chosen").waitForExist('.node_type_dd_container input')
		  .setValue('.node_type_dd_container input', "sling:Folder")
		  .addValue('.node_type_dd_container input', 'Return')
		  .click('#addNodeDialog .btn.btn-primary.submit').waitForExist('#root  li[nodename="aTestNode"] li[nodename="aFolder"][nodetype="sling:Folder"]', function(err, existed) {
			  assert(typeof err === "undefined" || err === null);
			  assert(existed === true);
		  })
	      .call(done);
	  });
	  
	  it('can add a node with a resource type on the second level ', function(done) {
		  var nodeName =  "a node with a resource type";
		  var resourceType =  "test/resource-editor/resource-type";
		  var resourceTypeSelector = '#root  li[nodename="aTestNode"].opened li[nodename="a node with a resource type"] span.node-type';
		  client.url(homeURL);
		  client.waitForExist('#last-element').click("#root li[nodename=\"aTestNode\"] i.add-icon")
		  .waitForVisible('#addNodeDialog.add-node-finished .node_name_dd_container', 1000)
		  .setValue('.node_name_dd_container input', nodeName)
		  .addValue('.node_name_dd_container input', 'Return') // presses the 'return' key
		  .click(".form-group.resource-type .select2-chosen").waitForExist('.resource_type_dd_container input', 1000)
		  .setValue('.resource_type_dd_container input', resourceType)
		  .addValue('.resource_type_dd_container input', 'Return') // presses the 'return' key
		  .click('#addNodeDialog .btn.btn-primary.submit').waitForExist(resourceTypeSelector, 1000)
		  .getText(resourceTypeSelector, function(err, text) {
			  assert(typeof err === "undefined" || err === null);
			  assert(text === resourceType);
		  })
	      .call(done);
	  });
	  
	  it('can rename a node with an encoded name', function(done) {
		  client = client.url(homeURL);
		  client.waitForExist('#last-element', function(err) {
			  client.click("#root li[nodename=\"aTestNode\"] i.jstree-ocl", function(err, res) {
				  var encodedNodeNameSelector = '#root li[nodename="aTestNode"].opened li[nodename="täßt ?&lt;&gt;"]';
				  var encodedNodeNameAnchorSelector = encodedNodeNameSelector +' a .node-type';
// 				  The open node animation will take longer than 500ms thus setting 2000ms as max.
				  client.waitForExist(encodedNodeNameAnchorSelector, 2000, function(err, existed) {
					  client.doubleClick(encodedNodeNameAnchorSelector, function(err) {
						  var encodedNodeNameInputSelector = encodedNodeNameSelector+' input.jstree-rename-input';
						  client.waitForExist(encodedNodeNameInputSelector, function(err, existed) {
							  client.setValue(encodedNodeNameInputSelector, 'täßt2& <>');
							  client.execute(function() {
								  $('#root li[nodename="aTestNode"] li[nodename="täßt ?&lt;&gt;"] input.jstree-rename-input').blur();
								  $('#root li[nodename="aTestNode"] li[nodename="täßt ?&lt;&gt;"] input.jstree-rename-input').blur();
								});
							  client.waitForExist('#root li[nodename="aTestNode"].opened li[nodename="täßt2&amp; &lt;&gt;"]', 2000, function(err, existed) {
								  assert(typeof err === "undefined" || err === null);
								  assert(true === existed);
								  
							  });
						  });
					  });
			      })  
		      })  
	      })
	      .call(done);
	  });
	  it('can delete nodes via multi selection and shortcut', function(done) {
		  client = client.url(homeURL);
		  client
		  .waitForExist('#last-element').click("#root li[nodename=\"aTestNode\"] i.add-icon")
		  	.waitForVisible('#addNodeDialog.add-node-finished', 1000).click('#addNodeDialog .btn.btn-primary.submit')
		  	// The open node animation will take longer than 500ms thus setting 2000ms as max.
		  	.waitForExist('#root li[nodename="aTestNode"].opened', 2000).elements('#root li[nodename="aTestNode"].opened li a .jstree-themeicon', function(err, res) {
			    client
		         .moveTo(res.value[0].ELEMENT, 0, 0)
		         .buttonPress('left')
		         .keys('Shift')
		         .moveTo(res.value[1].ELEMENT, 0, 0)
		         .buttonPress('left');

			    client.keys('Shift'); // release the Shift key
			    
			    // On Mac the ctrl key opens the context menu in the browser
			    // this is why the Cmd key should be used in this case.
			    if ('darwin' === process.platform){
			    	client.keys('Command');
			    } else {
			    	client.keys('Control');
			    }
		        
			    client.moveTo(res.value[3].ELEMENT, 0, 0)
		        .buttonPress('left');
			    client.keys('NULL'); // release all keys
		  });
		  var confirmationOkBtn = 'div.bootbox-confirm div.modal-footer button[data-bb-handler="confirm"]';
		  var openTestNodeIcon = '#root li[nodename=\"aTestNode\"] i.open-icon';
		  client.keys('Delete')
		  .waitForVisible(confirmationOkBtn)
		  .click(confirmationOkBtn)
		  .waitForVisible(openTestNodeIcon)
		  .click(openTestNodeIcon)
		  .waitForExist('#last-element').elements('#root li[nodename="aTestNode"] li a .jstree-themeicon', function(err, res) {
    		  assert(typeof err === "undefined" || err === null);
			  assert(1 === res.value.length);
		  });
	      client.call(done);
		  
	  });

	  it('can delete a node with the icon', function(done) {
		  client = client.url(homeURL);
		  client.waitForExist('#last-element', function(err) {
			  client.click('li[nodetype="rep:root"] li[nodename="aTestNode"] a i.remove-icon', function(err, res) {
				  client.waitForText('div.bootbox-confirm div.bootbox-body', function(err, result, third, fourth){
					  client.click('div.bootbox-confirm div.modal-footer button[data-bb-handler="confirm"]', function(err, res) {
						  client.waitForExist('li[nodetype="rep:root"] li[nodename="aTestNode"]', true/*reverse*/, function(err, existed) {
							  assert(typeof err === "undefined" || err === null);
							  assert(existed === false);
						  });
					  });
				  });
		      })  
	      })
	      .call(done);
	  });
	
});