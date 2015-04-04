<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- default is to keep all nodes -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <!-- upgrade version -->
  <xsl:template match="fll/@schemaVersion">
    <xsl:attribute name="schemaVersion">
    <xsl:value-of select="2" />
    </xsl:attribute>
  </xsl:template>

  <!-- remove bracketSort attribute -->
  <xsl:template match="fll/@bracketSort" />

</xsl:stylesheet>
