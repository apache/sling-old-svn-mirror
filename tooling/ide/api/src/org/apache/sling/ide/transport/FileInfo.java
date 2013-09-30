/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.transport;

public class FileInfo {
	
	private String location;
	private String name;
	private String relativeLocation;
	
	/**
	 * Constructs a new <tt>FileInfo</tt> object
	 * 
	 * @param location the absolute location of the file on the filesystem
	 * @param relativeLocation the location of the file relative to the repository root 
	 * @param name the name of the file
	 */
	public FileInfo(String location, String relativeLocation,String name) {
		super();
		this.location = location;
		this.name = name;
		this.relativeLocation = relativeLocation;
	}

    /**
     * @return the absolute location of the file on the filesystem
     */
	public String getLocation() {
		return location;
	}

	public String getName() {
		return name;
	}

    /**
     * @return the location of the file relative to the repository root
     */
	public String getRelativeLocation() {
		return relativeLocation;
	}

	@Override
	public String toString() {
		return "FileInfo [location=" + location + ", name=" + name
				+ ", relativeLocation=" + relativeLocation + "]";
	}	
}
