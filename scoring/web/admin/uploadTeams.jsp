<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ taglib uri="/WEB-INF/tld/taglib62.tld" prefix="up" %>

<%@ page import="fll.Queries" %>
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="java.io.File" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Upload Teams)</title>
  </head>

  <body>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Upload Teams)</h1>
      <up:parse id="numFiles">
<% final File file = File.createTempFile("fll", null); %>
        <up:saveFile path="<%=file.getAbsolutePath()%>"/>
<%
UploadTeams.parseFile(file, connection, session);
file.delete();
%>
      </up:parse>

      <p>
        <ul>
          <li><%=numFiles%> file(s) successfully uploaded.</li>
          <li>Normally you'd be redirected <a href="<%=response.encodeRedirectURL("filterTeams.jsp")%>">here.</a></li>
        </ul>
      </p>

<%
response.sendRedirect(response.encodeRedirectURL("filterTeams.jsp"));
%>
      
<%@ include file="../WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
