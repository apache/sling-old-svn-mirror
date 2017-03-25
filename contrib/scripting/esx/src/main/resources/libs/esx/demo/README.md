<!--
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-->
# Apache Sling ESX Quick Demo

This is a simple application demonstrating some of the ESX script engine possibilities and ideas how to use it.

In future we want to switch to full ES6 syntax.


The demo purpose of this demonstration is to show the usage of npm libraries.

following NPM modules are demonstrated:
- "handlebars": "^4.0.6",
- "highlight.js": "^9.10.0",
- "marked": "^0.3.6",
- "moment": "^2.18.0",
- "underscore": "^1.8.3"

## Installation

- switch to directory src/main/resources/libs/esx/demo
- run: npm install
- go back to package root directory
- run mvn clean install sling:install´

open the page /libs/esx/demo/content/demo.html
