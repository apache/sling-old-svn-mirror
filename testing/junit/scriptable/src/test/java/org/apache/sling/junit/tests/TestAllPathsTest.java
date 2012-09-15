/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2012 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package org.apache.sling.junit.tests;

import org.junit.Test;
import org.apache.sling.junit.scriptable.TestAllPaths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAllPathsTest {

    @Test
    public void checkTestTest() throws Exception {
        final String comment =      "# this is a comment" + "\n";
        final String empty =        "    " + "\n";
        final String testPassed =   "TEST_PASSED" + "\n";
        final String any =          "this is any line" + " \n";
        final String script1 = comment +  empty + testPassed;
        final String script2 = comment +  empty + testPassed + testPassed;
        final String script3 = comment +  empty + testPassed + any;
        final String script4 = comment +  empty;
        assertTrue(TestAllPaths.checkTest(script1));
        assertFalse(TestAllPaths.checkTest(script2));
        assertFalse(TestAllPaths.checkTest(script3));
        assertFalse(TestAllPaths.checkTest(script4));
    }

}
