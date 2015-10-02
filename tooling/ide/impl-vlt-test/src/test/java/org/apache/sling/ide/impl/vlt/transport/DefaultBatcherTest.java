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
package org.apache.sling.ide.impl.vlt.transport;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Repository;

import org.apache.sling.ide.impl.vlt.AddOrUpdateNodeCommand;
import org.apache.sling.ide.impl.vlt.DeleteNodeCommand;
import org.apache.sling.ide.impl.vlt.GetNodeContentCommand;
import org.apache.sling.ide.impl.vlt.ReorderChildNodesCommand;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.impl.DefaultBatcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

// TODO - this is not well-placed here, but in api-test we don't have access
// to a set of useful commands and I feel disclined to start a parallel
// set of stub commands just for that
public class DefaultBatcherTest {

    private Repository mockRepo;
    private Credentials credentials;
    private DefaultBatcher batcher;

    @Before
    public void prepare() {
        mockRepo = createMock(Repository.class); 
        credentials = createMock(Credentials.class);
        batcher = new DefaultBatcher();
    }
    
    @Test
    public void moreComprehensiveDeletesAreCompacted() {

        testMoreComprehensiveDeletesAreCompacted("/content", "/content", "/content/sub");
    }

    @Test
    public void moreComprehensiveDeletesAreCompacted_reverseOrder() {
        
        testMoreComprehensiveDeletesAreCompacted("/content", "/content/sub", "/content");
        
    }
    
    private void testMoreComprehensiveDeletesAreCompacted(String expected, String firstPath, String... otherPaths) {

        batcher.add(new DeleteNodeCommand(mockRepo, credentials, firstPath, null));
        for ( String otherPath: otherPaths) {
            batcher.add(new DeleteNodeCommand(mockRepo, credentials, otherPath, null));
        }
        
        List<Command<?>> batched = batcher.get();
        
        assertThat(batched, hasSize(1));
        Command<?> command = batched.get(0);
        assertThat(command, instanceOf(DeleteNodeCommand.class));
        assertThat(command.getPath(), equalTo(expected));
    }

    @Test
    public void unrelatedDeletesAreNotCompacted() {
        
        assertCommandsAreNotCompacted(new DeleteNodeCommand(mockRepo, credentials, "/content/branch", null), 
                new DeleteNodeCommand(mockRepo, credentials, "/content/sub", null));
    }
    
    public void assertCommandsAreNotCompacted(Command<?> first, Command<?> second) {

        batcher.add(first);
        batcher.add(second);
        
        List<Command<?>> batched = batcher.get();
        
        assertThat(batched, hasSize(2));
        assertThat(batched.get(0), Matchers.<Command<?>> sameInstance(first));
        assertThat(batched.get(1), Matchers.<Command<?>> sameInstance(second));

    }
    
    @Test
    public void dataIsClearedBetweenCalls() {
        batcher.add(new DeleteNodeCommand(mockRepo, credentials, "/content/branch", null));
        batcher.get();
        assertThat(batcher.get(), hasSize(0));
    }
    
    @Test
    public void identicalAddOrUpdatesAreCompacted() {
        
        AddOrUpdateNodeCommand first = new AddOrUpdateNodeCommand(mockRepo, credentials, null, null, new ResourceProxy("/content"), null);
        AddOrUpdateNodeCommand second = new AddOrUpdateNodeCommand(mockRepo, credentials, null, null, new ResourceProxy("/content"), null);
        
        batcher.add(first);
        batcher.add(second);
        
        List<Command<?>> batched = batcher.get();
        
        assertThat(batched, hasSize(1));
        Command<?> command = batched.get(0);
        assertThat(command, instanceOf(AddOrUpdateNodeCommand.class));
        assertThat(command.getPath(), equalTo("/content"));
        
    }

    @Test
    public void unrelatedAddOrUpdatesAreNotCompacted() {
        
        assertCommandsAreNotCompacted(new AddOrUpdateNodeCommand(mockRepo, credentials, null, null, new ResourceProxy("/content/a"), null), 
                new AddOrUpdateNodeCommand(mockRepo, credentials, null, null, new ResourceProxy("/content/b"), null));
    }

    @Test
    public void identicalsReorderingsAreCompacted() {
        
        ReorderChildNodesCommand first = new ReorderChildNodesCommand(mockRepo, credentials, new ResourceProxy("/content"), null);
        ReorderChildNodesCommand second = new ReorderChildNodesCommand(mockRepo, credentials,new ResourceProxy("/content"), null);
        
        batcher.add(first);
        batcher.add(second);
        
        List<Command<?>> batched = batcher.get();
        
        assertThat(batched, hasSize(1));
        Command<?> command = batched.get(0);
        assertThat(command, instanceOf(ReorderChildNodesCommand.class));
        assertThat(command.getPath(), equalTo("/content"));
    }
    
    @Test
    public void unrelatedReorderingsAreNotCompacted() {
        
        assertCommandsAreNotCompacted(new ReorderChildNodesCommand(mockRepo, credentials, new ResourceProxy("/content/a"), null), 
                new ReorderChildNodesCommand(mockRepo, credentials,new ResourceProxy("/content/b"), null));
    }
    
    @Test
    public void unhandledCommandIsReturnedAsIs() {
        
        assertCommandsAreNotCompacted(new GetNodeContentCommand(mockRepo, credentials, "/content", null), 
                new GetNodeContentCommand(mockRepo, credentials, "/content", null));
    }
}
