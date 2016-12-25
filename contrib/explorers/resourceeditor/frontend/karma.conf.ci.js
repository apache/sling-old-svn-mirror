var staticContentFolder = '../src/main/resources/SLING-INF/libs/sling/resource-editor/static';
module.exports = function(config) {
  config.set({
	    runnerPort: 9999,
	    singleRun: true,
	    browsers: ['PhantomJS'],
	    reporters: ["spec"],
	    specReporter: {maxLogLines: 5},
	    plugins : ['karma-jasmine', 'karma-phantomjs-launcher', 'karma-chrome-launcher', 'karma-firefox-launcher', 'karma-ie-launcher', 
	               'karma-spec-reporter'],
	    frameworks: ['jasmine'],
	    files: [
	            staticContentFolder+'/generated/3rd_party/js/jquery.min.js',
	            staticContentFolder+'/generated/3rd_party/js/**/*.js',
	            staticContentFolder+'/js/**/*.js',
	            '../src/test/javascript/spec/*spec.js'
	           ]
  });
};