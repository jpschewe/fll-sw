<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ taglib uri="../WEB-INF/tld/taglib62.tld" prefix="up" %>

<%@ page import="fll.Queries" %>
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="java.io.File" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("adminConnection");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Upload Teams)</title>
  </head>

  <body background='../images/bricks1.gif' bgcolor='#ffffff' topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Upload Teams)</h1>
      <up:parse id="numFiles">
<% final File file = File.createTempFile("fll", null); %>
        <up:saveFile path="<%=file.getAbsolutePath()%>"/>
<%
UploadTeams.parseFile(file, (Connection)application.getAttribute("adminConnection"), session);
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
