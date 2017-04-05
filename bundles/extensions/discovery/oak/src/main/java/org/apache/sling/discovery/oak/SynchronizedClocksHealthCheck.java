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
package org.apache.sling.discovery.oak;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.base.connectors.announcement.Announcement;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HealthCheck that builds on-top of DocumentNodeStore's
 * determineServerTimeDifferenceMillis method which checks how much the local
 * time differs from the DocumentStore's time. It then applies low- and
 * high-water marks to that time difference:
 * <ul>
 * <li>if the value is higher than the high-water mark (5sec by default), then
 * it issues a critical</li>
 * <li>if the value is lower than the high-water but higher than the low-water
 * mark (1sec by default), then it issues only a warn</li>
 * <li>if the value is lower than the low-water mark, then it issues only an
 * info</li>
 * </ul>
 */
@Component(immediate = true, metatype = true, label = "Apache Sling Discovery Oak Synchronized Clocks Health Check")
@Properties({
        @Property(name = HealthCheck.NAME, value = "Synchronized Clocks", description = "Health Check name", label = "Name"),
        @Property(name = HealthCheck.TAGS, unbounded = PropertyUnbounded.ARRAY, description = "Health Check tags", label = "Tags"),
        @Property(name = HealthCheck.MBEAN_NAME, value = "slingDiscoveryOakSynchronizedClocks", description = "Health Check MBean name", label = "MBean name") })
@Service(value = HealthCheck.class)
public class SynchronizedClocksHealthCheck implements HealthCheck {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String DOCUMENT_NODE_STORE_MBEAN = "org.apache.jackrabbit.oak:name=*,type=DocumentNodeStore";
    private static final String TIME_DIFF_METHOD_NAME = "determineServerTimeDifferenceMillis";

    private static final long INTRA_CLUSTER_HIGH_WATER_MARK = 5000;
    private static final long INTRA_CLUSTER_LOW_WATER_MARK = 1000;

    private static final long INTER_CLUSTER_HIGH_WATER_MARK = 10000;
    private static final long INTER_CLUSTER_LOW_WATER_MARK = 5000;

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private SlingSettingsService settingsService;

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.debug("Checking cluster internal clocks");
        try {
            final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName n = new ObjectName(DOCUMENT_NODE_STORE_MBEAN);
            Set<ObjectName> names = jmxServer.queryNames(n, null);

            if (names.size() == 0) {
                resultLog.info("Intra-cluster test n/a (No DocumentNodeStore MBean found)");
            } else {
                ObjectName firstName = names.iterator().next();
                final Object value = jmxServer.invoke(firstName, TIME_DIFF_METHOD_NAME, new Object[0], new String[0]);
                logger.debug("{} returns {}", new Object[] { firstName, TIME_DIFF_METHOD_NAME, value });
                resultLog.debug("{} returns {}", firstName, TIME_DIFF_METHOD_NAME, value);
                if (value != null && (value instanceof Long)) {
                    Long diffMillis = (Long) value;
                    if (Math.abs(diffMillis) >= INTRA_CLUSTER_HIGH_WATER_MARK) {
                        logger.warn(
                                "execute: clocks in local cluster out of sync by {}ms "
                                        + "which is equal or higher than the high-water mark of {}ms.",
                                diffMillis, INTRA_CLUSTER_HIGH_WATER_MARK);
                        resultLog.critical(
                                "Clocks heavily out of sync in local cluster: "
                                        + "time difference of this VM with DocumentStore server: "
                                        + "{}ms is equal or larger than high-water mark of {}ms",
                                diffMillis, INTRA_CLUSTER_HIGH_WATER_MARK);
                    } else if (Math.abs(diffMillis) >= INTRA_CLUSTER_LOW_WATER_MARK) {
                        logger.warn(
                                "execute: clocks in local cluster out of sync by {}ms"
                                        + "ms which is equal or higher than the low-water mark of {}ms.",
                                diffMillis, INTRA_CLUSTER_LOW_WATER_MARK);
                        resultLog.warn(
                                "Clocks noticeably out of sync in local cluster: "
                                        + "time difference of this VM with DocumentStore server: "
                                        + "{}ms is equal or larger than low-water mark of {}ms",
                                diffMillis, INTRA_CLUSTER_LOW_WATER_MARK);
                    } else {
                        logger.debug("execute: clocks in local cluster in sync. diff is {}ms"
                                + "ms which is within low-water mark of {}ms.", diffMillis, INTRA_CLUSTER_LOW_WATER_MARK);
                        resultLog.info("Clocks in sync in local cluster: time difference of this VM with DocumentStore server: "
                                + "{}ms is within low-water mark of {}ms", diffMillis, INTRA_CLUSTER_LOW_WATER_MARK);
                    }
                }
            }
        } catch (final Exception e) {
            logger.warn("execute: {}, JMX method {} invocation failed: {}",
                    new Object[] { DOCUMENT_NODE_STORE_MBEAN, TIME_DIFF_METHOD_NAME, e });
            resultLog.healthCheckError("{}, JMX method {} invocation failed: {}", DOCUMENT_NODE_STORE_MBEAN, TIME_DIFF_METHOD_NAME,
                    e);
        }

        final String slingId = settingsService == null ? "n/a" : settingsService.getSlingId();

        if (announcementRegistry == null) {
            logger.warn("execute: no announcementRegistry ({}) set", announcementRegistry);
            resultLog.warn("Cannot determine topology clocks since no announcementRegistry ({}) set", announcementRegistry);
        } else {
            final Collection<Announcement> localAnnouncements = announcementRegistry.listLocalAnnouncements();
            if (localAnnouncements.isEmpty()) {
                logger.info("execute: no topology connectors connected to local instance.");
                resultLog.info("No topology connectors connected to local instance.");
            }
            for (Announcement ann : localAnnouncements) {
                final String peerSlingId = ann.getOwnerId();
                final long originallyCreatedAt = ann.getOriginallyCreatedAt();
                final long receivedAt = ann.getReceivedAt();
                long diffMillis = Math.abs(originallyCreatedAt - receivedAt);
                if (Math.abs(diffMillis) >= INTER_CLUSTER_HIGH_WATER_MARK) {
                    logger.warn(
                            "execute: clocks between local instance (slingId: {}) and remote instance (slingId: {}) out of sync by {}ms"
                                    + "ms which is equal or higher than the high-water mark of {}ms.",
                            new Object[] { slingId, peerSlingId, diffMillis, INTER_CLUSTER_HIGH_WATER_MARK });
                    resultLog.critical(
                            "Clocks heavily out of sync between local instance (slingId: {}) and remote instance (slingId: {}): "
                                    + "by {}ms which is equal or larger than high-water mark of {}ms",
                            new Object[] { slingId, peerSlingId, diffMillis, INTER_CLUSTER_HIGH_WATER_MARK });
                } else if (Math.abs(diffMillis) >= INTER_CLUSTER_LOW_WATER_MARK) {
                    logger.warn(
                            "execute: clocks out of sync between local instance (slingId: {}) and remote instance (slingId: {}) by {}ms "
                                    + "ms which is equal or higher than the low-water mark of {}ms.",
                            new Object[] { slingId, peerSlingId, diffMillis, INTER_CLUSTER_HIGH_WATER_MARK });
                    resultLog.warn(
                            "Clocks noticeably out of sync between local instance (slingId: {}) and remote instance (slingId: {}): "
                            + "by {}ms which is equal or larger than low-water mark of {}ms",
                            new Object[] { slingId, peerSlingId, diffMillis, INTER_CLUSTER_HIGH_WATER_MARK });
                } else {
                    logger.debug(
                            "execute: clocks in sync between local instance (slingId: {}) and remote instance (slingId: {}). "
                            + "diff is {}ms which is within low-water mark of {}ms.",
                            new Object[] { slingId, peerSlingId, diffMillis, INTER_CLUSTER_HIGH_WATER_MARK });
                    resultLog.info(
                            "Clocks in sync between local instance (slingId: {}) and remote instance (slingId: {}): "
                            + "diff is {}ms which is within low-water mark of {}ms",
                            new Object[] { slingId, peerSlingId, diffMillis, INTER_CLUSTER_HIGH_WATER_MARK });
                }
            }
        }

        return new Result(resultLog);
    }

}
