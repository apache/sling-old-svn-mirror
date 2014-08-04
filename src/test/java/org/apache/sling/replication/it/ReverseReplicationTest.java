package org.apache.sling.replication.it;


import org.apache.sling.replication.communication.ReplicationActionType;
import org.junit.Test;

import static org.apache.sling.replication.it.ReplicationUtils.assertExists;
import static org.apache.sling.replication.it.ReplicationUtils.createRandomNode;
import static org.apache.sling.replication.it.ReplicationUtils.replicate;

public class ReverseReplicationTest extends ReplicationIntegrationTestBase {
    @Test
    public void testAddContent() throws Exception {
        String nodePath = createRandomNode(publishClient, "/content");
        assertExists(publishClient, nodePath);
        replicate(publish, "reverse", ReplicationActionType.ADD, nodePath);

        assertExists(authorClient, nodePath);
    }
}
