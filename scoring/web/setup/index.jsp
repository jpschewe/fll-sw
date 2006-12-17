<%@ taglib uri="/WEB-INF/tld/taglib62.tld" prefix="up" %>

  
<%@ page import="fll.db.GenerateDB" %>
<%@ page import="fll.xml.ChallengeParser" %>

<%@ page import="java.io.File" %>
<%@ page import="java.io.FileInputStream" %>
<%@ page import="java.io.InputStream" %>
  
<%@ page import="org.w3c.dom.Document" %>
  
<%
final StringBuffer message = new StringBuffer();
File xmlfile = null;
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>FLL (Database setup)</title>
  </head>

  <body>
    <h1>FLL (Database setup)</h1>
      
<%if("POST".equals(request.getMethod())) {%>

    <up:parse id="numFiles">
<% xmlfile = File.createTempFile("fll", null); %>
      <up:saveFile path="<%=xmlfile.getAbsolutePath()%>"/>
    </up:parse>

<%
  if(null != request.getAttribute("reinitializeDatabase")) {
    final boolean forceRebuild = "1".equals(request.getAttribute("force_rebuild"));
    final InputStream xmlstream;
    if(null == xmlfile) {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      xmlstream = classLoader.getResourceAsStream("resources/challenge.xml");
    } else {
      xmlstream = new FileInputStream(xmlfile);
    }
    final Document document = ChallengeParser.parse(xmlstream);
    xmlstream.close();
    final String db = config.getServletContext().getRealPath("/WEB-INF/flldb");
    GenerateDB.generateDB(document, db, forceRebuild);
  
    if(null != xmlfile) {
      xmlfile.delete();
    }
    
    //remove application variables that depend on the database
    application.removeAttribute("connection");
    application.removeAttribute("challengeDocument");
  
    message.append("<p id='success'><i>Successfully initialized database</i></p>");
  } else {
    message.append("reinitializeDatabase attribute not set? " + request.getAttribute("rootUser"));
  }
}
%>


    <p><%=message.toString()%>

      This will create a database called fll.<br>
          
      <form id='setup' action='index.jsp' method='post' enctype="multipart/form-data">
        XML description document (leave blank to use the default tournament description) <input type="file" size=32" name="xmldocument"><br/>
          <input type="checkbox"
            name='force_rebuild'
            value="1"/> Rebuild the whole database, including team data?<br/>
          
        <input type='submit' name='reinitializeDatabase' value='Initialize Database' onclick='return confirm("This will erase ALL scores in the database fll (if it already exists), are you sure?")'>
      </form>
    </p>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
        
  </body>
</html>
