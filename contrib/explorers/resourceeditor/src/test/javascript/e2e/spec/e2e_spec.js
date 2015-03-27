'use strict';

var assert = require('assert');

describe('The Apache Sling Resource Editor', function() {
  it('should have a title', function(done) {
	  browser
      .url('http://juliemr.github.io/protractor-demo/')
      .getTitle(function(err,title) {
          assert(title.indexOf('Super Calculator') !== -1);
      })
      .call(done);
  });
});