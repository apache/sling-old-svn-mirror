package org.apache.sling.replication.it;

import java.io.IOException;
import org.apache.sling.testing.tools.sling.SlingTestBase;

/**
 * Integration test base class for replication
 */
public abstract class ReplicationITBase extends SlingTestBase {

    protected void assertResourceExists(String path) throws IOException {
        getRequestExecutor().execute(
                getRequestBuilder().buildGetRequest(path)
                        .withCredentials(getServerUsername(), getServerPassword())
        ).assertStatus(200);
    }

    protected void assertResourceDoesNotExist(String path) throws IOException {
        getRequestExecutor().execute(
                getRequestBuilder().buildGetRequest(path)
                        .withCredentials(getServerUsername(), getServerPassword())
        ).assertStatus(404);
    }


}
