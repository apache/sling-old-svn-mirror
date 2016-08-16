package org.apache.sling.jobs.it;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by ieb on 07/04/2016.
 */
public class Models {
    public static final int LONG_TIMEOUT_SECONDS = 2; // TODO 10
    public static final int LONG_TIMEOUT_MSEC = LONG_TIMEOUT_SECONDS * 1000;
    public static final int STD_INTERVAL = 250;

    static final String[] DEFAULT_MODELS = {
            "/crankstart-model.txt",
            "/provisioning-model/base.txt",
            "/provisioning-model/jobs-runtime.txt",
            "/provisioning-model/crankstart-test-support.txt"
    };

    static void closeConnection(HttpResponse r) throws IOException {
        if(r != null && r.getEntity() != null) {
            EntityUtils.consume(r.getEntity());
        }
    }
}
