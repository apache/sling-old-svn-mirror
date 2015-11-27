/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.jcr.resource.internal.helper.JcrPropertyMapCacheEntry;

/**
 *
 * @author mikolaj.manski
 */
public abstract class JcrCacheableValueMap {

	protected final ValueMapCache cache;

	/**
	 * keep all prefixes for escaping
	 */
	private String[] namespacePrefixes;

	/**
	 * The underlying node.
	 */
	protected final Node node;

	/**
	 * Has the node been read completely?
	 */
	protected boolean fullyRead;

	protected JcrCacheableValueMap(final Node node, final ValueMapCache cache) {
		this.node = node;
		this.cache = cache;
		this.fullyRead = false;
	}

	/**
	 * Put a single property into the cache
	 *
	 * @param prop
	 * @return the cached property
	 * @throws IllegalArgumentException if a repository exception occurs
	 */
	protected JcrPropertyMapCacheEntry cacheProperty(final Property prop) {
		try {
			// calculate the key
			final String name = prop.getName();
			String key = null;
			if (name.indexOf("_x") != -1) {
				// for compatibility with older versions we use the (wrong)
				// ISO9075 path encoding
				key = ISO9075.decode(name);
				if (key.equals(name)) {
					key = null;
				}
			}
			if (key == null) {
				key = Text.unescapeIllegalJcrChars(name);
			}
			JcrPropertyMapCacheEntry entry = cache.getCache().get(key);
			if (entry == null) {
				entry = new JcrPropertyMapCacheEntry(prop);
				cache.getCache().put(key, entry);

				final Object defaultValue = entry.getPropertyValue();
				if (defaultValue != null) {
					cache.getValueCache().put(key, entry.getPropertyValue());
				}
			}
			return entry;
		} catch (final RepositoryException re) {
			throw new IllegalArgumentException(re);
		}
	}

	/**
	 * Read a single property.
	 *
	 * @throws IllegalArgumentException if a repository exception occurs
	 */
	protected JcrPropertyMapCacheEntry read(final String name) {
		// check for empty key
		if (name.length() == 0) {
			return null;
		}
		// if the name is a path, we should handle this differently
		if (name.indexOf('/') != -1) {
			// first a compatibility check with the old (wrong) ISO9075
			// encoding
			final String path = ISO9075.encodePath(name);
			try {
				if (node.hasProperty(path)) {
					return new JcrPropertyMapCacheEntry(node.getProperty(path));
				}
			} catch (final RepositoryException re) {
				throw new IllegalArgumentException(re);
			}
			// now we do a proper segment by segment encoding
			final StringBuilder sb = new StringBuilder();
			int pos = 0;
			int lastPos = -1;
			while (pos < name.length()) {
				if (name.charAt(pos) == '/') {
					if (lastPos + 1 < pos) {
						sb.append(Text.escapeIllegalJcrChars(name.substring(lastPos + 1, pos)));
					}
					sb.append('/');
					lastPos = pos;
				}
				pos++;
			}
			if (lastPos + 1 < pos) {
				sb.append(Text.escapeIllegalJcrChars(name.substring(lastPos + 1)));
			}
			final String newPath = sb.toString();
			try {
				if (node.hasProperty(newPath)) {
					return new JcrPropertyMapCacheEntry(node.getProperty(newPath));
				}
			} catch (final RepositoryException re) {
				throw new IllegalArgumentException(re);
			}

			return null;
		}

		// check cache
		JcrPropertyMapCacheEntry cachedValued = cache.getCache().get(name);
		if (fullyRead || cachedValued != null) {
			return cachedValued;
		}

		final String key;
		try {
			key = escapeKeyName(name);
			if (node.hasProperty(key)) {
				final Property prop = node.getProperty(key);
				return cacheProperty(prop);
			}
		} catch (final RepositoryException re) {
			throw new IllegalArgumentException(re);
		}

		try {
			// for compatibility with older versions we use the (wrong) ISO9075 path
			// encoding
			final String oldKey = ISO9075.encodePath(name);
			if (!oldKey.equals(key) && node.hasProperty(oldKey)) {
				final Property prop = node.getProperty(oldKey);
				return cacheProperty(prop);
			}
		} catch (final RepositoryException re) {
			// we ignore this
		}

		// property not found
		return null;
	}

	/**
	 * Handles key name escaping by taking into consideration if it contains a registered prefix
	 *
	 * @param key the key to escape
	 * @return escaped key name
	 * @throws RepositoryException if the repository's namespaced prefixes cannot be retrieved
	 */
	protected String escapeKeyName(final String key) throws RepositoryException {
		final int indexOfPrefix = key.indexOf(':');
		// check if colon is neither the first nor the last character
		if (indexOfPrefix > 0 && key.length() > indexOfPrefix + 1) {
			final String prefix = key.substring(0, indexOfPrefix);
			for (final String existingPrefix : getNamespacePrefixes()) {
				if (existingPrefix.equals(prefix)) {
					return prefix
							+ ":"
							+ Text.escapeIllegalJcrChars(key
									.substring(indexOfPrefix + 1));
				}
			}
		}
		return Text.escapeIllegalJcrChars(key);
	}

	/**
	 * Read namespace prefixes and store as member variable to minimize number of JCR API calls
	 *
	 * @return the namespace prefixes
	 * @throws RepositoryException if the namespace prefixes cannot be retrieved
	 */
	protected String[] getNamespacePrefixes() throws RepositoryException {
		if (this.namespacePrefixes == null) {
			this.namespacePrefixes = node.getSession().getNamespacePrefixes();
		}
		return this.namespacePrefixes;
	}

}
