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
	var homeURL = 'http://localhost:8080/reseditor/.html';
	var client = browser.url(homeURL);
	// TODO: Find a way to specify the host and the port via grunt. See 
	// http://stackoverflow.com/questions/29370075/how-to-pass-parameters-from-the-gruntfile-js-to-the-webdriverio-spec
	client.timeouts("script", 500);

	describe('can open the add node dialog with', function() {
		  it('the icon', function(done) {
			  client = client.url(homeURL);
			  client.waitForExist('#last-element', function(err) {
				  client.click('#root_anchor i.add-icon', function(err, res) {
					  client.waitForVisible('#addNodeDialog', function(err) {
			    		  client.getCssProperty('#addNodeDialog', 'display', function(err, display) {
				    		  assert(typeof err === "undefined" || err === null);
				    		  assert(display.value === "block");
				    	  });
			    	  });
			      })
		      })
		      .call(done);
		  });

		  it('the shortcut', function(done) {
			  client = client.url(homeURL);
			  client.waitForExist('#last-element', function(err) {
				  client.click('#root_anchor i.add-icon', function(err, res) {
					  client.keys("a", function(err) {
						  client.waitForVisible('#addNodeDialog', function(err) {
				    		  client.getCssProperty('#addNodeDialog', 'display', function(err, display) {
					    		  assert(typeof err === "undefined" || err === null);
					    		  assert(display.value === "block");
					    	  });
				    	  });
					  });
			      })
		      })
		      .call(done);
		  });
	});

	  it('can add an unstructured node to the root node', function(done) {
		  client = client.url(homeURL);
		  client.waitForExist('#last-element', function(err) {
			  client.click('#root_anchor i.add-icon', function(err, res) {
				  client.waitForVisible('#addNodeDialog .add-node-finished', function(err) {
					  client.setValue('.node_name_dd_container input', 'testnode');
					  client.addValue('.node_name_dd_container input', 'Return'); // presses the 'return' key
					  client.click('#addNodeDialog .btn.btn-primary.submit', function(err, res) {
						  client.pause(3000); 
					  });
		    	  });
				  
		      })  
	      })
	      .call(done);
	  });
	
});