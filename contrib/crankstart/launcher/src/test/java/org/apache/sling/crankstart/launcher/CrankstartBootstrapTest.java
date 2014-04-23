package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/** Verify that we can start the Felix HTTP service
 *  with a {@link CrankstartBootstrap}. 
 */
public class CrankstartBootstrapTest {
    
    private static final int port = Integer.valueOf(System.getProperty("test.http.port", "12345"));
    private static final HttpClient client = new HttpClient();
    private static Thread crankstartThread;
    private static URL rootUrl = null;
            
    static {
        try {
            rootUrl = new URL("http://localhost:" + port + "/");
        } catch(MalformedURLException mfe) {
            fail(mfe.toString());
        }
    }
    
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    private final static String CRANKSTART = 
        "classpath mvn:org.apache.felix/org.apache.felix.framework/4.4.0\n"
        + "classpath mvn:org.slf4j/slf4j-api/1.6.2\n"
        + "classpath mvn:org.ops4j.pax.url/pax-url-aether/1.6.0\n"
        + "classpath mvn:org.ops4j.pax.url/pax-url-commons/1.6.0\n"
        + "classpath mvn:org.apache.sling/org.apache.sling.crankstart.core/0.0.1-SNAPSHOT\n"
        + "classpath mvn:org.apache.sling/org.apache.sling.crankstart.api/0.0.1-SNAPSHOT\n"
        + "osgi.property org.osgi.service.http.port " + port + "\n"
        + "osgi.property org.osgi.framework.storage " + getOsgiStoragePath() + "\n"
        + "start.framework\n"
        + "bundle mvn:org.apache.felix/org.apache.felix.http.jetty/2.2.0\n"
        + "bundle mvn:org.apache.sling/org.apache.sling.commons.log/2.1.2\n"
        + "start.all.bundles\n"
        + "log felix http service should come up at http://localhost:" + port + "\n"
    ;
    
    @BeforeClass
    public static void setup() {
        final GetMethod get = new GetMethod(rootUrl.toExternalForm());
        
        try {
            client.executeMethod(get);
            fail("Expecting connection to " + port + " to fail before starting HTTP service");
        } catch(IOException expected) {
        }
        
        crankstartThread = new Thread() {
            public void run() {
                try {
                    new CrankstartBootstrap(new StringReader(CRANKSTART)).start();
                } catch(Exception e) {
                    fail("CrankstartBootstrap exception:" + e);
                }
            }
        };
        crankstartThread.setDaemon(true);
        crankstartThread.start();
    }
    
    @AfterClass
    public static void cleanup() throws InterruptedException {
        crankstartThread.interrupt();
        crankstartThread.join();
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testHttpResponse() throws Exception {
        final GetMethod get = new GetMethod(rootUrl.toExternalForm());
        client.executeMethod(get);
        assertEquals("Expecting 404 at " + get.getURI(), 404, get.getStatusCode());
    }
    
    private static String getOsgiStoragePath() {
        final File tmpRoot = new File(System.getProperty("java.io.tmpdir"));
        final Random random = new Random();
        final File tmpFolder = new File(tmpRoot, System.currentTimeMillis() + "_" + random.nextInt());
        if(!tmpFolder.mkdir()) {
            fail("Failed to create " + tmpFolder.getAbsolutePath());
        }
        tmpFolder.deleteOnExit();
        return tmpFolder.getAbsolutePath();
    }
}
