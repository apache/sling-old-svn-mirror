package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
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
    
    private static final int port = getAvailablePort();
    private static final HttpClient client = new HttpClient();
    private static Thread crankstartThread;
    private static String baseUrl = "http://localhost:" + port;
    public static final String TEST_RESOURCE = "/launcher-test.crank.txt";
    public static final String TEST_SYSTEM_PROPERTY = "the.test.system.property";
            
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    private static int getAvailablePort() {
        int result = -1;
        ServerSocket s = null;
        try {
            try {
                s = new ServerSocket(0);
                result = s.getLocalPort();
            } finally {
                if(s != null) {
                    s.close();
                }
            }
        } catch(Exception e) {
            throw new RuntimeException("getAvailablePort failed", e);
        }
        return result;
    }
    
    @BeforeClass
    public static void testExtensionPropertyBeforeTests() {
        assertNull(TEST_SYSTEM_PROPERTY + " should not be set before tests", System.getProperty(TEST_SYSTEM_PROPERTY));
    }
    
    @BeforeClass
    public static void setup() {
        final GetMethod get = new GetMethod(baseUrl);
        System.setProperty("http.port", String.valueOf(port));
        System.setProperty("osgi.storage.path", getOsgiStoragePath());
        
        final InputStream is = CrankstartBootstrapTest.class.getResourceAsStream(TEST_RESOURCE);
        assertNotNull("Expecting test resource to be found:" + TEST_RESOURCE, is);
        final Reader input = new InputStreamReader(is);
        
        try {
            client.executeMethod(get);
            fail("Expecting connection to " + port + " to fail before starting HTTP service");
        } catch(IOException expected) {
        }
        
        crankstartThread = new Thread() {
            public void run() {
                try {
                    new CrankstartBootstrap(input).start();
                } catch(Exception e) {
                    e.printStackTrace();
                    fail("CrankstartBootstrap exception:" + e);
                } finally {
                    try {
                        input.close();
                    } catch(IOException ignoreTheresNotMuchWeCanDoAnyway) {
                    }
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
    public void testHttpRoot() throws Exception {
        final GetMethod get = new GetMethod(baseUrl);
        client.executeMethod(get);
        assertEquals("Expecting page not found at " + get.getURI(), 404, get.getStatusCode());
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testSingleConfigServlet() throws Exception {
        final GetMethod get = new GetMethod(baseUrl + "/single");
        client.executeMethod(get);
        assertEquals("Expecting success for " + get.getURI(), 200, get.getStatusCode());
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testConfigFactoryServlet() throws Exception {
        final String [] paths = { "/foo", "/bar/test" };
        for(String path : paths) {
            final GetMethod get = new GetMethod(baseUrl + path);
            client.executeMethod(get);
            assertEquals("Expecting success for " + get.getURI(), 200, get.getStatusCode());
        }
    }
    
    @Test
    public void testExtensionCommand() {
        // The SystemPropertyCommand, provided by our test-services bundle, should have
        // processed the test.system.property instruction in our launcher file 
        assertEquals("was set by test-services bundle", System.getProperty(TEST_SYSTEM_PROPERTY));
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testJUnitServlet() throws Exception {
        final String path = "/system/sling/junit";
        final GetMethod get = new GetMethod(baseUrl + path);
        client.executeMethod(get);
        assertEquals("Expecting JUnit servlet to be installed via sling extension command, at " + get.getURI(), 200, get.getStatusCode());
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
