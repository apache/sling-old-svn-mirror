package org.apache.sling.replication.it;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.sling.SlingTestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration test base class for replication
 */
public abstract class ReplicationITBase extends SlingTestBase {

    private static final String JSON_SELECTOR = ".json";
    private static final String REPLICATION_ROOT_PATH = "/libs/sling/replication";

    protected void assertGetResource(int status, String path, String... parameters) throws IOException {
        getRequestExecutor().execute(
                getRequestBuilder().buildGetRequest(path, parameters)
                        .withCredentials(getServerUsername(), getServerPassword())
        ).assertStatus(status);
    }

    protected void assertPostResourceWithParameters(int status, String path, String... parameters) throws IOException {
        Request request = getRequestBuilder().buildPostRequest(path);

        if (parameters != null) {
            assertEquals(0, parameters.length % 2);
            List<NameValuePair> valuePairList = new ArrayList<NameValuePair>();

            for (int i = 0; i < parameters.length; i += 2) {
                valuePairList.add(new BasicNameValuePair(parameters[i], parameters[i + 1]));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairList);
            request.withEntity(entity);
        }
        getRequestExecutor().execute(
                request.withCredentials(getServerUsername(), getServerPassword())
        ).assertStatus(status);
    }

    protected void assertPostResourceWithHeaders(int status, String path, String... headers) throws IOException {
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
        if (!path.endsWith(JSON_SELECTOR)) {
            path += JSON_SELECTOR;
        }

        assertGetResource(200, path);
    }

    protected void assertResourceDoesNotExist(String path) throws IOException {
        if (!path.endsWith(JSON_SELECTOR)) {
            path += JSON_SELECTOR;
        }
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

    protected void assertJsonResponseContains(String resource, String... parameters) throws IOException {
        if (!resource.endsWith(JSON_SELECTOR)) {
            resource += JSON_SELECTOR;
        }
        String content = getRequestExecutor().execute(
                getRequestBuilder().buildGetRequest(resource)
                        .withCredentials(getServerUsername(), getServerPassword())
        ).getContent().replaceAll("\n", "").trim();

        assertEquals(0, parameters.length % 2);

        for (int i = 0; i < parameters.length; i += 2) {
            assertTrue(parameters[i] + ":" + parameters[i + 1] + " is not contained in " + content,
                    content.contains("\"" + parameters[i] + "\":\"" + parameters[i + 1] + "\"") ||
                            content.contains("\"" + parameters[i] + "\":" + parameters[i + 1])
            );
        }
    }


    protected String getAgentRootUrl() {
        return REPLICATION_ROOT_PATH + "/agent";
    }

    protected String getAgentUrl(String agentName) {
        return REPLICATION_ROOT_PATH + "/agent/" + agentName;
    }

    protected String getAgentConfigUrl(String agentName) {
        return REPLICATION_ROOT_PATH + "/config/agent/" + agentName;
    }


    protected String getImporterRootUrl() {
        return REPLICATION_ROOT_PATH + "/importer";
    }

    protected String getImporterUrl(String importerName) {
        return REPLICATION_ROOT_PATH + "/importer/" + importerName;
    }
}
