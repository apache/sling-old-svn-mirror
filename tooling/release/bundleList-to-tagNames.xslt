<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="text" omit-xml-declaration="yes"/>

	<!-- start at the root node and apply templates for all nodes named 'bundle' whose groupId child is equal to 'org.apache.sling' -->
	<xsl:template match="/">
		<xsl:apply-templates select="//bundle[groupId='org.apache.sling']"/>
	</xsl:template>	

	<!-- for each matched bundle node output the expected SVN tag value - ${artifactId}-${groupId} - on a single line -->
	<xsl:template match="bundle">
		<xsl:value-of select="artifactId"/>
		<xsl:text>-</xsl:text>
		<xsl:value-of select="version"/>
		<xsl:text><!-- newline -->&#xa;</xsl:text>
	</xsl:template>
</xsl:stylesheet>