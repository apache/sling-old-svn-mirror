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
package org.apache.sling.discovery.base.connectors.announcement;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.NonLocalInstanceDescription;

/**
 * An announcement is the information exchanged by the topology connector and
 * contains all clusters and instances which both the topology connector client
 * and servlet see (in their part before joining the two worlds).
 * <p>
 * An announcement is exchanged in json format and carries a timeout.
 */
public class Announcement {

    /** the protocol version this announcement currently represents. Mismatching protocol versions are
     * used to detect incompatible topology connectors
     */
    private final static int PROTOCOL_VERSION = 1;

    /** the sling id of the owner of this announcement. the owner is where this announcement comes from **/
    private final String ownerId;

    /** announcement protocol version **/
    private final int protocolVersion;

    /** the local cluster view **/
    private ClusterView localCluster;

    /** the incoming instances **/
    private List<Announcement> incomings = new LinkedList<Announcement>();

    /** whether or not this annoucement was inherited (response of a connect) or incoming (the connect) **/
    private boolean inherited = false;

    /** some information about the server where this announcement came from **/
    private String serverInfo;

    /** whether or not this announcement represents a loop detected in the topology connectors **/
    private boolean loop = false;

    /** SLING-3382: Sets the backoffInterval which the connector servlets passes back to the client to use as the next heartbeatInterval **/
    private long backoffInterval = -1;

    /** SLING-3382: the resetBackoff flag is sent from client to server and indicates that the client wants to start from (backoff) scratch **/
    private boolean resetBackoff = false;
    
    private long originallyCreatedAt = -1;
    
    private long receivedAt = System.currentTimeMillis();
    
    private static final JsonReaderFactory jsonReaderFactory;
    static {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("org.apache.johnzon.supports-comments", true);
        jsonReaderFactory = Json.createReaderFactory(config);
    }

    public Announcement(final String ownerId) {
        this(ownerId, PROTOCOL_VERSION);
    }

    public Announcement(final String ownerId, int protocolVersion) {
        if (ownerId==null || ownerId.length()==0) {
            throw new IllegalArgumentException("ownerId must not be null or empty");
        }
        this.ownerId = ownerId;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public String toString() {
        StringBuilder incomingList = new StringBuilder();
        for (Iterator<Announcement> it = incomings.iterator(); it.hasNext();) {
            Announcement anIncomingAnnouncement = it.next();
            if (incomingList.length()!=0) {
                incomingList.append(", ");
            }
            incomingList.append(anIncomingAnnouncement);
        }
        return "Announcement[ownerId="+getOwnerId()+
                ", protocolVersion="+protocolVersion+
                ", inherited="+isInherited()+
                ", loop="+loop+
                ", incomings="+incomingList+"]";
    }

    /** check whether this is announcement contains the valid protocol version **/
    public boolean isCorrectVersion() {
        return (protocolVersion==PROTOCOL_VERSION);
    }

    /** check whether this is a valid announcement, containing the minimal information **/
    public boolean isValid() {
        if (ownerId==null || ownerId.length()==0) {
            return false;
        }
        if (loop) {
            return true;
        }
        if (!isCorrectVersion()) {
            return false;
        }
        if (localCluster==null) {
            return false;
        }
        try{
            List<InstanceDescription> instances = localCluster.getInstances();
            if (instances==null || instances.size()==0) {
                return false;
            }
            boolean isOwnerMemberOfLocalCluster = false;
            for (Iterator<InstanceDescription> it = instances.iterator(); it.hasNext();) {
                InstanceDescription instanceDescription = it.next();
                if (instanceDescription.getSlingId().equals(ownerId)) {
                    isOwnerMemberOfLocalCluster = true;
                }
            }
            if (!isOwnerMemberOfLocalCluster) {
                return false;
            }
        } catch(Exception ise) {
            return false;
        }
        return true;
    }

    /** set the inherited flag - if true this means this announcement is the response of a topology connect **/
    public void setInherited(final boolean inherited) {
        this.inherited = inherited;
    }

    /** Returns the inherited flag - if true this means that this announcement is the response of a topology connect **/
    public boolean isInherited() {
        return inherited;
    }

    /** Sets the loop falg - set true when this announcement should represent a loop detected in the topology connectors **/
    public void setLoop(final boolean loop) {
        this.loop = loop;
    }
    
    /** Sets the backoffInterval which the connector servlets passes back to the client to use as the next heartbeatInterval **/
    public void setBackoffInterval(long backoffInterval) {
        this.backoffInterval = backoffInterval;
    }
    
    /** Gets the backoffInterval which the connector servlets passes back to the client to use as the next heartbeatInterval **/
    public long getBackoffInterval() {
        return this.backoffInterval;
    }
    
    public long getOriginallyCreatedAt() {
        return this.originallyCreatedAt;
    }
    
    public long getReceivedAt() {
        return this.receivedAt;
    }
    
    /** sets the resetBackoff flag **/
    public void setResetBackoff(boolean resetBackoff) {
        this.resetBackoff = resetBackoff;
    }
    
    /** gets the resetBackoff flag **/
    public boolean getResetBackoff() {
        return resetBackoff;
    }

    /** Returns the loop flag - set when this announcement represents a loop detected in the topology connectors **/
    public boolean isLoop() {
        return loop;
    }

    /** Returns the protocolVersion of this announcement **/
    public int getProtocolVersion() {
        return protocolVersion;
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
    
    /** Convert this announcement into a json object **/
    private JsonObject asJSONObject(boolean filterTimes) {
        JsonObjectBuilder announcement = Json.createObjectBuilder();
        announcement.add("ownerId", ownerId);
        announcement.add("protocolVersion", protocolVersion);
        // SLING-3389: leaving the 'created' property in the announcement
        // for backwards compatibility!
        if (!filterTimes) {
            announcement.add("created", System.currentTimeMillis());
        }
        announcement.add("inherited", inherited);
        if (loop) {
            announcement.add("loop", loop);
        }
        if (serverInfo != null) {
            announcement.add("serverInfo", serverInfo);
        }
        if (localCluster!=null) {
            announcement.add("localClusterView", asJSON(localCluster));
        }
        if (!filterTimes && backoffInterval>0) {
            announcement.add("backoffInterval", backoffInterval);
        }
        if (resetBackoff) {
            announcement.add("resetBackoff", resetBackoff);
        }
        JsonArrayBuilder incomingAnnouncements = Json.createArrayBuilder();
        for (Iterator<Announcement> it = incomings.iterator(); it.hasNext();) {
            Announcement incoming = it.next();
            incomingAnnouncements.add(incoming.asJSONObject(filterTimes));
        }
        announcement.add("topologyAnnouncements", incomingAnnouncements);
        return announcement.build();
    }

    /** Create an announcement form json **/
    public static Announcement fromJSON(final String topologyAnnouncementJSON) {
        
        JsonObject announcement = jsonReaderFactory.createReader(new StringReader(topologyAnnouncementJSON)).readObject();
        final String ownerId = announcement.getString("ownerId");
        final int protocolVersion;
        if (!announcement.containsKey("protocolVersion")) {
            protocolVersion = -1;
        } else {
            protocolVersion = announcement.getInt("protocolVersion");
        }
        final Announcement result = new Announcement(ownerId, protocolVersion);
        if (announcement.containsKey("created")) {
            result.originallyCreatedAt = announcement.getJsonNumber("created").longValue();
        }
        if (announcement.containsKey("backoffInterval")) {
            long backoffInterval = announcement.getJsonNumber("backoffInterval").longValue();
            result.backoffInterval = backoffInterval;
        }
        if (announcement.containsKey("resetBackoff")) {
            boolean resetBackoff = announcement.getBoolean("resetBackoff");
            result.resetBackoff = resetBackoff;
        }
        if (announcement.containsKey("loop") && announcement.getBoolean("loop")) {
            result.setLoop(true);
            return result;
        }
        if (announcement.containsKey("localClusterView"))
        {
            final String localClusterViewJSON = asJSON(announcement
                    .getJsonObject("localClusterView"));
            
            final ClusterView localClusterView = asClusterView(localClusterViewJSON);
    
            result.setLocalCluster(localClusterView);
        }

        if (announcement.containsKey("inherited")) {
            final Boolean inherited = announcement.getBoolean("inherited");
            result.inherited = inherited;
        }
        if (announcement.containsKey("serverInfo")) {
            String serverInfo = announcement.getString("serverInfo");
            result.serverInfo = serverInfo;
        }

        final JsonArray subAnnouncements = announcement
                .getJsonArray("topologyAnnouncements");
        
        for (int i = 0; i < subAnnouncements.size(); i++) {
            String subAnnouncementJSON = subAnnouncements.get(i).toString();
            result.addIncomingTopologyAnnouncement(fromJSON(subAnnouncementJSON));
        }
        return result;
    }

    /** create a clusterview from json **/
    private static ClusterView asClusterView(final String localClusterViewJSON) {
        JsonObject obj = jsonReaderFactory.createReader(new StringReader(localClusterViewJSON)).readObject();
        DefaultClusterView clusterView = new DefaultClusterView(
                obj.getString("id"));
        JsonArray instancesObj = obj.getJsonArray("instances");

        for (int i = 0; i < instancesObj.size(); i++) {
            JsonObject anInstance = instancesObj.getJsonObject(i);
            clusterView.addInstanceDescription(asInstance(anInstance));
        }

        return clusterView;
    }

    /** convert a clusterview into json **/
    private static JsonObject asJSON(final ClusterView clusterView) {
        JsonObjectBuilder obj = Json.createObjectBuilder();
        obj.add("id", clusterView.getId());
        JsonArrayBuilder instancesObj = Json.createArrayBuilder();
        List<InstanceDescription> instances = clusterView.getInstances();
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instanceDescription = it.next();
            instancesObj.add(asJSON(instanceDescription));
        }
        obj.add("instances", instancesObj);
        return obj.build();
    }

    /** create an instancedescription from json **/
    private static DefaultInstanceDescription asInstance(final JsonObject anInstance) {
        final boolean isLeader = anInstance.getBoolean("isLeader");
        final String slingId = anInstance.getString("slingId");

        final JsonObject propertiesObj = anInstance.getJsonObject("properties");
        Iterator<String> it = propertiesObj.keySet().iterator();
        Map<String, String> properties = new HashMap<String, String>();
        while (it.hasNext()) {
            String key = it.next();
            properties.put(key, propertiesObj.getString(key));
        }

        NonLocalInstanceDescription instance = new NonLocalInstanceDescription(
                null, isLeader, slingId, properties);
        return instance;
    }

    /** convert an instance description into a json object **/
    private static JsonObject asJSON(final InstanceDescription instanceDescription) {
        JsonObjectBuilder obj = Json.createObjectBuilder();
        obj.add("slingId", instanceDescription.getSlingId());
        obj.add("isLeader", instanceDescription.isLeader());
        ClusterView cluster = instanceDescription.getClusterView();
        if (cluster != null) {
            obj.add("cluster", cluster.getId());
        }
        JsonObjectBuilder propertiesObj = Json.createObjectBuilder();
        Map<String, String> propertiesMap = instanceDescription.getProperties();
        for (Iterator<Entry<String, String>> it = propertiesMap.entrySet()
                .iterator(); it.hasNext();) {
            Entry<String, String> entry = it.next();
            propertiesObj.add(entry.getKey(), entry.getValue());
        }
        obj.add("properties", propertiesObj);
        return obj.build();
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
    public String asJSON() {
        return asJSON(asJSONObject(false));
    }
    
    private static String asJSON(JsonValue json) {
        StringWriter writer = new StringWriter();
        Json.createGenerator(writer).write(json).close();
        return writer.toString();
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
            throws PersistenceException {
        Resource announcementChildResource = announcementsResource.getChild(getPrimaryKey());
        
        // SLING-2967 used to introduce 'resetting the created time' here
        // in order to become machine-clock independent.
        // With introduction of SLING-3389, where we dont store any
        // announcement-heartbeat-dates anymore at all, this resetting here
        // became unnecessary.
        
        final String announcementJson = asJSON();
		if (announcementChildResource==null) {
            final ResourceResolver resourceResolver = announcementsResource.getResourceResolver();
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("topologyAnnouncement", announcementJson);
            resourceResolver.create(announcementsResource, getPrimaryKey(), properties);
        } else {
            final ModifiableValueMap announcementChildMap = announcementChildResource.adaptTo(ModifiableValueMap.class);
            announcementChildMap.put("topologyAnnouncement", announcementJson);
        }
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

    /**
     * Compare this Announcement with another one, ignoring the 'created'
     * property - which gets added to the JSON object automatically due
     * to SLING-3389 wire-backwards-compatibility - and backoffInterval
     * introduced as part of SLING-3382
     */
    public boolean correspondsTo(Announcement announcement) {
        final JsonObject myJson = asJSONObject(true);
        final JsonObject otherJson = announcement.asJSONObject(true);
        return asJSON(myJson).equals(asJSON(otherJson));
    }

    public void registerPing(Announcement incomingAnnouncement) {
        originallyCreatedAt = incomingAnnouncement.originallyCreatedAt;
        receivedAt = incomingAnnouncement.receivedAt;
    }

}
