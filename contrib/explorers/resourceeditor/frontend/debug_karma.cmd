#!/bin/sh
export PATH=$PATH:./node
export PHANTOMJS_BIN="node_modules/karma-phantomjs-launcher/node_modules/phantomjs/lib/phantom/bin/phantomjs"
./grunt node-inspector &
node --debug-brk ./node_modules/karma/bin/karma start
echo "visit http://localhost:5050/?ws=localhost:5050&port=5858 and http://localhost:9876/"