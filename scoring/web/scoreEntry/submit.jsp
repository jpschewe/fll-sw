<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<%@ page import="fll.Queries" %>
<%@ page import="fll.web.scoreEntry.Submit" %>

<%@ page import="java.sql.Connection" %>
  
<%@ page import="org.w3c.dom.Document" %>
    
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
 
final Connection connection = (Connection)application.getAttribute("connection");
%>

<%-- save to database --%>
<c:choose>
  <c:when test="${not empty param.delete}">
  <%pageContext.setAttribute("sql", Queries.deletePerformanceScore(connection, request));%>
  </c:when>
  <c:when test="${not empty param.EditFlag}">
  <%pageContext.setAttribute("sql", Queries.updatePerformanceScore(challengeDocument, connection, request));%>
  </c:when>
  <c:otherwise>
  <%pageContext.setAttribute("sql", Queries.insertPerformanceScore(challengeDocument, connection, request));%>
  </c:otherwise>
</c:choose>
  
    
<html>
  <head>
    <title><x:out select="$challengeDocument//@title"/> (Submit Scores)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <p>SQL executed: <br>&nbsp;&nbsp;<c:out value="${sql}"/></p>
      
    <h1>Submit Scores</h1>
      <table border='1'>          
         <tr>
           <td>Team Number</td>
           <td><c:out value="${param.TeamNumber}"/></td>
         </tr>
         <tr>
           <td>Run Number</td>
           <td><c:out value="${param.RunNumber}"/></td>
         </tr>                
         <tr>
           <td>NoShow</td>
           <td><c:out value="${param.NoShow}"/></td>
         </tr>
         <tr>
           <td>Edit?</td>
           <td><c:out value="${param.EditFlag}"/></td>
         </tr>
          <tr>
            <td>Delete?</td>
            <td><c:out value="${param.delete}"/></td>
          </tr>
         <%Submit.generateParameterTableRows(out, challengeDocument, request);%>
       </table>

       <a href="select_team.jsp">Normally you'd be redirected here</a>
       <c:redirect url="select_team.jsp"/>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
