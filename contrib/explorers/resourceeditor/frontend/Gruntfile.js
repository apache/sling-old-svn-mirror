module.exports = function(grunt) {
	grunt.initConfig({
	    less: {
	      compileCore: {
	        options: {
	          strictMath: true,
	          sourceMap: true,
	          outputSourceFiles: true,
	          sourceMapURL: 'bootstrap.css.map',
	          sourceMapFilename: '../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/css/bootstrap.css.map'
	        },
	        src: '../src/main/less/reseditor.less',
	        dest: '../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/css/bootstrap.css'
	      }
	    }, 
	    watch: {
			less : {
				files : '../src/main/less/**/*.less',
				tasks : [ 'less' ],
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
		                  'bootbox/bootbox.min.js',
		                  'jstree/dist/jstree.min.js'
		                 ], // Actual pattern(s) to match.
		            dest: '../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/3rd_party',   // Destination path prefix.
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
	                  'animate.css/animate.min.css',
	                 ], // Actual pattern(s) to match.
	            dest: '../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/css/3rd_party',   // Destination path prefix.
	            flatten: true
	          },
	        ],
	      }
	    },
	    jasmine: {
	        main: {
	          src: ['../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/jquery.min.js',
	                '../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/**/*.js'],
	          options: {
	            specs: '../src/test/javascript/spec/*spec.js',
	            helpers: '../src/test/javascript/spec/*Helper.js',
	            version: '2.2.1',
	            summary: true
	          }
	        }
	    },
	    karma: {
	    	options: {
	    	    runnerPort: 9999,
	    	    singleRun: true,
	    	    browsers: ['PhantomJS'],
	    	    plugins : ['karma-jasmine', 'karma-phantomjs-launcher'],
	    	    frameworks: ['jasmine']
	    	},
	    	build: {
	    	    singleRun: true,
	    	    files: [
	    	            { src: ['../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/3rd_party/jquery.min.js']},
	    	            { src: ['../src/test/javascript/spec/*spec.js']},
	    	            { src: ['../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/**/*.js']}
	    	          ]
	    	},
	    	local_build: {
	    	    singleRun: true,
	    	    browsers: ['Chrome', 'Firefox', 'PhantomJS'],
	    	    plugins : ['karma-jasmine', 'karma-phantomjs-launcher', 'karma-chrome-launcher', 'karma-firefox-launcher', 'karma-ie-launcher'],
	    	    files: [
	    	            { src: ['../src/test/javascript/spec/*spec.js']},
	    	            { src: ['../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/3rd_party/jquery.min.js']},
	    	            { src: ['../src/main/resources/SLING-INF/libs/sling/resource-editor-static-content/js/**/*.js']}
	    	          ]
	    	},
	    	watch: {
	    	    reporters: 'dots',
	    	    autoWatch: true,
	    	    background: true,
	    	    singleRun: false
	    	}
	    },
        webdriver: {
            options: {
//                desiredCapabilities: {
//                    browserName: 'chrome'
//                }
            },
            chrome: {
                tests: ['../src/test/javascript/e2e/spec/**/*spec.js'],
                options: {
                    // overwrite default settings 
                    desiredCapabilities: {
                        browserName: 'chrome'
                    }
                }
            },
            firefox: {
                tests: ['../src/test/javascript/e2e/spec/**/*spec.js'],
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

//	grunt.registerTask('build', ['less', 'copy', 'jasmine', 'karma:build']);
	grunt.registerTask('build', ['less', 'copy', 'karma:build']);

	grunt.registerTask('default', ['build']);
	

    grunt.registerTask('local_build', ['less', 'copy', 'karma:local_build', 'webdriver:chrome', 'webdriver:firefox', 'build']);
};