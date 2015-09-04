/*-
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

package org.apache.sling.query.mock.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.query.mock.ResourceMock;
import org.apache.sling.query.mock.PropertyResourceMock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public final class JsonToResource {
	private JsonToResource() {
	}

	public static Resource parse(InputStream inputStream) {
		JsonElement element = new JsonParser().parse(new InputStreamReader(inputStream));
		return parseResource(element.getAsJsonObject(), "/", null);
	}

	private static Resource parseResource(JsonElement object, String name, Resource parent) {
		if (object.isJsonArray()) {
			return parseResource(object.getAsJsonArray(), name, parent);
		} else if (object.isJsonPrimitive()) {
			return parseResource(object.getAsJsonPrimitive(), name, parent);
		} else if (object.isJsonObject()) {
			return parseResource(object.getAsJsonObject(), name, parent);
		} else {
			return null;
		}
	}

	private static Resource parseResource(JsonArray array, String name, Resource parent) {
		final String[] values = new String[array.size()];
		int i = 0;
		for (JsonElement e : array) {
			values[i++] = e.getAsString();
		}
		return new PropertyResourceMock(parent, name, values);
	}

	private static Resource parseResource(JsonPrimitive primitive, String name, Resource parent) {
		return new PropertyResourceMock(parent, name, primitive.getAsString());
	}

	private static Resource parseResource(JsonObject object, String name, Resource parent) {
		ResourceMock resource = new ResourceMock(parent, name);
		for (Entry<String, JsonElement> entry : object.entrySet()) {
			resource.addChild(parseResource(entry.getValue(), entry.getKey(), resource));
		}
		return resource;
	}
}
