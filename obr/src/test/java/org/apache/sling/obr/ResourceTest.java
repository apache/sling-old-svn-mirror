/*
 * $Url: $
 * $Id: ResourceTest.java 28983 2007-07-02 14:13:58Z fmeschbe $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.obr;

import java.io.File;
import java.net.URL;

import org.apache.sling.obr.Resource;


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
//        File bundle = new File("S:/maven/m2/repository/com/day/osgi/day-osgi-log/1.0.0-SNAPSHOT/day-osgi-log-1.0.0-SNAPSHOT.jar");
//        testInternal(bundle);
//    }
    
//    public void testMapper2() throws Exception {
//        File bundle = new File("s:/src/osgi/day-osgi-servlet/obr/day-osgi-obr-1.0.0-SNAPSHOT.jar");
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
