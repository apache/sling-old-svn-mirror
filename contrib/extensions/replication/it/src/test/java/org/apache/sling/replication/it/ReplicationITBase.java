package org.apache.sling.replication.it;

import java.io.IOException;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.sling.SlingTestBase;

import static org.junit.Assert.assertEquals;

/**
 * Integration test base class for replication
 */
public abstract class ReplicationITBase extends SlingTestBase {

    private static final String JSON_SELECTOR = ".json";

    protected void assertGetResource(int status, String path, String... parameters) throws IOException {
        getRequestExecutor().execute(
                getRequestBuilder().buildGetRequest(path, parameters)
                        .withCredentials(getServerUsername(), getServerPassword())
        ).assertStatus(status);
    }

    protected void assertPostResource(int status, String path, String... headers) throws IOException {
        Request request = getRequestBuilder().buildPostRequest(path);
        if (headers != null) {
            assertEquals(0, headers.length % 2);
            for (int i = 0; i < headers.length; i += 2) {
                request = request.withHeader(headers[i], headers[i + 1]);
            }
        }
        getRequestExecutor().execute(
                request.withCredentials(getServerUsername(), getServerPassword())
        ).assertStatus(status);
    }

    protected void assertResourceExists(String path) throws IOException {
        assertGetResource(200, path);
    }

    protected void assertResourceDoesNotExist(String path) throws IOException {
        assertGetResource(404, path);
    }

    protected void assertJsonResponseEquals(String resource, String expectedJsonString) throws IOException {
        if (!resource.endsWith(JSON_SELECTOR)) {
            resource += JSON_SELECTOR;
        }
        assertEquals(expectedJsonString.trim(), getRequestExecutor().execute(
                getRequestBuilder().buildGetRequest(resource)
                        .withCredentials(getServerUsername(), getServerPassword())
        ).getContent().replaceAll("\n", "").trim());

    }


}
