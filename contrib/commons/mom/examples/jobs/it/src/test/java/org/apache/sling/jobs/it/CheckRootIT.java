package org.apache.sling.jobs.it;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by ieb on 07/04/2016.
 */
public class CheckRootIT {


    @Rule
    public final RetryRule retryRule = new RetryRule();

    private DefaultHttpClient client;


    @Before
    public void setup() throws IOException {
        client = new DefaultHttpClient();
    }

    @Test
    @Retry(timeoutMsec=Models.LONG_TIMEOUT_MSEC, intervalMsec=Models.STD_INTERVAL)
    public void testHttpRoot() throws Exception {
        final HttpUriRequest get = new HttpGet(TestSuiteLauncherIT.crankstartSetup.getBaseUrl());
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting page not found at " + get.getURI(), 404, response.getStatusLine().getStatusCode());
        } finally {
            Models.closeConnection(response);
        }
    }


}
