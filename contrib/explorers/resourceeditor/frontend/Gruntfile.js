module.exports = function(grunt) {

	var staticContentFolder = '../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content';
	var jspFolder = '../src/main/resources/SLING-INF/libs/sling/resource-editor';
	var e2eTestSpecFolder = '../src/test/javascript/e2e/spec/**/*spec.js';
	var server = 'localhost';
	var port = '8080';
	
	grunt.initConfig({
		env : {
		    build : {
		    	PHANTOMJS_BIN: 'node_modules/karma-phantomjs-launcher/node_modules/phantomjs/lib/phantom/bin/phantomjs',
		    	SLING_SERVER: (typeof process.env.SLING_SERVER === 'undefined' || process.env.SLING_SERVER === null || '' === process.env.SLING_SERVER) ? server : process.env.SLING_SERVER,
		    	SLING_PORT: (typeof process.env.SLING_PORT === 'undefined' || process.env.SLING_PORT === null || '' === process.env.SLING_PORT) ? port : process.env.SLING_PORT
		    }
		},
		'node-inspector': {
		  custom: {
		    options: {
		      'web-port': 5050,
		      'web-host': 'localhost',
		      'debug-port': 5857
		    }
		  }
		},
	    less: {
	      compileCore: {
	        options: {
	          strictMath: true,
	          sourceMap: true,
	          outputSourceFiles: true,
	          sourceMapURL: 'bootstrap.css.map',
	          sourceMapFilename: staticContentFolder+'/generated/css/bootstrap.css.map'
	        },
	        src: '../src/main/less/reseditor.less',
	        dest: staticContentFolder+'/generated/css/bootstrap.css'
	      }
	    }, 
	    watch: {
			less : {
				files : '../src/main/less/**/*.less',
				tasks : [ 'less' ],
			},
			all : {
				files : ['../src/main/less/**/*.less', 
				         '../src/test/javascript/**/*spec.js',
				         staticContentFolder+'/js/**/*.js',
				         jspFolder+'/*.*'
				         ],
				tasks : [ 'desktop_build' ],
			},
			e2e : {
				files : ['../src/main/less/**/*.less', 
				         '../src/test/javascript/**/*spec.js',
				         staticContentFolder+'/js/**/*.js',
				         jspFolder+'/*.*'
				         ],
				tasks : ['env:build', 'webdriver:chrome', 'webdriver:firefox']
			},
			karma : {
				files:[
			            staticContentFolder+'/generated/3rd_party/js/**/*.js',
			            staticContentFolder+'/js/**/*.js',
			            '../src/test/javascript/spec/*spec.js'
				        ],
				tasks: ['karma:desktop_build']
				
			}
	    },
	    _comment:'The google web fonts could be downloaded and copied via grunt-goog-webfont-dl. But goog-webfont-dl directly points to the global #!/usr/bin/env node and not to the local one.',
	    copy: {
	    	js_dependencies: {
		        files: [
		          {
		            expand: true,     // Enable dynamic expansion.
		            cwd: 'node_modules/',      // Src matches are relative to this path.
		            src: [
		                  'bootstrap/dist/js/bootstrap.min.js',
		                  'select2/select2.min.js',
		                  'jquery/dist/jquery.min.js',
		                  'jquery/dist/jquery.min.map',
		                  'bootbox/bootbox.min.js',
		                  'jstree/dist/jstree.min.js',
		                  'bootstrap-notify/dist/bootstrap-notify.min.js'
		                 ], // Actual pattern(s) to match.
		            dest: staticContentFolder+'/generated/3rd_party/js',   // Destination path prefix.
		            flatten: true
		          },
		        ],
		      },
	    	css_dependencies: {
		        files: [
		          {
		            expand: true,     // Enable dynamic expansion.
		            cwd: 'node_modules/',      // Src matches are relative to this path.
		            src: [
		                  'select2/select2.css',
		                  'select2/select2.png',
		                  'select2/select2-spinner.gif',
		                  'animate.css/animate.min.css',
		                  'jstree/dist/themes/default/style.min.css',
		                  'jstree/dist/themes/default/32px.png',
		                  'jstree/dist/themes/default/40px.png',
		                  'jstree/dist/themes/default/throbber.gif',
		                 ], // Actual pattern(s) to match.
		            dest: staticContentFolder+'/generated/3rd_party/css',   // Destination path prefix.
		            flatten: true
		          },
		        ],
		      },
		    font_dependencies: {
		        files: [
		          {
		            expand: true,     // Enable dynamic expansion.
		            cwd: 'node_modules/',      // Src matches are relative to this path.
		            src: [
		                  'bootstrap/fonts/glyphicons-halflings-regular.ttf',
		                  'bootstrap/fonts/glyphicons-halflings-regular.woff2',
		                  'bootstrap/fonts/glyphicons-halflings-regular.woff'
		                 ], // Actual pattern(s) to match.
		            dest: staticContentFolder+'/generated/3rd_party/fonts',   // Destination path prefix.
		            flatten: true
		          },
		        ],
		      }
	    },
	    karma: {
	    	options: {
	    	    runnerPort: 9999,
	    	    singleRun: true,
	    	    browsers: ['Chrome', 'Firefox', 'PhantomJS'],
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
	    	},  
	    	desktop_build: {
	    	    singleRun: true,
	    	    browsers: ['Chrome', 'Firefox']
	    	},
	    	multi_run: {
	    	    singleRun: false,
	    	    browsers: ['Chrome', 'Firefox']
	    	},
	    	build: {
	    	    singleRun: true,
	    	    browsers: ['PhantomJS']
	    	}
	    },
        webdriver: {
            options: {
            },
            chrome: {
                tests: [e2eTestSpecFolder],
                options: {
                    // overwrite default settings 
                    desiredCapabilities: {
                        browserName: 'chrome'
                    }
                }
            },
            firefox: {
                tests: [e2eTestSpecFolder],
                options: {
                    // overwrite default settings 
                    desiredCapabilities: {
                        browserName: 'firefox'
                    }
                }
            }
        }
	})
	
    // These plugins provide necessary tasks.
    require('load-grunt-tasks')(grunt, { scope: 'devDependencies' });

	grunt.registerTask('setup', ['env:build']);
	grunt.registerTask('build', ['setup', 'less', 'copy', 'karma:build']);

	grunt.registerTask('default', ['build']);
	

    grunt.registerTask('desktop_build', ['setup', 'less', 'copy', 'karma:desktop_build', 'webdriver:chrome', 'webdriver:firefox']);
};