package org.apache.sling.testing.samples.sampletests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParametrizedJunit4Test {
    private static String buffer;
    
    private static final int [] TEST_DATA = { 1,2,3 };
    
    public ParametrizedJunit4Test(Integer value) {
        buffer += value;
    }

    @BeforeClass
    public static void clear() {
        buffer = "";
    }
    
    @AfterClass
    public static void checkResult() {
        assertEquals("123", buffer);
    }
    
    @Test
    public void testSequence() {
        assertTrue(buffer.length() > 0);
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        final Collection<Object[]> data = new ArrayList<Object[]>();
        for(int i : TEST_DATA) {
            data.add(new Object[]{i});
        }
        return data;
    }
}