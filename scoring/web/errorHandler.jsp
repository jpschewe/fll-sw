<%@ include file="WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page isErrorPage="true" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (An error has occured)</title>
  </head>

  <body topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (An error has occured)</h1>

    <p><font color="red">An error has occurred!</font>  Error messages:</p>
    <ul>
<%
Throwable e = exception;
while(null != e) {
%>
       <li><%=e.getMessage()%></li>
<%
  e = e.getCause();
}
%>
    </ul>

    <p>Full exception trace:</p>
    <%exception.printStackTrace(new java.io.PrintWriter(out));%>
      


<%@ include file="WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
