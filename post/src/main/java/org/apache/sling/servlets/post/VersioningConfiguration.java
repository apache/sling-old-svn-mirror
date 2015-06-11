/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.servlets.post;


/**
 * Data structure to hold the various options associated with how versionable
 * nodes are handled in the post servlet.
 */
public class VersioningConfiguration implements Cloneable {

    private boolean autoCheckout = false;

    private boolean checkinOnNewVersionableNode = false;

    private boolean autoCheckin = true;

    @Override
    public VersioningConfiguration clone() {
        VersioningConfiguration cfg = new VersioningConfiguration();
        cfg.checkinOnNewVersionableNode = checkinOnNewVersionableNode;
        cfg.autoCheckout = autoCheckout;
        cfg.autoCheckin = autoCheckin;
        return cfg;
    }

    public boolean isAutoCheckout() {
        return autoCheckout;
    }

    public boolean isCheckinOnNewVersionableNode() {
        return checkinOnNewVersionableNode;
    }

    public boolean isAutoCheckin() {
        return autoCheckin;
    }

    public void setAutoCheckin(boolean autoCheckin) {
        this.autoCheckin = autoCheckin;
    }

    public void setAutoCheckout(boolean autoCheckout) {
        this.autoCheckout = autoCheckout;
    }

    public void setCheckinOnNewVersionableNode(boolean checkinOnNewVersionableNode) {
        this.checkinOnNewVersionableNode = checkinOnNewVersionableNode;
    }

}
