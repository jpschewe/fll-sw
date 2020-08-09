<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="1.0">

  <xsl:output method="xml" indent="yes" />
  
  <!-- default is to keep all nodes -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <!-- upgrade version -->
  <xsl:template match="fll/@schemaVersion">
    <xsl:attribute name="schemaVersion">
      <xsl:value-of select="3" />
    </xsl:attribute>
  </xsl:template>

  <!-- default rule for a goal is to just copy it -->
  <xsl:template match="goal" priority="0">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>    
  </xsl:template>

  <!-- if the goal has a category, then create a goalGroup element -->
  <xsl:template match="goal[@category]" priority="3">
    <goalGroup title="{@category}">
      <xsl:apply-templates select="." mode="sequence" />
    </goalGroup>
  </xsl:template>
  
  <!-- if the previous goal element has the same category, don't output -->
  <xsl:template match="goal[@category=preceding-sibling::*[1]/@category]" priority="4"/>

  <xsl:template match="goal" mode="sequence">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
    <xsl:variable name="currentGoal" select="."/>
    <xsl:apply-templates select="following-sibling::*[1][self::goal/@category=$currentGoal/@category]" mode="sequence"/>    
  </xsl:template>

  <!-- remove category attribute from goal as they are now grouped  -->
  <xsl:template match="goal/@category" />
  
</xsl:stylesheet>
