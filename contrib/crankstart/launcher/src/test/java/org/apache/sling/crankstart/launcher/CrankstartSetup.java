package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.util.Random;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

/** Setup a Crankstart-launched instance for our tests */ 
public class CrankstartSetup {
    
    private static final int port = getAvailablePort();
    private static Thread crankstartThread;
    private static final String baseUrl = "http://localhost:" + port;
    
    public static final String [] MODEL_PATHS = {
        "/crankstart-model.txt",
        "/provisioning-model/base.txt",
        "/provisioning-model/sling-extensions.txt",
        "/provisioning-model/start-level-99.txt",
        "/provisioning-model/crankstart-tests.txt"
    };
            
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
    
    private static void mergeModelResource(Launcher launcher, String path) throws IOException {
        final InputStream is = CrankstartSetup.class.getResourceAsStream(path);
        assertNotNull("Expecting test resource to be found:" + path, is);
        final Reader input = new InputStreamReader(is);
        try {
            launcher.mergeModel(input, path);
        } finally {
            input.close();
        }
    }
    
    String getBaseUrl() {
        return baseUrl;
    }
     
    synchronized void setup() throws IOException {
        if(crankstartThread != null) {
            return;
        }
        
        final HttpUriRequest get = new HttpGet(baseUrl);
        System.setProperty("crankstart.model.http.port", String.valueOf(port));
        System.setProperty("crankstart.model.osgi.storage.path", getOsgiStoragePath());
        
        try {
            new DefaultHttpClient().execute(get);
            fail("Expecting connection to " + port + " to fail before starting HTTP service");
        } catch(IOException expected) {
        }
        
        final Launcher launcher = new Launcher();
        for(String path : MODEL_PATHS) {
            mergeModelResource(launcher, path);
        }
        
        crankstartThread = new Thread() {
            public void run() {
                try {
                    launcher.launch();
                } catch(Exception e) {
                    e.printStackTrace();
                    fail("Launcher exception:" + e);
                }
            }
        };
        crankstartThread.setDaemon(true);
        crankstartThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                crankstartThread.interrupt();
                try {
                    crankstartThread.join();
                } catch(InterruptedException ignore) {
                }
            }
        });
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