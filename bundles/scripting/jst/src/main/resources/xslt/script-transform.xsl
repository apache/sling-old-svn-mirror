<?xml version="1.0" encoding="UTF-8"?>

<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

<!-- 
  Transform JST scripts (after they've been parsed by nekoHTML) to 
  their HTML rendering, by adding some values passed via xsl:param 
  to the head element, and replacing the body with the default 
  rendering and a reference to the script that does the actual 
  client-side rendering. 
 -->
<xsl:transform 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

  <!-- parameters provided by the HtmlCodeGenerator class -->
  <xsl:param name="pageTitle"/>
  <xsl:param name="jstScriptPath"/>
  <xsl:param name="slingScriptPath"/>
  <xsl:param name="defaultRendering"/>
  <xsl:param name="jsonData"/>
  
  <!-- 
    NekoHTML should provide lowercase element names but
    that doesn't seem to work
   -->
  <xsl:variable name="upper" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
  <xsl:variable name="lower" select="'abcdefghijklmnopqrstuvwxyz'"/>
  
  <!-- lowercase element names and call mode=inner templates -->
  <xsl:template match="*">
    <xsl:element name="{translate(local-name(),$upper,$lower)}">
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates mode="inner" select="."/>
    </xsl:element>
  </xsl:template>
  
  <!-- copy attributes, lowercasing element names -->
  <xsl:template match="@*">
    <xsl:attribute name="{translate(local-name(),$upper,$lower)}">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>
  
  <!-- by default, nothing special in mode=inner for  -->
  <xsl:template match="*" mode="inner">
    <xsl:apply-templates/>
  </xsl:template>
  
  <!-- replace body with default rendering + script reference -->
  <xsl:template match="body|BODY" mode="inner">
    <div id="JstDefaultRendering">
<xsl:text>
</xsl:text>
      <xsl:value-of select="$defaultRendering" disable-output-escaping="yes" />
<xsl:text>
</xsl:text>
    </div>
    <script language="javascript">
      var e = document.getElementById("JstDefaultRendering"); 
      e.parentNode.removeChild(e);
    </script>
    <xsl:text>
    </xsl:text>
    <script src="{$jstScriptPath}"/>
  </xsl:template>
  
  <!-- add reference to sling.js in head, and currentNode data -->
  <xsl:template match="head|HEAD" mode="inner">
    <xsl:apply-templates/>
    <script src="{$slingScriptPath}"></script>
    <xsl:text>
    </xsl:text>
    <script language="javascript">
      <xsl:value-of select="$jsonData"/>
    </script>
  </xsl:template>
  
  <!-- For title, set value computed from node properties -->
  <xsl:template match="head/title|HEAD/TITLE">
    <title>
      <xsl:value-of select="$pageTitle"/>
    </title>
  </xsl:template>
  
</xsl:transform>