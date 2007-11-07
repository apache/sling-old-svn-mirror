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
package org.apache.sling.osgi.obr;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

/**
 * The <code>ResourceTest</code> TODO
 *
 * @author fmeschbe
 * @version $Rev$, $Date: 2007-07-02 16:13:58 +0200 (Mon, 02 Jul 2007) $
 */
public class ResourceTest extends TestCase {

    public void testMapper0() throws Exception {
//        File bundle = new File("S:/maven/m2/repository/org/apache/felix/org.apache.felix.shell.gui.plugin/0.8.0-SNAPSHOT/org.apache.felix.shell.gui.plugin-0.8.0-SNAPSHOT.jar");
//        testInternal(bundle);
    }

//    public void testMapper1() throws Exception {
//        File bundle = new File("S:/maven/m2/repository/org/apache/sling/sling-log/2.0.0-INCUBATOR-SNAPSHOT/sling-log-2.0.0-INCUBATOR-SNAPSHOT.jar");
//        testInternal(bundle);
//    }

    private void testInternal(File bundle) throws Exception {
        URL bundleURL = bundle.toURI().toURL();
        Resource resource = Resource.create(bundleURL);

//        PrintWriter pw = new PrintWriter(System.out);
//        resource.serialize(pw, "");
//        pw.flush();
//        pw.close();
    }
}
