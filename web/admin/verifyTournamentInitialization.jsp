<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jstl/sql" %>
      
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
      
<%@ page import="fll.Queries" %>
<%@ page import="org.w3c.dom.Document" %>

<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");

pageContext.setAttribute("regions", Queries.getRegions(connection));
%>

<c:set var='url'>
  jdbc:mysql://<c:out value="${initParam.database_host}"/>/<c:out value="${database}"/>?autoReconnect=true
</c:set>
<sql:setDataSource driver="org.gjt.mm.mysql.Driver"
                   url="${url}"
                   user="fll"
                   password="fll"
                   />
        
        
<c:if test='${"true" == param.verified}'>
  <c:forEach var="parameter" items="${paramValues}">
    <c:if test='${"nochange" != parameter.value[0] && "verified" != parameter.key && "submit" != parameter.key}'>
      <!-- need MySQL 4.0.2 or greater for this one! -->
      <sql:update>
        DELETE FROM TournamentTeams Using Teams, TournamentTeams WHERE TournamentTeams.TeamNumber = Teams.TeamNumber AND Teams.Region = '<c:out value="${parameter.key}"/>'
      </sql:update>
      <sql:update>
        INSERT INTO TournamentTeams (TeamNumber, Tournament) SELECT Teams.TeamNumber AS TeamNumber, '<c:out value="${parameter.value[0]}"/>' FROM Teams WHERE Teams.Region = '<c:out value="${parameter.key}"/>'
      </sql:update>
    </c:if>
  </c:forEach>
  
  <c:redirect url="index.jsp" >
    <c:param name="message">
      Successfully initialized tournament for teams based on region.
    </c:param>
  </c:redirect>
</c:if>
      
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Verify Tournament Initialization)</title>
  </head>

  <body background="<c:url value="/images/bricks1.gif" />" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Verify Tournament Initialization)</h1>

    <p>The following teams will have their tournament changed if you continue:</p>

    <form name="verify" action="verifyTournamentInitialization.jsp" method="post">
      <table border='1'>
        <tr><th>Team Number</th><th>Team Name</th></tr>
              
        <c:forEach var="parameter" items="${paramValues}">
          <input type='hidden'
                 name='<c:out value="${parameter.key}" />'
                 value='<c:out value="${parameter.value[0]}" />'
                 />
          <c:if test='${"nochange" != parameter.value[0]}'>
            <sql:query var="result">
              SELECT DISTINCT Teams.TeamNumber AS TeamNumber, Teams.TeamName AS TeamName
                FROM Teams,TournamentTeams
                WHERE Teams.Region = '<c:out value="${parameter.key}"/>'
                AND TournamentTeams.TeamNumber = Teams.TeamNumber
                ORDER BY Teams.TeamNumber
            </sql:query>
            <c:forEach items="${result.rows}" var="row">
              <tr>
                <td><c:out value="${row.TeamNumber}" /></td>
                <td><c:out value="${row.TeamName}" /></td>
              </tr>
            </c:forEach>
          </c:if>
        </c:forEach>
      </table>
            
      <input type='hidden' name='verified' value='true' />
      <input type='submit' name='submit' value='Continue' />
    </form>


<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
