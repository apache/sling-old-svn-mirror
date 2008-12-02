package org.apache.sling.jcr.jcrinstall.jcr.impl;

import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

public class MockStartLevel implements StartLevel {

    public int getBundleStartLevel(Bundle arg0) {
        return 0;
    }

    public int getInitialBundleStartLevel() {
        return 0;
    }

    public int getStartLevel() {
        return 0;
    }

    public boolean isBundleActivationPolicyUsed(Bundle arg0) {
        return false;
    }

    public boolean isBundlePersistentlyStarted(Bundle arg0) {
        return false;
    }

    public void setBundleStartLevel(Bundle arg0, int arg1) {
    }

    public void setInitialBundleStartLevel(int arg0) {
    }

    public void setStartLevel(int arg0) {
    }
}
