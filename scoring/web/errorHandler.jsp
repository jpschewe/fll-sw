
<%@ include file="WEB-INF/jspf/init.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page isErrorPage="true" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (An error has occured)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
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
