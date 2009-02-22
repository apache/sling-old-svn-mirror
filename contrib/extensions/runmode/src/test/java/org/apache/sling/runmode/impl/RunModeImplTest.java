package org.apache.sling.runmode.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.apache.sling.runmode.RunMode;

public class RunModeImplTest {
    
    private void assertParse(String str, String [] expected) {
        final RunMode rm = new RunModeImpl(str);
        final String [] actual = rm.getCurrentRunModes();
        assertArrayEquals("Parsed runModes match for '" + str + "'", expected, actual);
    }
    
    @org.junit.Test public void testParseRunModes() {
        assertParse(null, new String[0]);
        assertParse("", new String[0]);
        assertParse(" foo \t", new String[] { "foo" }); 
        assertParse(" foo \t,  bar\n", new String[] { "foo", "bar" }); 
    }
    
    @org.junit.Test public void testToString() {
        final RunMode rm = new RunModeImpl("\nfoo, bar\t");
        assertEquals("RunModeImpl: foo, bar", rm.toString());
    }
    
    @org.junit.Test public void testMatchesNotEmpty() {
        final RunMode rm = new RunModeImpl("foo,bar");
        
        assertTrue("single foo should be active", rm.isActive(new String[] { "foo" }));
        assertTrue("foo wiz should be active", rm.isActive(new String[] { "foo", "wiz" }));
        assertTrue("star wiz should be active", rm.isActive(new String[] { "*", "wiz" }));
        assertTrue("star should be active", rm.isActive(new String[] { "*" }));
        
        assertFalse("wiz should be not active", rm.isActive(new String[] { "wiz" }));
        assertFalse("wiz bah should be not active", rm.isActive(new String[] { "wiz", "bah" }));
        assertFalse("empty should be not active", rm.isActive(new String[0]));
    }
}