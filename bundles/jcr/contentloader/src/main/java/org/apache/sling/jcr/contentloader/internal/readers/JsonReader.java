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
package org.apache.sling.jcr.contentloader.internal.readers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.ContentReader;

/**
 * The <code>JsonReader</code> Parses a Json document on content load and creates the
 * corresponding node structure with properties. Will not update protected nodes and
 * properties like rep:Policy and children.
 *
 * <pre>
 * Nodes, Properties and in fact complete subtrees may be described in JSON files
 * using the following skeleton structure (see http://www.json.org for information
 * on the syntax of JSON) :
 *
 * # the name of the node is taken from the name of the file without the .json ext.
 *   {
 *
 *     # optional primary node type, default &quot;nt:unstructured&quot;
 *     &quot;jcr:primaryType&quot;:&quot;sling:ScriptedComponent&quot;,
 *     # optional mixin node types as array
 *     &quot;jcr:mixinTypes&quot;: [ ],
 *
 *
 *       # &quot;properties&quot; are added as key value pairs, the name of the key being the name
 *       # of the property. The value is either the string property value, array for
 *       # multi-values or an object whose value[s] property denotes the property
 *       # value(s) and whose type property denotes the property type
 *       &quot;sling:contentClass&quot;: &quot;com.day.sling.jcr.test.Test&quot;,
 *       &quot;sampleMulti&quot;: [ &quot;v1&quot;, &quot;v2&quot; ],
 *       &quot;sampleStruct&quot;: 1,
 *       &quot;sampleStructMulti&quot;: [ 1, 2, 3 ],
 *
 *       # reference properties start with jcr:reference
 *       &quot;jcr:reference:sampleReference&quot;: &quot;/test/content&quot;,
 *
 *       # path propertie start with jcr:path
 *       &quot;jcr:path:sampleReference&quot;: &quot;/test/path&quot;,
 *
 *       # nested nodes are added as nested maps.
 *     &quot;sling:scripts&quot;:  {
 *
 *         &quot;jcr:primaryType&quot;: &quot;sling:ScriptList&quot;,
 *         &quot;script1&quot; :{
 *             &quot;primaryNodeType&quot;: &quot;sling:Script&quot;,
 *               &quot;sling:name&quot;: &quot;/test/content/jsp/start.jsp&quot;,
 *             &quot;sling:type&quot;: &quot;jsp&quot;,
 *             &quot;sling:glob&quot;: &quot;*&quot;
 *         }
 *     }
 *   }
 *
 * </pre>
 */
@Component
@Service
@Properties({
    @Property(name = ContentReader.PROPERTY_EXTENSIONS, value = "json"),
    @Property(name = ContentReader.PROPERTY_TYPES, value = "application/json")
})
public class JsonReader implements ContentReader {

    private static final Pattern jsonDate = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}[-+]{1}[0-9]{2}[:]{0,1}[0-9]{2}$");
    private static final String REFERENCE = "jcr:reference:";
    private static final String PATH = "jcr:path:";
    private static final String NAME = "jcr:name:";
    private static final String URI = "jcr:uri:";

    protected static final Set<String> ignoredNames = new HashSet<String>();
    static {
        ignoredNames.add("jcr:primaryType");
        ignoredNames.add("jcr:mixinTypes");
        ignoredNames.add("jcr:uuid");
        ignoredNames.add("jcr:baseVersion");
        ignoredNames.add("jcr:predecessors");
        ignoredNames.add("jcr:successors");
        ignoredNames.add("jcr:checkedOut");
        ignoredNames.add("jcr:created");
    }

    private static final Set<String> ignoredPrincipalPropertyNames = new HashSet<String>();
    static {
    	ignoredPrincipalPropertyNames.add("name");
    	ignoredPrincipalPropertyNames.add("isgroup");
    	ignoredPrincipalPropertyNames.add("members");
    	ignoredPrincipalPropertyNames.add("dynamic");
    	ignoredPrincipalPropertyNames.add("password");
    }
    private static final String SECURITY_PRINCIPLES = "security:principals";
    private static final String SECURITY_ACL = "security:acl";

    /**
     * @see org.apache.sling.jcr.contentloader.ContentReader#parse(java.net.URL, org.apache.sling.jcr.contentloader.ContentCreator)
     */
    public void parse(java.net.URL url, ContentCreator contentCreator)
    throws IOException, RepositoryException {
        InputStream ins = null;
        try {
            ins = url.openStream();
            parse(ins, contentCreator);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public void parse(InputStream ins, ContentCreator contentCreator) throws IOException, RepositoryException {
        try {
            String jsonString = toString(ins).trim();
            if (!jsonString.startsWith("{")) {
                jsonString = "{" + jsonString + "}";
            }
            Map<String, Object> config = new HashMap<>();
            config.put("org.apache.johnzon.supports-comments", true);
            JsonObject json = Json.createReaderFactory(config).createReader(new StringReader(jsonString)).readObject();
            this.createNode(null, json, contentCreator);
        } catch (JsonException je) {
            throw (IOException) new IOException(je.getMessage()).initCause(je);
        }
    }

    protected boolean handleSecurity(String n, Object o, ContentCreator contentCreator) throws RepositoryException{
        if (SECURITY_PRINCIPLES.equals(n)) {
            this.createPrincipals(o, contentCreator);
        } else if (SECURITY_ACL.equals(n)) {
            this.createAcl(o, contentCreator);
        } else {
            return false;
        }
        return true;
    }

    protected void writeChildren(JsonObject obj, ContentCreator contentCreator) throws RepositoryException{
        // add properties and nodes
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            final String n = entry.getKey();
            // skip well known objects
            if (!ignoredNames.contains(n)) {
                Object o = entry.getValue();
                if (!handleSecurity(n, o, contentCreator)) {
                    if (o instanceof JsonObject) {
                        this.createNode(n, (JsonObject) o, contentCreator);
                    } else {
                        this.createProperty(n, o, contentCreator);
                    }
                }
            }
        }
    }

    protected void createNode(String name, JsonObject obj, ContentCreator contentCreator) throws RepositoryException {
        String primaryType = obj.getString("jcr:primaryType", null);

        String[] mixinTypes = null;
        Object mixinsObject = obj.get("jcr:mixinTypes");
        if (mixinsObject instanceof JsonArray) {
            JsonArray mixins = (JsonArray) mixinsObject;
            mixinTypes = new String[mixins.size()];
            for (int i = 0; i < mixinTypes.length; i++) {
                mixinTypes[i] = mixins.getString(i);
            }
        }

        contentCreator.createNode(name, primaryType, mixinTypes);
        writeChildren(obj, contentCreator);
        contentCreator.finishNode();
    }

    protected void createProperty(String name, Object value, ContentCreator contentCreator) throws RepositoryException {
        // assume simple value
        if (value instanceof JsonArray) {
            // multivalue
            final JsonArray array = (JsonArray) value;
            if (array.size() > 0) {
                final String values[] = new String[array.size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] =  unbox(array.get(i)).toString();
                }
                final int propertyType = getType(name, unbox(array.get(0)));
                contentCreator.createProperty(getName(name), propertyType, values);
            } else {
                contentCreator.createProperty(getName(name), PropertyType.STRING, new String[0]);
            }

        } else if (value instanceof JsonValue) {
            // single value
            value = unbox(value);
            final int propertyType = getType(name, value);
            contentCreator.createProperty(getName(name), propertyType, value.toString());
        }
    }

    private Object unbox(Object o) {
        if (o instanceof JsonValue) {
            switch (((JsonValue)o).getValueType()) {
                case FALSE:
                    return false;
                case TRUE:
                    return true;
                case NULL:
                    return null;
                case NUMBER:
                    if (((JsonNumber) o).isIntegral()) {
                        return Long.valueOf(((JsonNumber) o).longValue());
                    }
                    else
                    {
                        return Double.valueOf(((JsonNumber)o).doubleValue());
                    }
                case STRING:
                    return ((JsonString) o).getString();
                default:
                    return o;
            }
        }
        return o;
    }
    private int getType(String name, Object object) {
        if (object instanceof Double || object instanceof Float) {
            return PropertyType.DOUBLE;
        } else if (object instanceof Number) {
            return PropertyType.LONG;
        } else if (object instanceof Boolean) {
            return PropertyType.BOOLEAN;
        } else if (object instanceof String) {
            if (name.startsWith(REFERENCE)) return PropertyType.REFERENCE;
            if (name.startsWith(PATH)) return PropertyType.PATH;
            if (name.startsWith(NAME)) return PropertyType.NAME;
            if (name.startsWith(URI)) return PropertyType.URI;
            if (jsonDate.matcher((String) object).matches()) return PropertyType.DATE;
        }

        // fall back to default
        return PropertyType.UNDEFINED;
    }

    private String getName(String name) {
        if (name.startsWith(REFERENCE)) return name.substring(REFERENCE.length());
        if (name.startsWith(PATH)) return name.substring(PATH.length());
        if (name.startsWith(NAME)) return name.substring(NAME.length());
        if (name.startsWith(URI)) return name.substring(URI.length());
        return name;
    }

    private String toString(InputStream ins) throws IOException {
        if (!ins.markSupported()) {
            ins = new BufferedInputStream(ins);
        }

        String encoding;
        ins.mark(5);
        int c = ins.read();
        if (c == '#') {
            // character encoding following
            StringBuffer buf = new StringBuffer();
            for (c = ins.read(); !Character.isWhitespace((char) c); c = ins.read()) {
                buf.append((char) c);
            }
            encoding = buf.toString();
        } else {
            ins.reset();
            encoding = "UTF-8";
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int rd;
        while ( (rd = ins.read(buf)) >= 0) {
            bos.write(buf, 0, rd);
        }
        bos.close(); // just to comply with the contract

        return new String(bos.toByteArray(), encoding);
    }


    /**
     * Create or update one or more user and/or groups
     *	<code>
     *  {
     *     "security:principals" : [
     *        {
     *           "name":"owner",
     *           "isgroup":"true",
     *           "members":[],
     *           "dynamic":"true"
     *        }
     *     ],
     *  }
     *  </code>
     *  @param obj Object
     *  @param contentCreator Content creator
     *  @throws RepositoryException Repository exception
     */
    protected void createPrincipals(Object obj, ContentCreator contentCreator) throws RepositoryException {
    	if (obj instanceof JsonObject) {
    		//single principal
    		createPrincipal((JsonObject)obj, contentCreator);
    	} else if (obj instanceof JsonArray) {
    		//array of principals
    		JsonArray jsonArray = (JsonArray)obj;
    		for (int i=0; i < jsonArray.size(); i++) {
    			Object object = jsonArray.get(i);
    			if (object instanceof JsonObject) {
    	    		createPrincipal((JsonObject)object, contentCreator);
    			} else {
    				throw new JsonException("Unexpected data type in principals array: " + object.getClass().getName());
    			}
    		}
    	}
    }

    /**
     * Create or update a user or group
     */
    private void createPrincipal(JsonObject json, ContentCreator contentCreator) throws RepositoryException {
    	//create a principal
    	String name = json.getString("name");
    	boolean isGroup = json.getBoolean("isgroup", false);

    	//collect the extra property names to assign to the new principal
    	Map<String, Object> extraProps = new LinkedHashMap<String, Object>();
		for(Map.Entry<String, JsonValue> entry : json.entrySet()) {
			String propName = entry.getKey();
			if (!ignoredPrincipalPropertyNames.contains(propName)) {
    			Object value = unbox(entry.getValue());
    			extraProps.put(propName, value);
			}
		}

    	if (isGroup) {
    		String [] members = null;
    		JsonArray membersJSONArray = (JsonArray) json.get("members");
    		if (membersJSONArray != null) {
    			members = new String[membersJSONArray.size()];
    			for (int i=0; i < members.length; i++) {
    				members[i] = membersJSONArray.getString(i);
    			}
    		}
    		contentCreator.createGroup(name, members, extraProps);
    	} else {
    		String password = json.getString("password");
    		contentCreator.createUser(name, password, extraProps);
    	}
    }

    /**
     * Create or update one or more access control entries for the current
     * node.
     *
     *  <code>
     *  {
     *   "security:acl" : [
     *     	{
     *     		"principal" : "username1",
     *     		"granted" : [
     *      		"jcr:read",
     *      		"jcr:write"
     *     		],
     *     		"denied" : [
     *     		]
     *     	},
     *     	{
     *     		"principal" : "groupname1",
     *     		"granted" : [
     *      		"jcr:read",
     *      		"jcr:write"
     *     		]
     *     	}
     *   ]
     *  }
     *  </code>
     */
    private void createAcl(Object obj, ContentCreator contentCreator) throws RepositoryException {
    	if (obj instanceof JsonObject) {
    		//single ace
    		createAce((JsonObject)obj, contentCreator);
    	} else if (obj instanceof JsonArray) {
    		//array of aces
    		JsonArray jsonArray = (JsonArray)obj;
    		for (int i=0; i < jsonArray.size(); i++) {
    			Object object = jsonArray.get(i);
    			if (object instanceof JsonObject) {
    	    		createAce((JsonObject)object, contentCreator);
    			} else {
    				throw new JsonException("Unexpected data type in acl array: " + object.getClass().getName());
    			}
    		}
    	}
    }

    /**
     * Create or update an access control entry
     */
    private void createAce(JsonObject ace, ContentCreator contentCreator) throws RepositoryException {
		String principalID = ace.getString("principal");

		String [] grantedPrivileges = null;
		JsonArray granted = (JsonArray) ace.get("granted");
		if (granted != null) {
			grantedPrivileges = new String[granted.size()];
			for (int a=0; a < grantedPrivileges.length; a++) {
				grantedPrivileges[a] = granted.getString(a);
			}
		}

		String [] deniedPrivileges = null;
		JsonArray denied = (JsonArray) ace.get("denied");
		if (denied != null) {
			deniedPrivileges = new String[denied.size()];
			for (int a=0; a < deniedPrivileges.length; a++) {
				deniedPrivileges[a] = denied.getString(a);
			}
		}

		String order = ace.getString("order", null);

		//do the work.
		contentCreator.createAce(principalID, grantedPrivileges, deniedPrivileges, order);
    }

}
