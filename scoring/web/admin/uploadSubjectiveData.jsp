<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%@ taglib uri="/WEB-INF/tld/taglib62.tld" prefix="up" %>

<%@ page import="fll.web.admin.UploadSubjectiveData" %>
  
<%@ page import="java.io.File" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="java.sql.Connection" %>
  
<html>
  <head>
    <title>Upload Subjective Data</title>
  </head>

  <body>
    <p>
      <ul>
      <up:parse id="numFiles">
<% final File file = File.createTempFile("fll", null); %>
        <up:saveFile path="<%=file.getAbsolutePath()%>"/>
<%
UploadSubjectiveData.saveSubjectiveData(out,
                                        file,
                                        (String)application.getAttribute("currentTournament"),
                                        (Document)application.getAttribute("challengeDocument"),
                                        (Connection)application.getAttribute("connection"));
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
      
<%@ include file="../WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
