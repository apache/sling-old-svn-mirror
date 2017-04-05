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
package org.apache.sling.caconfig.management.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;

/**
 * Test {@link ConfigurationManagerImpl} with custom persistence.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplCustomPersistenceTest extends ConfigurationManagerImplTest {
    
    @Before
    public void setUpCustomPersistence() {
        // custom strategy which redirects all config resources to a jcr:content subnode
        context.registerService(ConfigurationPersistenceStrategy2.class,
                new CustomConfigurationPersistenceStrategy(), Constants.SERVICE_RANKING, 2000);
    }

    @Override
    protected String getConfigResolvePath(String path) {
        return replaceBucketName(path) + "/jcr:content";
    }
    
    @Override
    protected String getConfigPersistPath(String path) {
        return path + "/jcr:content";
    }
    
    @Override
    protected String getConfigCollectionParentResolvePath(String path) {
        return replaceBucketName(path);
    }
    
    @Override
    protected String getConfigCollectionParentPersistPath(String path) {
        return path;
    }
    
    @Override
    protected String getConfigCollectionItemResolvePath(String path) {
        return replaceBucketName(path) + "/jcr:content";
    }
    
    @Override
    protected String getConfigCollectionItemPersistPath(String path) {
        return path + "/jcr:content";
    }

    private String replaceBucketName(String path) {
        return StringUtils.replace(path, "/sling:configs/", "/settings/");
    }

    @Override
    protected String[] getAlternativeBucketNames() {
        return new String[] { "settings" };
    }

}
