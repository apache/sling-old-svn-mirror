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
package org.apache.sling.event.jobs.jmx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.StandardMBean;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.jobs.QueueStatusEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@Component(immediate = true, enabled = false)
// disabled while dev in progress
@Service(value = { QueuesMBean.class, EventHandler.class })
@Properties({
        @Property(name = "jmx.objectname", propertyPrivate = true, value = "org.apache.sling.event.Queues;type=Statistics"),
        @Property(name = "event.topics", propertyPrivate = true, value = { QueueStatusEvent.TOPIC }) })
public class QueuesMBeanImpl extends StandardMBean implements QueuesMBean,
        EventHandler {

    private Map<String, QueueMBeanImpl> queues = new ConcurrentHashMap<String, QueueMBeanImpl>();

    public QueuesMBeanImpl() {
        super(QueuesMBean.class, false);
    }

    public void handleEvent(Event event) {
        if (event instanceof QueueStatusEvent) {
            QueueStatusEvent e = (QueueStatusEvent) event;
            if (e.isNew()) {
                bindQueueMBean(e);
            } else if (e.isRemoved()) {
                unbindQueueMBean(e);
            } else {
                updateQueueMBean(e);
            }
        }
    }

    private void updateQueueMBean(QueueStatusEvent e) {
        QueueMBeanImpl queueMBean = queues.get(e.getQueue().getName());
        if (queueMBean != null) {
            queueMBean.notifyUpdate(e.getQueue());
        }
    }

    private void unbindQueueMBean(QueueStatusEvent e) {
        QueueMBeanImpl queueMBean = queues.get(e.getQueue().getName());
        if (queueMBean != null) {
            queueMBean.notifyRemove();
        }
    }

    private void bindQueueMBean(QueueStatusEvent e) {
        QueueMBeanImpl queueMBean = queues.get(e.getQueue().getName());
        if (queueMBean != null) {
            queueMBean.notifyRemove();
        }
        queueMBean = new QueueMBeanImpl(e.getQueue());

    }

}
