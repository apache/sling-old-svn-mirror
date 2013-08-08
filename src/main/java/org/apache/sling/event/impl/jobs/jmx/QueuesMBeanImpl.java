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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.jmx.QueuesMBean;
import org.apache.sling.event.jobs.jmx.StatisticsMBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@Component
@Service(value = { QueuesMBean.class })
@Property(name = "jmx.objectname", value = "org.apache.sling:type=queues,name=QueueNames")
public class QueuesMBeanImpl extends StandardEmitterMBean implements QueuesMBean {

    private static final String QUEUE_NOTIFICATION = "org.apache.sling.event.queue";
    private static final String[] NOTIFICATION_TYPES = { QUEUE_NOTIFICATION };
    private Map<String, QueueMBeanHolder> queues = new ConcurrentHashMap<String, QueueMBeanHolder>();
    private String[] names;
    private AtomicLong sequence = new AtomicLong(System.currentTimeMillis());
    private BundleContext bundleContext;

    class QueueMBeanHolder {

        QueueMBeanHolder(String name, QueueMBeanImpl queueMBean,
                ServiceRegistration registration) {
            this.name = name;
            this.queueMBean = queueMBean;
            this.registration = registration;
        }

        QueueMBeanImpl queueMBean;
        ServiceRegistration registration;
        String name;

    }

    public QueuesMBeanImpl() {
        super(QueuesMBean.class, false, new NotificationBroadcasterSupport(
                new MBeanNotificationInfo(NOTIFICATION_TYPES,
                        Notification.class.getName(),
                        "Notifications about queues")));
    }

    @Activate
    public void activate(final BundleContext bc) {
        bundleContext = bc;
    }

    @Deactivate
    public void deactivate() {
        bundleContext = null;
    }

    public void sendEvent(final QueueStatusEvent e) {
        if (e.isNew()) {
            bindQueueMBean(e);
        } else if (e.isRemoved()) {
            unbindQueueMBean(e);
        } else {
            updateQueueMBean(e);
        }
    }

    private void updateQueueMBean(QueueStatusEvent e) {
        QueueMBeanHolder queueMBeanHolder = queues.get(e.getQueue().getName());
        if (queueMBeanHolder != null) {
            String[] oldQueue = getQueueNames();
            names = null;
            this.sendNotification(new AttributeChangeNotification(this,
                    sequence.incrementAndGet(), System.currentTimeMillis(),
                    "Queue " + e.getQueue().getName() + " updated ",
                    "queueNames", "String[]", oldQueue, getQueueNames()));
        }
    }

    private void unbindQueueMBean(QueueStatusEvent e) {
        QueueMBeanHolder queueMBeanHolder = queues.get(e.getOldQueue().getName());
        if (queueMBeanHolder != null) {
            removeAndNotify(queueMBeanHolder);
        }
    }

    private void bindQueueMBean(QueueStatusEvent e) {
        QueueMBeanHolder queueMBeanHolder = queues.get(e.getQueue().getName());
        if (queueMBeanHolder != null) {
            removeAndNotify(queueMBeanHolder);
        }
        addAndNotify(e.getQueue());
    }

    private void addAndNotify(Queue queue) {
        String[] oldQueue = getQueueNames();
        QueueMBeanHolder queueMBeanHolder = add(queue);
        names = null;
        this.sendNotification(new AttributeChangeNotification(this, sequence
                .incrementAndGet(), System.currentTimeMillis(), "Queue "
                + queueMBeanHolder.name + " added ", "queueNames", "String[]",
                oldQueue, getQueueNames()));
    }

    private void removeAndNotify(QueueMBeanHolder queueMBeanHolder) {
        String[] oldQueue = getQueueNames();
        remove(queueMBeanHolder);
        names = null;
        this.sendNotification(new AttributeChangeNotification(this, sequence
                .incrementAndGet(), System.currentTimeMillis(), "Queue "
                + queueMBeanHolder.name + " removed ", "queueNames",
                "String[]", oldQueue, getQueueNames()));
    }

    private QueueMBeanHolder add(Queue queue) {
        QueueMBeanImpl queueMBean = new QueueMBeanImpl(queue);
        ServiceRegistration serviceRegistration = bundleContext
                .registerService(StatisticsMBean.class.getName(), queueMBean,
                        createProperties(
                                "jmx.objectname","org.apache.sling:type=queues,name="+queue.getName(),
                                Constants.SERVICE_DESCRIPTION, "QueueMBean for queue "+queue.getName(),
                                Constants.SERVICE_VENDOR, "The Apache Software Foundation"));
        QueueMBeanHolder queueMBeanHolder = new QueueMBeanHolder(
                queue.getName(), queueMBean, serviceRegistration);
        queues.put(queueMBeanHolder.name, queueMBeanHolder);
        return queueMBeanHolder;
    }

    private Dictionary<String, Object> createProperties(Object ... values) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        for ( int i = 0; i < values.length; i+=2) {
            props.put((String) values[i], values[i+1]);
        }
        return props;
    }

    private void remove(QueueMBeanHolder queueMBeanHolder) {
        queueMBeanHolder.registration.unregister();
        queues.remove(queueMBeanHolder.name);
    }

    @Override
    public String[] getQueueNames() {
        if (names == null) {
            List<String> lnames = new ArrayList<String>(queues.keySet());
            Collections.sort(lnames);
            names = lnames.toArray(new String[lnames.size()]);
        }
        return names;
    }

}
