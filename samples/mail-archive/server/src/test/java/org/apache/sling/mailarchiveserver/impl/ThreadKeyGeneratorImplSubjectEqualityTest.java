package org.apache.sling.mailarchiveserver.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ThreadKeyGeneratorImplSubjectEqualityTest {
    private ThreadKeyGeneratorImpl generator = new ThreadKeyGeneratorImpl();
    private final String orig;
    private final String re;

    public ThreadKeyGeneratorImplSubjectEqualityTest(String input, String expected) {
        this.orig = input;
        this.re = expected;
    }

    @Parameters(name="{0}")
    public static List<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>();

        result.add(new Object[] {"Chef cookbooks for Installing CQ & packages", "Re: Chef cookbooks for Installing CQ & packages"} ); 
        result.add(new Object[] {"Dropbox to throw random files in and be accessible through http/ ftp?", "Re: Dropbox to throw random files in and be accessible through http/ ftp?"} ); 
        result.add(new Object[] {"Dropbox to throw random files in and be accessible through http/ ftp?", "RE: Dropbox to throw random files in and be accessible through http/ ftp?"} ); 
        result.add(new Object[] {"CRX integration guidelines for ES3", "答复: CRX integration guidelines for ES3"} ); 
        //        result.add(new Object[] {, } ); 

        return result;
    }

    @Test
    public void testGetThreadKey() {
        assertEquals(generator.getThreadKey(orig), generator.getThreadKey(re));
    }
}
