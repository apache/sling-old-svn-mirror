package org.apache.sling.crankstart.launcher;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;

/** General testing utilities */ 
public class U {
    
    public static final String ADMIN = "admin";
    
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
}