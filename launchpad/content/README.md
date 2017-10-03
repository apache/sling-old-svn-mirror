# Apache Sling Launchpad Content

This module is part of the [Apache Sling](https://sling.apache.org) project.

The Launchpad Content bundle provides some initial and sample content
as well as links to the Online resources at http://sling.apache.org

## Working with the Front-end

This package uses SCSS / SASS to build the styles for the content. It is not necessary
to build the SCSS for every build unless you wish to edit the styles. To do so, you
will need NPM and Gulp. Please use the following steps to install (if needed) and 
build the front-end.

    1. Install NPM (https://www.npmjs.com/get-npm)
    2. Install Gulp (https://github.com/gulpjs/gulp/blob/master/docs/getting-started.md)
    3. Install dependencies in src/main/resources/content/etc/clientlibs/launchpad/src
        $ npm install
    4. Run Gulp
        $ gulp

The gulp process will compile the grid system, fonts, and index SCSS files into 
a minified index.css in the dist folder.
