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
/**
 * <h1>Smoke tests for the Sling Launchpad</h1>
 * 
 * <p>This package contains a minimal set of tests for the Sling launchpad. The
 * tests validate that the launchpad is correctly assembled and that there are
 * no obvious mistakes such as bundles that can't be wired. More extensive
 * tests must be placed in specific test projects.</p>
 * 
 * <p>The launchpad tests don't depend on other Sling bundles,to prevent circular
 * dependencies. As such, there is some duplication of code ( at least intent, if 
 * not implementation ) with some of the testing projects. This is another 
 * argument for keeping the tests minimal.</p>
 */
package org.apache.sling.launchpad;