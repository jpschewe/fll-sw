<%@ taglib uri="/WEB-INF/tld/taglib62.tld" prefix="up" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ page import="fll.xml.GenerateDB" %>
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
    final String rootUser = (String)request.getAttribute("rootUser");
    final String rootPassword = (String)request.getAttribute("rootPassword");
    final InputStream xmlstream;
    if(null == xmlfile) {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      xmlstream = classLoader.getResourceAsStream("resources/challenge.xml");
    } else {
      xmlstream = new FileInputStream(xmlfile);
    }
    final Document document = ChallengeParser.parse(xmlstream);
    xmlstream.close();
    GenerateDB.generateDB(document, application.getInitParameter("database_host"), rootUser, rootPassword, "fll");
  
    if(null != xmlfile) {
      xmlfile.delete();
    }
    
    //remove application variables that depend on the database
    application.removeAttribute("connection");
    application.removeAttribute("tournamentTeams");
    application.removeAttribute("challengeDocument");
  
    message.append("<i>Successfully initialized database</i><br>");
  } else {
    message.append("reinitializeDatabase attribute not set? " + request.getAttribute("rootUser"));
  }
}
%>


    <p><%=message.toString()%>

      This will create a database called fll.<br>
          
      <form action='index.jsp' method='post' enctype="multipart/form-data">
        Root username <input type='text' name='rootUser'><br>
        Root password <input type='password' name='rootPassword'><br>
        XML description document (leave blank to use the default tournament description) <input type="file" size=32" name="xmldocument">
        <input type='submit' name='reinitializeDatabase' value='Initialize Database' onclick='return confirm("This will erase ALL data in the database fll (if it already exists), are you sure?")'>
      </form>
    </p>
        
  </body>
</html>
