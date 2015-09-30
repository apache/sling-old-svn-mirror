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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.ide.transport.Batcher;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Command.Kind;
import org.apache.sling.ide.util.PathUtil;

public class VltBatcher implements Batcher {
    
    private List<Command<?>> queue = new ArrayList<Command<?>>();
    
    @Override
    public void add(Command<?> command) {
        queue.add(command);
    }
    
    @Override
    public List<Command<?>> get() {

        LinkedCommands deletes = new LinkedCommands();
        List<Command<?>> result = new ArrayList<Command<?>>();
        
        for ( Command<?> cmd : queue) {
            if ( cmd.getKind() == Kind.DELETE ) {
                deletes.addLinked(cmd);        
            } else {
                result.add(cmd);
            }
        }
        
        result.addAll(0, deletes.getLinkHeads());
        
        queue.clear();
        
        return result;
    }
    
    private static class LinkedCommands {
        
        private List<CommandWrapper> wrappers = new ArrayList<CommandWrapper>();
        
        public void addLinked(Command<?> cmd) {
            
            String path = cmd.getPath();
            for ( CommandWrapper wrapper : wrappers ) {
                if ( PathUtil.isAncestor(wrapper.main.getPath(), path ) ) {
                    return;
                }
                
                if ( PathUtil.isAncestor(path, wrapper.main.getPath())) {
                    wrapper.main = cmd;
                    return;
                }
            }
            
            wrappers.add(new CommandWrapper(cmd));
        }
        
        public List<Command<?>> getLinkHeads() {
            List<Command<?>> heads = new ArrayList<Command<?>>(wrappers.size());
            for ( CommandWrapper wrapper : wrappers ) {
                heads.add(wrapper.main);
            }
            
            return heads;
        }
    }
    
    private static class CommandWrapper {
        
        public Command<?> main;
        
        private CommandWrapper(Command<?> main) {
            this.main = main;
        }
    }
}
