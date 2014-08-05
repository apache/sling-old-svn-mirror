package org.apache.sling.replication.it;


import org.apache.sling.replication.communication.ReplicationActionType;
import org.junit.Test;

import static org.apache.sling.replication.it.ReplicationUtils.*;

public class ReverseReplicationTest extends ReplicationIntegrationTestBase {
    @Test
    public void testAddContent() throws Exception {
        String nodePath = createRandomNode(publishClient, "/content/reverse_add_" + System.nanoTime());
        assertExists(publishClient, nodePath);
        replicate(publish, "reverse", ReplicationActionType.ADD, nodePath);
        assertExists(authorClient, nodePath);
    }


    @Test
    public void testDeleteContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/reverse_del_" + System.nanoTime());
        assertExists(authorClient, nodePath);
        replicate(publish, "reverse", ReplicationActionType.DELETE, nodePath);
        assertNotExits(authorClient, nodePath);
    }
}
