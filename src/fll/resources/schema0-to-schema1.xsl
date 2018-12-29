<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- default is to keep all nodes -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

 <xsl:template match="fll">
  <xsl:copy>
   <xsl:attribute name="schemaVersion">1</xsl:attribute>
   <xsl:apply-templates select="@*|node()" />
  </xsl:copy>
 </xsl:template>

  <!-- rename goalRef to enumGoalRef -->
  <xsl:template match="goalRef">
    <enumGoalRef>
      <xsl:apply-templates select="@*|node()" />
    </enumGoalRef>
  </xsl:template>
  
  
  <!-- make term have children -->
  <xsl:template match="term">
    <term>
      <xsl:if test="@coefficient">
	<constant value="{@coefficient}"/>
      </xsl:if>
      
      <goalRef goal="{@goal}" scoreType="{@scoreType}"/>
      
    </term>
  </xsl:template>
  
  <!-- transform constant -->
  <xsl:template match="constant">
    <term>
      <xsl:copy select=".">
	<xsl:copy-of select="@*"/>
      </xsl:copy>
    </term>
  </xsl:template>

  <!-- transform variableRef -->
  <xsl:template match="variableRef">
    <term>     
      <xsl:if test="@coefficient">
	<constant value="{@coefficient}"/>
      </xsl:if>

      <variableRef variable="{@variable}"/>

    </term>

  </xsl:template>

</xsl:stylesheet>
