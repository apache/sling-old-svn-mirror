package org.apache.sling.hc.junitbridge.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.hc.junitbridge.HealthCheckTestsProvider;
import org.apache.sling.junit.TestsProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

/** Test the HealthCheckTestsProvider, which 
 *  uses everything else.
 */
public class HealthCheckTestsProviderTest {
    private TestsProvider provider;
    
    final String [] TAG_GROUPS = {
            "foo,bar",
            "wii",
            "blue"
    };
    
    private static String testName(String tagGroup) {
        return HealthCheckTestsProvider.TEST_NAME_PREFIX + tagGroup + HealthCheckTestsProvider.TEST_NAME_SUFFIX;
    }
            
    @Before
    public void setup() {
        final ComponentContext ctx = Mockito.mock(ComponentContext.class);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheckTestsProvider.PROP_TAG_GROUPS, TAG_GROUPS);
        Mockito.when(ctx.getProperties()).thenReturn(props);
        
        provider = new HealthCheckTestsProvider() {
            {
                activate(ctx);
            }
        };
    }
    
    @Test
    public void testGetTestNames() {
        final List<String> names = provider.getTestNames();
        assertEquals(TAG_GROUPS.length, names.size());
        for(String tag : TAG_GROUPS) {
            final String expected = testName(tag);
            assertTrue("Expecting test names to contain " + expected + ", " + names, names.contains(expected));
        }
    }
    
}
