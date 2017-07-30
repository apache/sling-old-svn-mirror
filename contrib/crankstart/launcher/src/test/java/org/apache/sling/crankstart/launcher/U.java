package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;

/** General testing utilities and constants */ 
public class U {
    
    public static final String ADMIN = "admin";
    public static final int LONG_TIMEOUT_SECONDS = 10;
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
    static JsonObject getBundleData(CrankstartSetup C, DefaultHttpClient client, String symbolicName) 
            throws ClientProtocolException, IOException, JsonException {
        final RequestBuilder b = new RequestBuilder(C.getBaseUrl());
        final RequestExecutor e = new RequestExecutor(client);
        return Json.createReader(new StringReader((e.execute(
                b.buildGetRequest("/system/console/bundles/" + symbolicName + ".json")
                .withCredentials(U.ADMIN, U.ADMIN))
            ).assertStatus(200).getContent())).readObject();
    }
    
    public static String getContent(HttpResponse response) throws IOException{
    
        final HttpEntity e = response.getEntity();
        if(e == null) {
            throw new IOException("Response does not provide an Entity");
        }
        
        String encoding = "UTF-8";
        if(response.getEntity().getContentEncoding() != null) {
            encoding = response.getEntity().getContentEncoding().getValue();
        }
        
        try {
            return IOUtils.toString(e.getContent(), encoding);
        } finally {
            e.consumeContent();
        }
    }
    
    public static void assertHttpGet(CrankstartSetup C, DefaultHttpClient client, String path, String expectedContent) throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting 200 response at " + path, 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            final String content = getContent(response);
            assertEquals(expectedContent, content);
        } finally {
            U.closeConnection(response);
        }
    }
}