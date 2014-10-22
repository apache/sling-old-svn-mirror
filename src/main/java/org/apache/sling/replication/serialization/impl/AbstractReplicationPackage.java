package org.apache.sling.replication.serialization.impl;

import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageInfo;

public abstract class AbstractReplicationPackage implements ReplicationPackage {
    private final ReplicationPackageInfo info  = new SimpleReplicationPackageInfo();

    public ReplicationPackageInfo getInfo() {
        return info;
    }


}
