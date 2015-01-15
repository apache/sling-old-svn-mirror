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
package org.apache.sling.datasource;

import java.sql.Connection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Filter;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import static org.apache.commons.beanutils.BeanUtils.getProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(PaxExam.class)
public class DataSourceIT extends DataSourceTestBase{

    static {
        //paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    String PID = "org.apache.sling.datasource.DataSourceFactory";

    @Inject
    ConfigurationAdmin ca;

    @SuppressWarnings("unchecked")
    @Test
    public void testDataSourceAsService() throws Exception{
        Configuration config = ca.createFactoryConfiguration(PID, null);
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("url","jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        p.put("datasource.name","test");
        p.put("initialSize","5");
        p.put("defaultAutoCommit","default");
        p.put("defaultReadOnly","false");
        p.put("datasource.svc.properties",new String[]{
                "initSQL=SELECT 1",
        });
        p.put("maxActive",70);
        config.update(p);

        Filter filter = context.createFilter("(&(objectclass=javax.sql.DataSource)(datasource.name=test))");
        ServiceTracker<DataSource, DataSource> st =
                new ServiceTracker<DataSource, DataSource>(context, filter, null);
        st.open();

        DataSource ds = st.waitForService(10000);
        assertNotNull(ds);

        Connection conn = ds.getConnection();
        assertNotNull(conn);

        //Cannot access directly so access via reflection
        assertEquals("70", getProperty(ds, "poolProperties.maxActive"));
        assertEquals("5", getProperty(ds, "poolProperties.initialSize"));
        assertEquals("SELECT 1", getProperty(ds, "poolProperties.initSQL"));
        assertEquals("false", getProperty(ds, "poolProperties.defaultReadOnly"));
        assertNull(getProperty(ds, "poolProperties.defaultAutoCommit"));

        config = ca.listConfigurations("(datasource.name=test)")[0];
        Dictionary dic = config.getProperties();
        dic.put("defaultReadOnly", Boolean.TRUE);
        config.update(dic);

        TimeUnit.MILLISECONDS.sleep(100);
        assertEquals("true", getProperty(ds, "poolProperties.defaultReadOnly"));
    }

}
