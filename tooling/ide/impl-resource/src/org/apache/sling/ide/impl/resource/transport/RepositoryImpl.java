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
package org.apache.sling.ide.impl.resource.transport;

import org.apache.commons.httpclient.HttpClient;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.TracingCommand;
import org.osgi.service.event.EventAdmin;

public class RepositoryImpl extends AbstractRepository{
	
    private final HttpClient httpClient = new HttpClient();
    private EventAdmin eventAdmin;

    private <T> Command<T> wrap(AbstractCommand<T> command) {
        return new TracingCommand<T>(command, eventAdmin);
    }

	@Override
	public Command<Void> newDeleteNodeCommand(final ResourceProxy resource) {
        return wrap(new DeleteNodeCommand(resource, repositoryInfo, httpClient));
	}
	
	@Override
    public Command<ResourceProxy> newListChildrenNodeCommand(final String path) {
        return wrap(new ListChildrenCommand(repositoryInfo, httpClient, path + ".1.json"));
	}

	@Override
	public Command<byte[]> newGetNodeCommand(final String path) {
		
        return wrap(new GetNodeCommand(repositoryInfo, httpClient, path));
	}
	
	@Override
    public Command<ResourceProxy> newGetNodeContentCommand(final String path) {
        return wrap(new GetNodeContentCommand(repositoryInfo, httpClient, path + ".json"));
	}
	
	@Override
    public Command<Void> newAddOrUpdateNodeCommand(final FileInfo fileInfo, ResourceProxy resource) {
		
        return wrap(new UpdateContentCommand(repositoryInfo, httpClient, fileInfo.getRelativeLocation(),
                resource.getProperties(), fileInfo));
	}

    public void bindEventAdmin(EventAdmin eventAdmin) {

        this.eventAdmin = eventAdmin;
    }

    public void unbindEventAdmin(EventAdmin eventAdmin) {

        this.eventAdmin = null;
    }
}
