/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.tools.sling;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.sling.testing.tools.jarexec.JarExecutor;

/**
 * Information about a sling instance that is shared between tests.
 */
public class SlingInstanceState {

    public static final String DEFAULT_INSTANCE_NAME = "default";

    private String serverBaseUrl;
    private boolean serverStarted;
    private boolean serverReady;
    private boolean serverReadyTestFailed;
    private boolean installBundlesFailed;
    private boolean extraBundlesInstalled;
    private boolean startupInfoProvided;
    private boolean serverInfoLogged;
    private JarExecutor jarExecutor;

    /**
     * List of the urls of currently started servers
     */
    static Set<String> startedServersUrls = new CopyOnWriteArraySet<String>();

    /**
     * List of the instance names and states
     */
    private static final Map<String, SlingInstanceState> slingInstancesState = new HashMap<String, SlingInstanceState>();


    public static synchronized SlingInstanceState getInstance(String instanceName) {
        if (slingInstancesState.containsKey(instanceName)) {
            return slingInstancesState.get(instanceName);
        }
        else {
            slingInstancesState.put(instanceName, new SlingInstanceState());
        }

        return slingInstancesState.get(instanceName);
    }


    private SlingInstanceState() {

    }

    public boolean isServerStarted() {
        return serverStarted;
    }

    public boolean setServerStarted(boolean serverStarted) {
        this.serverStarted = serverStarted;
        return startedServersUrls.add(serverBaseUrl);
    }

    public boolean isServerReady() {
        return serverReady;
    }

    public void setServerReady(boolean serverReady) {
        this.serverReady = serverReady;
    }

    public boolean isServerReadyTestFailed() {
        return serverReadyTestFailed;
    }

    public void setServerReadyTestFailed(boolean serverReadyTestFailed) {
        this.serverReadyTestFailed = serverReadyTestFailed;
    }

    public boolean isInstallBundlesFailed() {
        return installBundlesFailed;
    }

    public void setInstallBundlesFailed(boolean installBundlesFailed) {
        this.installBundlesFailed = installBundlesFailed;
    }

    public boolean isExtraBundlesInstalled() {
        return extraBundlesInstalled;
    }

    public void setExtraBundlesInstalled(boolean extraBundlesInstalled) {
        this.extraBundlesInstalled = extraBundlesInstalled;
    }

    public boolean isStartupInfoProvided() {
        return startupInfoProvided;
    }

    public void setStartupInfoProvided(boolean startupInfoProvided) {
        this.startupInfoProvided = startupInfoProvided;
    }

    public boolean isServerInfoLogged() {
        return serverInfoLogged;
    }

    public void setServerInfoLogged(boolean serverInfoLogged) {
        this.serverInfoLogged = serverInfoLogged;
    }

    public JarExecutor getJarExecutor() {
        return jarExecutor;
    }

    public void setJarExecutor(JarExecutor jarExecutor) {
        this.jarExecutor = jarExecutor;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }
}