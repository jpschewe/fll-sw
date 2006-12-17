<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ taglib uri="/WEB-INF/tld/taglib62.tld" prefix="up" %>

<%@ page import="fll.web.admin.UploadSubjectiveData" %>
  
<%@ page import="java.io.File" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="fll.db.Queries" %>  
<%@ page import="org.w3c.dom.Document" %>
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Upload Subjective Data</title>
  </head>

  <body>
    <p>
      <ul>
      <up:parse id="numFiles">
<% final File file = File.createTempFile("fll", null); %>
        <up:saveFile path="<%=file.getAbsolutePath()%>"/>
<%
final Connection connection = (Connection)application.getAttribute("connection");
UploadSubjectiveData.saveSubjectiveData(out,
                                        file,
                                        Queries.getCurrentTournament(connection),
                                        (Document)application.getAttribute("challengeDocument"),
                                        connection);
file.delete();
%>
        </li>                  
      </up:parse>
      </ul>
    </p>

      <p>
        <ul>
          <li><%=numFiles%> file(s) successfully uploaded.</li>
          <li>Normally you'd be redirected <a href="<%=response.encodeRedirectURL("index.jsp?message=Subjective+data+uploaded+successfully")%>">here.</a></li>
        </ul>
      </p>
      
<% response.sendRedirect(response.encodeRedirectURL("index.jsp?message=Subjective+data+uploaded+successfully")); %>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
