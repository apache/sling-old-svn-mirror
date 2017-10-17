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
package org.apache.sling.pipes.models;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.pipes.AbstractPipeTest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Testing model initialisation
 */
public class PipeModelTest extends AbstractPipeTest {
    private static final String COMPONENT = "/content/component";
    private static final String NN_CURRENTRESOURCEPIPE = "resourceBinding";
    PipeModel model;

    @Before
    public void setup() throws PersistenceException {
        super.setup();
        Map map = new HashMap();
        map.put("jcr:primaryType","sling:Folder");
        map.put("test",PATH_APPLE);
        ResourceUtil.getOrCreateResource(context.resourceResolver(), COMPONENT, map, "sling:Folder", true);
        context.load().json("/container.json", COMPONENT + "/" + PipeModel.NN_PIPES);
        model = new PipeModel(context.resourceResolver().getResource(COMPONENT));
        model.plumber = plumber;
        model.init();
    }

    @Test
    /**
     * we've loaded under NN_PIPES all the pipes from container json (direct children): all those pipes output should be mounted
     * as outputs of the model
     */
    public void testInit(){
        assertTrue("there should be pipes outputs", model.getOutputs().size() > 0);
    }

    @Test
    /**
     * we take a special child pipe, here, named 'currentResource' that is using ${currentResource.test} binding
     * to set its input. It's a base pipe, so we expect output to be the same.
     * Model is built upon a resource where test property is set to apple resource path.
     *
     * As a result, we expect the one & only output of 'currentResource' model item to be apple resource
     */
    public void testCurrentResourceBindings() throws PersistenceException {
        Iterator<Resource> output = model.getOutputs().get(NN_CURRENTRESOURCEPIPE);
        assertTrue("resource binding pipe should have output", output.hasNext());
        assertEquals("output should be apple's resource", PATH_APPLE, output.next().getPath());
        assertFalse("there should be no other resource", output.hasNext());
    }
}