package org.apache.sling.hc.impl.healthchecks;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PathSpecTest {
    private final String pathSpec;
    private final String expectedPath;
    private final int expectedStatus;
    
    @Parameters(name="{1}")
    public static List<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>();

        result.add(new Object[] { "/one.html", "/one.html", 200 } ); 
        result.add(new Object[] { "/two.html:404", "/two.html", 404 } ); 
        result.add(new Object[] { "three.html : 404 ", "three.html", 404 } ); 
        result.add(new Object[] { "four.html:not an integer", "four.html:not an integer", 200 } ); 
        result.add(new Object[] { "", "", 200 } ); 

        return result;
    }

    public PathSpecTest(String pathSpec, String expectedPath, int expectedStatus) {
        this.pathSpec = pathSpec;
        this.expectedPath = expectedPath;
        this.expectedStatus = expectedStatus;
    }
    
    @Test
    public void testParsing() {
        final SlingRequestStatusHealthCheck.PathSpec ps = new SlingRequestStatusHealthCheck.PathSpec(pathSpec);
        assertEquals(expectedPath, ps.path);
        assertEquals(expectedStatus, ps.status);
    }
}
