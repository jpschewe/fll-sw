<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%></title>
  </head>

  <body background="<c:url value="/images/bricks1.gif" />" bgcolor="#ffffff" topmargin='4'>

    <center>
      <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%></h1>
      
      <br />
      <br />
      <img width='600' src='<c:url value="/images/Minnesota-FLL-logo.gif"/>' />

      <font face='arial' size='3'>
        <b><c:out value="${ScorePageText}" /></b>
      </font>

            
    </center>
        
  </body>
</html>
