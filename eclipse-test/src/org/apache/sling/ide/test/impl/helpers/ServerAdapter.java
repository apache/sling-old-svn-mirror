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
package org.apache.sling.ide.test.impl.helpers;

import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.concurrent.Callable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;

/**
 * The <tt>ServerAdapter</tt> adapts the Eclipse server APIs to make them simpler to use for testing purposes
 *
 */
public class ServerAdapter {

    private final IServer server;

    public ServerAdapter(IServer server) {
        this.server = server;
    }

    public void installModule(final IProject project) throws CoreException, InterruptedException {

        // not sure why we need to poll at all here ... there is some async operation that I'm not aware of
        IModule bundleModule = new Poller().pollUntil(new Callable<IModule>() {
            @Override
            public IModule call() throws Exception {
                return ServerUtil.getModule(project);
            }
        }, notNullValue());

        IServerWorkingCopy serverWorkingCopy = server.createWorkingCopy();
        serverWorkingCopy.modifyModules(new IModule[] { bundleModule }, new IModule[0], new NullProgressMonitor());
        serverWorkingCopy.save(false, new NullProgressMonitor());

    }
}
