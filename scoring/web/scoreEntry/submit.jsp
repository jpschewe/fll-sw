<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.web.scoreEntry.Submit" %>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.SessionAttributes" %>

<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="org.w3c.dom.Document"%>
<%@ page import="fll.db.Queries"%>

<%
final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
%>


<html>
  <head>
    <title>Submit Scores</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>

    <%-- save to database --%>
    <c:choose>
      <c:when test="${not empty param.delete}">
        <%Queries.deletePerformanceScore(connection, request);%>
      </c:when>
      <c:when test="${not empty param.EditFlag}">
        <%Queries.updatePerformanceScore(challengeDocument, connection, request);%>
      </c:when>
      <c:otherwise>
        <%Queries.insertPerformanceScore(challengeDocument, connection, request);%>
      </c:otherwise>
    </c:choose>

    <h1>Submitted Scores</h1>

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
                        

  </body>
</html>
