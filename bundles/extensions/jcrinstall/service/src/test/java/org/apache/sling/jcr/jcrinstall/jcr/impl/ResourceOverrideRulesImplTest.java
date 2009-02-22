package org.apache.sling.jcr.jcrinstall.jcr.impl;

import static org.junit.Assert.assertEquals;

public class ResourceOverrideRulesImplTest {
    private static final String [] main = { "/libs/", "/foo/" };
    private static final String [] override = { "/apps/", "/bar/" };
    
    private final ResourceOverrideRulesImpl rr = new ResourceOverrideRulesImpl(main, override);
    
    @org.junit.Test public void testNoMappings() {
        final String [] input = { "", "/foox", "somethingelse" };
        for(String path : input) {
            String [] result = rr.getHigherPriorityResources(path);
            assertEquals("Path '" + path + "' should not have higher priority resource", 0, result.length);
            result = rr.getLowerPriorityResources(path);
            assertEquals("Path '" + path + "' should not have lower priority resource", 0, result.length);
        }
    }
    
    @org.junit.Test public void testHigherPriority() {
        final String [] input = { "/libs/a/b", "libs/a/b", "libs/", "/libs/" };
        final String [] output = { "/apps/a/b", "apps/a/b", "apps/", "/apps/" };
        
        for(int i=0 ; i < input.length; i++) {
            String [] result = rr.getHigherPriorityResources(input[i]);
            assertEquals("Path '" + input[i] + "' should have one higher priority resource", 1, result.length);
            assertEquals("Path '" + input[i] + "' should map to '" + output[i] + "'", output[i], result[0]);
            result = rr.getLowerPriorityResources(input[i]);
            assertEquals("Path '" + input[i] + "' should not have lower priority resource", 0, result.length);
        }
    }
    
    @org.junit.Test public void testLowerPriority() {
        final String [] input = { "/apps/a/b", "apps/a/b", "apps/", "/apps/" };
        final String [] output = { "/libs/a/b", "libs/a/b", "libs/", "/libs/" };
        
        for(int i=0 ; i < input.length; i++) {
            String [] result = rr.getLowerPriorityResources(input[i]);
            assertEquals("Path '" + input[i] + "' should have one lower priority resource", 1, result.length);
            assertEquals("Path '" + input[i] + "' should map to '" + output[i] + "'", output[i], result[0]);
            result = rr.getHigherPriorityResources(input[i]);
            assertEquals("Path '" + input[i] + "' should not have higher priority resource", 0, result.length);
        }
    }
}
