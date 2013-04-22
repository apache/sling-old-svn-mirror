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
package org.apache.sling.discovery.impl.topology.announcement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorClientInformation.OriginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An announcement is the information exchanged by the topology connector and
 * contains all clusters and instances which both the topology connector client
 * and servlet see (in their part before joining the two worlds).
 * <p>
 * An announcement is exchanged in json format and carries a timeout.
 */
public class Announcement {

    private final static Logger logger = LoggerFactory
            .getLogger(Announcement.class);

    /** the sling id of the owner of this announcement. the owner is where this announcement comes from **/
    private final String ownerId;
    
    /** the local cluster view **/
    private ClusterView localCluster;
    
    /** the incoming instances **/
    private List<Announcement> incomings = new LinkedList<Announcement>();
    
    /** time this announcement was received **/
    private long created = System.currentTimeMillis();

    /** whether or not this annoucement was inherited (response of a connect) or incoming (the connect) **/
    private boolean inherited = false;

    /** some information about where the corresponding topology connector came from **/
    private OriginInfo originInfo = null;

    /** some information about the server where this announcement came from **/
    private String serverInfo;

    public Announcement(final String ownerId) {
        this.ownerId = ownerId;
    }

    /** set the inherited flag - if true this means this announcement is the response of a topology connect **/
    public void setInherited(final boolean inherited) {
        this.inherited = inherited;
    }

    /** Returns the inherited flag - if true this means that this announcement is the response of a topology connect **/
    public boolean isInherited() {
        return inherited;
    }

    /** sets the information where the corresponding topology connector came from **/
    public void setOriginInfo(final OriginInfo originInfo) {
        this.originInfo = originInfo;
    }

    /** where the corresponding topology connector came from **/
    public OriginInfo getOriginInfo() {
        return originInfo;
    }

    /** sets the information about the server where this announcement came from **/
    public void setServerInfo(final String serverInfo) {
        this.serverInfo = serverInfo;
    }

    /** the information about the server where this announcement came from **/
    public String getServerInfo() {
        return serverInfo;
    }

    /** 
     * Returns the slingid of the owner of this announcement.
     * <p>
     * The owner is the instance which initiated the topology connection
     */
    public String getOwnerId() {
        return ownerId;
    }

    /** Checks whether this announcement has expired or not **/
    public boolean hasExpired(final Config config) {
        final long now = System.currentTimeMillis();
        final long diff = now - created;
        return diff > 1000 * config.getHeartbeatTimeout();
    }

    /** Convert this announcement into a json object **/
    public JSONObject asJSONObject() throws JSONException {
        JSONObject announcement = new JSONObject();
        announcement.put("ownerId", ownerId);
        announcement.put("created", created);
        announcement.put("inherited", inherited);
        if (originInfo != null) {
            announcement.put("originInfo", originInfo.toString());
        }
        if (serverInfo != null) {
            announcement.put("serverInfo", serverInfo);
        }
        announcement.put("localClusterView", asJSON(localCluster));
        JSONArray incomingAnnouncements = new JSONArray();
        for (Iterator<Announcement> it = incomings.iterator(); it.hasNext();) {
            Announcement incoming = it.next();
            incomingAnnouncements.put(incoming.asJSONObject());
        }
        announcement.put("topologyAnnouncements", incomingAnnouncements);
        return announcement;
    }

    /** Create an announcement form json **/
    public static Announcement fromJSON(final String topologyAnnouncementJSON)
            throws JSONException {
        JSONObject announcement = new JSONObject(topologyAnnouncementJSON);
        final String ownerId = announcement.getString("ownerId");
        final String localClusterViewJSON = announcement
                .getString("localClusterView");
        final ClusterView localClusterView = asClusterView(localClusterViewJSON);
        final JSONArray subAnnouncements = announcement
                .getJSONArray("topologyAnnouncements");

        final Announcement result = new Announcement(ownerId);
        final Long created = announcement.getLong("created");
        if (announcement.has("inherited")) {
            final Boolean inherited = announcement.getBoolean("inherited");
            result.inherited = inherited;
        }
        if (announcement.has("originInfo")) {
            String originInfoStr = announcement.getString("originInfo");
            try {
                result.originInfo = OriginInfo.valueOf(originInfoStr);
            } catch (Exception e) {
                logger.warn("Could not translate " + originInfoStr
                        + " into OriginInfo enumeration: " + e, e);
            }
        }
        if (announcement.has("serverInfo")) {
            String serverInfo = announcement.getString("serverInfo");
            result.serverInfo = serverInfo;
        }
        result.created = created;
        result.setLocalCluster(localClusterView);
        for (int i = 0; i < subAnnouncements.length(); i++) {
            String subAnnouncementJSON = subAnnouncements.getString(i);
            result.addIncomingTopologyAnnouncement(fromJSON(subAnnouncementJSON));
        }
        return result;
    }

    /** create a clusterview from json **/
    private static ClusterView asClusterView(final String localClusterViewJSON)
            throws JSONException {
        JSONObject obj = new JSONObject(localClusterViewJSON);
        DefaultClusterViewImpl clusterView = new DefaultClusterViewImpl(
                obj.getString("id"));
        JSONArray instancesObj = obj.getJSONArray("instances");

        for (int i = 0; i < instancesObj.length(); i++) {
            JSONObject anInstance = instancesObj.getJSONObject(i);
            clusterView.addInstanceDescription(asInstance(anInstance));
        }

        return clusterView;
    }

    /** convert a clusterview into json **/
    private static JSONObject asJSON(final ClusterView clusterView)
            throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", clusterView.getId());
        JSONArray instancesObj = new JSONArray();
        List<InstanceDescription> instances = clusterView.getInstances();
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instanceDescription = it.next();
            instancesObj.put(asJSON(instanceDescription));
        }
        obj.put("instances", instancesObj);
        return obj;
    }

    /** create an instancedescription from json **/
    private static DefaultInstanceDescriptionImpl asInstance(
            final JSONObject anInstance) throws JSONException {
        final boolean isLeader = anInstance.getBoolean("isLeader");
        final String slingId = anInstance.getString("slingId");

        final JSONObject propertiesObj = anInstance.getJSONObject("properties");
        Iterator<String> it = propertiesObj.keys();
        Map<String, String> properties = new HashMap<String, String>();
        while (it.hasNext()) {
            String key = it.next();
            properties.put(key, propertiesObj.getString(key));
        }

        IncomingInstanceDescription instance = new IncomingInstanceDescription(
                null, isLeader, slingId, properties);
        return instance;
    }

    /** convert an instance description into a json object **/
    private static JSONObject asJSON(final InstanceDescription instanceDescription)
            throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("slingId", instanceDescription.getSlingId());
        obj.put("isLeader", instanceDescription.isLeader());
        ClusterView cluster = instanceDescription.getClusterView();
        if (cluster != null) {
            obj.put("cluster", cluster.getId());
        }
        JSONObject propertiesObj = new JSONObject();
        Map<String, String> propertiesMap = instanceDescription.getProperties();
        for (Iterator<Entry<String, String>> it = propertiesMap.entrySet()
                .iterator(); it.hasNext();) {
            Entry<String, String> entry = it.next();
            propertiesObj.put(entry.getKey(), entry.getValue());
        }
        obj.put("properties", propertiesObj);
        return obj;
    }

    /** sets the local clusterview **/
    public void setLocalCluster(ClusterView localCluster) {
        this.localCluster = localCluster;
    }

    /** adds an incoming announcement to this announcement **/
    public void addIncomingTopologyAnnouncement(
            Announcement incomingTopologyAnnouncement) {
        incomings.add(incomingTopologyAnnouncement);
    }

    /** Convert this announcement into json **/
    public String asJSON() throws JSONException {
        return asJSONObject().toString();
    }

    /** the key which is unique to this announcement **/
    public String getPrimaryKey() {
        return ownerId;
    }

    /** Returns the list of instances that are contained in this announcement **/
    public Collection<InstanceDescription> listInstances() {
        Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();
        instances.addAll(localCluster.getInstances());

        for (Iterator<Announcement> it = incomings.iterator(); it.hasNext();) {
            Announcement incomingAnnouncement = it.next();
            instances.addAll(incomingAnnouncement.listInstances());
        }
        return instances;
    }

    /** 
     * Persists this announcement using the given 'announcements' resource,
     * under which a node with the primary key is created
     **/
    public void persistTo(Resource announcementsResource)
            throws RepositoryException, JSONException {
        Node node = announcementsResource.adaptTo(Node.class);
        if (node.hasNode(getPrimaryKey())) {
            node = node.getNode(getPrimaryKey());
        } else {
            node = node.addNode(getPrimaryKey());
        }

        node.setProperty("topologyAnnouncement", asJSON());
    }

    /** 
     * Remove all announcements that match the given owner Id
     */
    public void removeInherited(final String ownerId) {
        for (Iterator<Announcement> it = incomings.iterator(); it.hasNext();) {
            Announcement anIncomingAnnouncement = it.next();
            if (anIncomingAnnouncement.isInherited()
                    && anIncomingAnnouncement.getOwnerId().equals(ownerId)) {
                // then filter this
                it.remove();
            }

        }
    }

}
