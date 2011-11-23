/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.testservices.jcr;

import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;

@Component
@Property(name = Constants.SERVICE_DESCRIPTION, value = "Generates Test Workspaces ws1, ws2, ws3")
public class WorkspaceCreator {

    @Reference
    private SlingRepository repo;

    @SuppressWarnings("unused")
    @Activate
    private void activate() {
        Session s = null;
        try {
            s = repo.loginAdministrative(null);
            Workspace w = s.getWorkspace();
            createWorkspace(w, "ws1");
            createWorkspace(w, "ws2");
            createWorkspace(w, "ws3");
        } catch (Exception e) {
            // ignore
        } finally {
            if (s != null) {
                s.logout();
            }
        }
    }

    @SuppressWarnings("unused")
    @Deactivate
    private void deactivate() {
        Session s = null;
        try {
            s = repo.loginAdministrative(null);
            Workspace w = s.getWorkspace();
            deleteWorkspace(w, "ws3");
            deleteWorkspace(w, "ws2");
            deleteWorkspace(w, "ws1");
        } catch (Exception e) {
            // ignore
        } finally {
            if (s != null) {
                s.logout();
            }
        }
    }

    private void createWorkspace(final Workspace w, final String name) {
        try {
            w.createWorkspace(name);
        } catch (Exception e) {
            // ignore;
        }
    }

    private void deleteWorkspace(final Workspace w, final String name) {
        try {
            w.deleteWorkspace(name);
        } catch (Exception e) {
            // ignore;
        }
    }
}
