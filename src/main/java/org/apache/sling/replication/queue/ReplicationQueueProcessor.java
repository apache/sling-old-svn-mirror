package org.apache.sling.replication.queue;


public interface ReplicationQueueProcessor {
    public boolean process(ReplicationQueueItem packageInfo);
}
