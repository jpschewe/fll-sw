<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Credits</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Credits)</h1>

    <p>This software is licensed by INSciTE under the <a
    href="LICENSE.txt">GPL</a></p>

    <p>Developers:</p>
    <ul>
      <li><a href="http://mtu.net/~jpschewe">Jon Schewe</a></li>
      <li><a href="http://mtu.net/~engstrom">Eric Engstrom</a></li>
      <li>Dan Churchill</li>
    </ul>

    <p>Testers (besides the developers):</p>
    <ul>
      <li>Chuck Davis</li>
    </ul>
      
    <p>The following software packages are used in the application:</p>
    <ul>
      <li><a href="http://mtu.net/~jpschewe/JonsInfra/index.html">JonsInfra</a> -
        <a href="JonsInfra-license.txt">License</a></li>
      
      <li><a href="http://jakarta.apache.org/tomcat/index.html">Apache Tomcat</a> -
        <a href="tomcat-license.txt">License</a></li>

      <li><a href="http://java.sun.com/">Java Development Kit</a> -
        <a href="jdk-license.txt">License</a></li>

      <li><a href="http://jakarta.apache.org/commons/lang/">Jakarta Commons Lang</a> -
        <a href="commons-lang-LICENSE.txt">License</a></li>

      <li><a href="http://www.lowagie.com/iText/">iText (PDF generation library)</a> -
        <a href="iText-license.txt">License Information</a></li>
        
      <li><a href="http://jakarta.apache.org/commons/io/">Jakarta Commons IO</a> -
        <a href="commons-io-LICENSE.txt">License</a></li>
        
      <li><a href="http://jakarta.apache.org/commons/fileupload/">Jakarta Commons Fileupload</a> -
        <a href="commons-fileupload-LICENSE.txt">License</a></li>

      <li><a href="http://hsqldb.org">HSQLDB</a> -
        <a href="hsqldb-license.txt">License</a></li>

      <li><a href="http://logging.apache.org/log4j/docs/">Log4j</a> -
        <a href="log4j-license.txt">License</a></li>

      <li><a href="http://jakarta.apache.org/taglibs/doc/standard-doc/intro.html">Jakarta Taglibs - Standard</a> -
        <a href="jstl-license.txt">License</a></li>

      <li><a href="http://xml.apache.org/xalan-j/">Xalan</a> -
        <a href="xalan-license.txt">License</a></li>

      <li><a href="http://xerces.apache.org/xerces2-j/">Xerces</a> -
        <a href="xerces-license.txt">License</a></li>
        
        <li><a href="http://poi.apache.org/">POI</a> (Used for reading Excel files) - <a href="poi-license.txt">License</a></li>
    </ul>


  </body>
</html>
