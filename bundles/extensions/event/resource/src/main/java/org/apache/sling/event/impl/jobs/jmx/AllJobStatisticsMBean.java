/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.event.impl.jobs.jmx;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.jmx.StatisticsMBean;

@Component
@Service(value = StatisticsMBean.class)
@Properties(@Property(name = "jmx.objectname", value = "org.apache.sling:type=queues,name=AllQueues"))
public class AllJobStatisticsMBean extends AbstractJobStatistics {

    private static final long TTL = 1000L;
    private long agregateStatisticsTTL = 0L;
    private Statistics aggregateStatistics;
    @Reference
    private JobManager jobManager;

    /**
     * @return the aggregate stats from the job manager.
     */
    @Override
    protected Statistics getStatistics() {
        if (System.currentTimeMillis() > agregateStatisticsTTL) {
            aggregateStatistics = jobManager.getStatistics();
            agregateStatisticsTTL = System.currentTimeMillis() + TTL;
        }
        return aggregateStatistics;
    }

    @Override
    public String getName() {
        return "All Queues";
    }

}
