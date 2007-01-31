<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="fll.web.admin.UploadTeams" %>
<%@ page import="fll.web.UploadProcessor"%>
<%@ page import="org.apache.commons.fileupload.FileItem"%>
<%@ page import="java.io.File" %>

<%@ page import="java.sql.Connection" %>
  
<%
final Connection connection = (Connection)application.getAttribute("connection");
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Upload Teams)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Upload Teams)</h1>
<% 
UploadProcessor.processUpload(request);
final FileItem teamsFileItem = (FileItem) request.getAttribute("teamsFile");
final File file = File.createTempFile("fll", null);
teamsFileItem.write(file);
UploadTeams.parseFile(file, connection, session);
file.delete();
%>

      <p>
        <ul>
          <li>Normally you'd be redirected <a href="<%=response.encodeRedirectURL("filterTeams.jsp")%>">here.</a></li>
        </ul>
      </p>

<%
response.sendRedirect(response.encodeRedirectURL("filterTeams.jsp"));
%>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
