'use strict';

var assert = require('assert');

describe('The Apache Sling Resource Editor', function() {
	  it('should have a title', function(done) {
		  browser
	      .url('http://localhost:8080/reseditor/.html')
	      .getTitle(function(err,title) {
	          assert(title.indexOf('Apache Sling Resource Editor') !== -1);
	      })
	      .call(done);
	  });
});