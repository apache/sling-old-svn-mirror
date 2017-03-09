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
package org.apache.sling.maven.bundlesupport.fsresource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

class FsResourceConfiguration {

    private FsMode fsMode;
    private String providerRootPath;
    private String contentRootDir;
    private String initialContentImportOptions;
    private String fileVaultFilterXml;

    public FsMode getFsMode() {
        return fsMode;
    }
    public FsResourceConfiguration fsMode(FsMode value) {
        this.fsMode = value;
        return this;
    }
    public FsResourceConfiguration fsMode(String value) {
        if (StringUtils.isBlank(value)) {
            this.fsMode = null;
        }
        else {
            this.fsMode = FsMode.valueOf(StringUtils.upperCase(value));
        }
        return this;
    }

    public String getProviderRootPath() {
        return providerRootPath;
    }
    public FsResourceConfiguration providerRootPath(String value) {
        this.providerRootPath = value;
        return this;
    }

    public String getContentRootDir() {
        return contentRootDir;
    }
    public FsResourceConfiguration contentRootDir(String value) {
        this.contentRootDir = value;
        return this;
    }

    public String getInitialContentImportOptions() {
        return initialContentImportOptions;
    }
    public FsResourceConfiguration initialContentImportOptions(String value) {
        this.initialContentImportOptions = value;
        return this;
    }

    public String getStringVaultFilterXml() {
        return fileVaultFilterXml;
    }
    public FsResourceConfiguration fileVaultFilterXml(String value) {
        this.fileVaultFilterXml = value;
        return this;
    }
    
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

}
