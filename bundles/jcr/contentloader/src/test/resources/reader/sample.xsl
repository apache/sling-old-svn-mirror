<?xml version="1.0"?>

<!-- 
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
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="yes" />

    <xsl:template match="/">
        <xsl:apply-templates select="pages/page"/>
    </xsl:template>

    <xsl:template match="page">
        <node>
            <name><xsl:value-of select="@name"/></name>
            <primaryNodeType>nt:unstructured</primaryNodeType>
            <mixinNodeTypes/>
            <properties>
                <xsl:call-template name="property">
                    <xsl:with-param name="name">content</xsl:with-param>
                    <xsl:with-param name="value"><xsl:value-of select="content"/></xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="property">
                    <xsl:with-param name="name">sling:resourceType</xsl:with-param>
                    <xsl:with-param name="value">page</xsl:with-param>
                </xsl:call-template>
            </properties>
        </node>
    </xsl:template>

    <xsl:template name="property">
        <xsl:param name="name"/>
        <xsl:param name="value"/>
        <property>
            <name><xsl:value-of select="$name"/></name>
            <type>String</type>
            <value><xsl:value-of select="$value"/></value>
        </property>
    </xsl:template>

</xsl:stylesheet>