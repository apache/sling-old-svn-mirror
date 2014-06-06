package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/** Verify that we can start the Felix HTTP service
 *  with a {@link CrankstartBootstrap}. 
 */
public class CrankstartBootstrapTest {
    
    private static final int port = getAvailablePort();
    private static DefaultHttpClient client;
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
    
    @Before
    public void setupHttpClient() {
        client = new DefaultHttpClient(); 
    }
    
    private void setAdminCredentials() {
        client.getCredentialsProvider().setCredentials(
                AuthScope.ANY, 
                new UsernamePasswordCredentials("admin", "admin"));
        client.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
    }
    
    @BeforeClass
    public static void testExtensionPropertyBeforeTests() {
        assertNull(TEST_SYSTEM_PROPERTY + " should not be set before tests", System.getProperty(TEST_SYSTEM_PROPERTY));
    }
    
    @BeforeClass
    public static void setup() {
        client = new DefaultHttpClient(); 
        final HttpUriRequest get = new HttpGet(baseUrl);
        System.setProperty("http.port", String.valueOf(port));
        System.setProperty("osgi.storage.path", getOsgiStoragePath());
        
        final InputStream is = CrankstartBootstrapTest.class.getResourceAsStream(TEST_RESOURCE);
        assertNotNull("Expecting test resource to be found:" + TEST_RESOURCE, is);
        final Reader input = new InputStreamReader(is);
        
        try {
            client.execute(get);
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
    
    private void closeConnection(HttpResponse r) throws IOException {
        if(r != null && r.getEntity() != null) {
            EntityUtils.consume(r.getEntity());
        }
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testHttpRoot() throws Exception {
        final HttpUriRequest get = new HttpGet(baseUrl);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting page not found at " + get.getURI(), 404, response.getStatusLine().getStatusCode());
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testSingleConfigServlet() throws Exception {
        final HttpUriRequest get = new HttpGet(baseUrl + "/single");
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testConfigFactoryServlet() throws Exception {
        final String [] paths = { "/foo", "/bar/test" };
        for(String path : paths) {
            final HttpUriRequest get = new HttpGet(baseUrl + path);
            HttpResponse response = null;
            try {
                response = client.execute(get);
                assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            } finally {
                closeConnection(response);
            }
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
        final HttpUriRequest get = new HttpGet(baseUrl + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting JUnit servlet to be installed via sling extension command, at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=10000, intervalMsec=250)
    public void testFelixFormatConfig() throws Exception {
        setAdminCredentials();
        final String path = "/system/console/config/configuration-status-20140606-1347+0200.txt";
        final HttpUriRequest get = new HttpGet(baseUrl + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting config dump to be available at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            String encoding = "UTF-8";
            if(response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            final String content = IOUtils.toString(response.getEntity().getContent(), encoding);
            final String [] expected = new String[] {
                    "array = [foo, bar.from.launcher.test]",
                    "service.ranking.launcher.test = 54321"
            };
            for(String exp : expected) {
                assertTrue("Expecting config content to contain " + exp, content.contains(exp));
            }
        } finally {
            closeConnection(response);
        }
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