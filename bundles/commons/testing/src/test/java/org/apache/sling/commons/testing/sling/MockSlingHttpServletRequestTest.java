package org.apache.sling.commons.testing.sling;

import static org.junit.Assert.assertSame;

import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MockSlingHttpServletRequestTest {

    private MockSlingHttpServletRequest cut;

    @Before
    public void setUp() throws Exception {
        cut = new MockSlingHttpServletRequest();
    }

    @After
    public void tearDown() throws Exception {
        cut = null;
    }

    @Test
    public void testAdaptToWorksForResource() throws Exception {
        Resource r = new MockResource(null, "/path", "aType");
        cut.setResource(r);
        assertSame(r, cut.adaptTo(Resource.class));
    }
}
