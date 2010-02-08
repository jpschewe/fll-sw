<%@ include file="/WEB-INF/jspf/init.jspf" %>

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
    <c:redirect url="select_team.jsp"/>
                        

  </body>
</html>
