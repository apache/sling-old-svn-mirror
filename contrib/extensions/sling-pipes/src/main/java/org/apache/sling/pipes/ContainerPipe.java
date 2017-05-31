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
package org.apache.sling.pipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This pipe executes the pipes it has in its configuration, chaining their result, and
 * modifying each contained pipe's expression with its context
 */
public class ContainerPipe extends BasePipe {
    private static final Logger log = LoggerFactory.getLogger(ContainerPipe.class);

    public static final String RESOURCE_TYPE = "slingPipes/container";

    Map<String, Pipe> pipes = new HashMap<>();

    List<Pipe> pipeList = new ArrayList<>();

    List<Pipe> reversePipeList = new ArrayList<>();

    /**
     * Constructor
     * @param plumber plumber
     * @param resource container's configuration resource
     * @throws Exception bad configuration handling
     */
    public ContainerPipe(Plumber plumber, Resource resource) throws Exception{
        super(plumber, resource);
        for (Iterator<Resource> childPipeResources = getConfiguration().listChildren(); childPipeResources.hasNext();){
            Resource pipeResource = childPipeResources.next();
            Pipe pipe = plumber.getPipe(pipeResource);
            if (pipe == null) {
                log.error("configured pipe {} is either not registered, or not computable by the plumber", pipeResource.getPath());
            } else {
                pipe.setParent(this);
                pipe.setBindings(bindings);
                pipes.put(pipe.getName(), pipe);
                pipeList.add(pipe);
                reversePipeList.add(pipe);
            }
        }
        Collections.reverse(reversePipeList);
    }

    @Override
    public boolean modifiesContent() {
        for (Pipe pipe : pipes.values()){
            if (pipe.modifiesContent()){
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Resource> getOutput()  {
        return new ContainerResourceIterator(this);
    }

    /**
     * Returns the pipe immediately before the given pipe, null if it's the first
     * @param pipe given pipe
     * @return previous pipe of the param
     */
    public Pipe getPreviousPipe(Pipe pipe){
        Pipe previousPipe = null;
        for (Pipe candidate : pipeList){
            if (candidate.equals(pipe)){
                return previousPipe;
            }
            previousPipe = candidate;
        }
        return null;
    }

    /**
     * Return the first pipe in the container
     * @return first pipe of the container
     */
    public Pipe getFirstPipe() {
        return pipeList.iterator().next();
    }

    /**
     * Return the last pipe in the container
     * @return pipe in the last position of the container's pipes
     */
    public Pipe getLastPipe() {
        return reversePipeList.iterator().next();
    }

    /**
     * output resource of the container pipe
     * @return output resource of the last pipe of the container
     */
    public Resource getOutputResource() {
        return bindings.getExecutedResource(getLastPipe().getName());
    }

    /**
     * Container Iterator, that iterates through the whole chain
     * of pipes, returning the result resources of the end of the chain
     */
    static class ContainerResourceIterator implements Iterator<Resource> {
        /**
         * map name -> iterator
         */
        Map<Pipe, Iterator<Resource>> iterators;

        /**
         * container pipe
         */
        ContainerPipe container;

        PipeBindings bindings;

        boolean computedCursor = false;
        boolean hasNext = false;
        int cursor = 0;

        /**
         * Constructor
         * @param containerPipe corresponding container pipe
         */
        ContainerResourceIterator(ContainerPipe containerPipe) {
            container = containerPipe;
            bindings = container.bindings;
            iterators = new HashMap<>();
            Pipe firstPipe = container.getFirstPipe();
            //we initialize the first iterator the only one not to be updated
            iterators.put(firstPipe, firstPipe.getOutput());
        }

        /**
         * go up and down the container iterators until cursor is at 0 (first pipe) with no
         * more resources, or at length - 1 (last pipe) with a next one
         * @return true if cursor has been updated
         */
        private boolean updateCursor(){
            Pipe currentPipe = container.pipeList.get(cursor);
            Iterator<Resource> it = iterators.get(currentPipe);
            do {
                // go up to at best reach the last pipe, updating iterators & bindings of the
                // all intermediates, if an intermediate pipe is not outputing anything
                // anymore, stop.
                while (it.hasNext() && cursor < container.pipeList.size() - 1) {
                    Resource resource = it.next();
                    bindings.updateBindings(currentPipe, resource);
                    //now we update the following pipe output with that new context
                    Pipe nextPipe = container.pipeList.get(++cursor);
                    iterators.put(nextPipe, nextPipe.getOutput());
                    currentPipe = nextPipe;
                    it = iterators.get(currentPipe);
                }
                //go down (or stay) to the first pipe having a next item
                while (!it.hasNext() && cursor > 0) {
                    currentPipe = container.pipeList.get(--cursor);
                    it = iterators.get(currentPipe);
                }
            } while (it.hasNext() && cursor < container.pipeList.size() - 1);
            //2 choices here:
            // either cursor is at 0 with no resource left: end,
            // either cursor is on last pipe with a resource left: hasNext
            // the second part is for the corner case with only one item
            return cursor > 0 || (iterators.size() == 1 && it.hasNext());
        }

        /**
         * we need to find the first "path" from first pipe to the last
         * where each pipe returns something, if no "path", this pipe is
         * done, other wise we must have updated iterators (next is allowed
         * up to the pipe before the last), and return true
         * @return
         */
        @Override
        public boolean hasNext() {
            if (! computedCursor) {
                hasNext = updateCursor();
            }
            return hasNext;
        }

        @Override
        public Resource next() {
            hasNext = computedCursor && hasNext || hasNext();
            if (hasNext) {
                computedCursor = false;
                hasNext = false;
                Resource resource =  iterators.get(container.getLastPipe()).next();
                bindings.updateBindings(container.getLastPipe(), resource);
                return resource;
            }
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
