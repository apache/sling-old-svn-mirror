package org.apache.sling.jcr.jcrinstall.osgi.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;

public class MockInstallableData implements InstallableData {

	private final InputStream inputStream;
	private long lastModified;
	private String digest;
	private static int counter;
	
	public MockInstallableData(String uri) {
        inputStream = new ByteArrayInputStream(uri.getBytes());
        lastModified = System.currentTimeMillis() + counter;
        counter++;
        digest = String.valueOf(lastModified);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MockInstallableData) {
			final MockInstallableData other = (MockInstallableData)obj;
			return digest.equals(other.digest);
		}
		return false;
	}
	
	public long getLastModified() {
		return lastModified;
	}

	@Override
	public int hashCode() {
		return digest.hashCode();
	}

	@SuppressWarnings("unchecked")
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if(type.equals(InputStream.class)) {
			return (AdapterType)inputStream;
		}
		return null;
	}

	void setDigest(String d) {
		digest = d;
	}
	
	public String getDigest() {
		return digest;
	}
}
