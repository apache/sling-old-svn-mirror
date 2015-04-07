'use strict';

var assert = require('assert');

describe('The Apache Sling Resource Editor', function() {
	browser = browser.url('http://localhost:8080/reseditor/.html');
	// Find a way to specify the host and the port via grunt. See 
	// http://stackoverflow.com/questions/29370075/how-to-pass-parameters-from-the-gruntfile-js-to-the-webdriverio-spec
	
	  it('should have a title', function(done) {
		  browser
	      .getTitle(function(err,title) {
	          assert(title.indexOf('Apache Sling Resource Editor') !== -1);
	      })
	      .call(done);
	  });
});