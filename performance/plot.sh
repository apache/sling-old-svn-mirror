#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This is an example Gnuplot script for plotting the performance results
# produced by the Sling performance test suite. Before you run this
# script you need to preprocess the individual performance reports.

cat <<HTML >target/report.html
<html>
  <head>
    <title>Sling performance</title>
  </head>
  <body>
    <h1>Sling performance</h1>
    <p>
HTML

for dat in */target/*.txt; do
    rm -f target/`basename "$dat"`
done

for dat in */target/performance-reports/*.txt; do
    cat "$dat" >>target/`basename "$dat"`
done

for dat in target/*.txt; do
    name=`basename "$dat" .txt`
    rows=`grep -v "#" "$dat" | wc -l`
    gnuplot <<PLOT
set term svg size 800,400
set xtic rotate by -45
set xlabel "bundle version"
set xrange [-1:$rows]
set ylabel "Time (ms)"
set yrange [0:]
set output "target/$name.svg"
set title "$name"
plot "$dat" using 0:4:3:5:xtic(1) with yerrorlines notitle
PLOT
    convert "target/$name.svg" "target/$name.png"
    cat <<HTML >>target/report.html
      <img src="$name.png" alt="$name"><hr align="left" width="800">
HTML
done

cat <<HTML >>target/report.html
    </p>
  </body>
</html>
HTML

echo file://`pwd`/target/report.html
