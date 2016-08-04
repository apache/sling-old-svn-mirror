package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;

/** General testing utilities and constants */ 
public class U {
    
    public static final String ADMIN = "admin";
    public static final int LONG_TIMEOUT_SECONDS = 2; // TODO 10
    public static final int LONG_TIMEOUT_MSEC = LONG_TIMEOUT_SECONDS * 1000;
    public static final int STD_INTERVAL = 250;
    public static final String SLING_API_BUNDLE = "org.apache.sling.api";
    
    static final String [] DEFAULT_MODELS = {
        "/crankstart-model.txt",
        "/provisioning-model/base.txt",
        "/provisioning-model/sling-extensions.txt",
        "/provisioning-model/start-level-99.txt",
        "/provisioning-model/crankstart-tests.txt"
    };

    static void setAdminCredentials(DefaultHttpClient c) {
        c.getCredentialsProvider().setCredentials(
                AuthScope.ANY, 
                new UsernamePasswordCredentials(ADMIN, ADMIN));
        c.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
    }
    
    static void closeConnection(HttpResponse r) throws IOException {
        if(r != null && r.getEntity() != null) {
            EntityUtils.consume(r.getEntity());
        }
    }

    /** Get JSON bundle data from webconsole */ 
    static JSONObject getBundleData(CrankstartSetup C, DefaultHttpClient client, String symbolicName) 
            throws ClientProtocolException, IOException, JSONException {
        final RequestBuilder b = new RequestBuilder(C.getBaseUrl());
        final RequestExecutor e = new RequestExecutor(client);
        return new JSONObject(e.execute(
                b.buildGetRequest("/system/console/bundles/" + symbolicName + ".json")
                .withCredentials(U.ADMIN, U.ADMIN)
        ).assertStatus(200)
        .getContent());
    }
    
    public static void assertHttpGet(CrankstartSetup C, DefaultHttpClient client, String path, String expectedContent) throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting 200 response at " + path, 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            String encoding = "UTF-8";
            if(response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            final String content = IOUtils.toString(response.getEntity().getContent(), encoding);
            assertEquals(expectedContent, content);
        } finally {
            U.closeConnection(response);
        }
    }
}