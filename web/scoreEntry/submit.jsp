<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.playoff.Playoff" %>
<%@ page import="fll.web.scoreEntry.Submit" %>

<%@ page import="java.sql.Connection" %>
  
<%@ page import="org.w3c.dom.Document" %>
    
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
 
final Connection connection = (Connection)application.getAttribute("connection");
%>

    
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Submit Scores)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
      
    <c:set var="error" value="false" />
    <c:url value="scoreEntry.jsp" var="url"> <%-- build up a url in case we need it --%>
      <c:forEach items="${paramValues}" var="parameter">
        <c:forEach items="${parameter.value}" var="parameterValue">
          <c:param name="${parameter.key}" value="${parameterValue}"/>
      	</c:forEach>
      </c:forEach>
      <x:forEach select="$challengeDocument/fll/Performance/restriction">
        <x:set select="string(./@message)" var="message" />
        <x:set select="number(./@lowerBound)" var="lowerBound" />
        <x:set select="number(./@upperBound)" var="upperBound" />
        <c:set var="restrictionSum" value="0" />
        <x:forEach select="./term">
          <x:set select="string(./@goal)" var="goal" />
          <x:set select="number(./@coefficient)" var="coefficient" />
          <c:set var="restrictionSum" value="${coefficient * paramValues[goal][0] + restrictionSum}" />
        </x:forEach>
        <%--LowerBound <c:out value="${lowerBound}"/><br/>
        UpperBound <c:out value="${upperBound}"/><br/>
        RestrictionSum <c:out value="${restrictionSum}"/><br/>--%>
        <c:if test="${restrictionSum < lowerBound || restrictionSum > upperBound}">
          <x:forEach select="./term">
            <c:set var="paramName"><x:out select="string(./@goal)"/>_error</c:set>
            <c:param name="${paramName}" value="${message}" />
            <%--Adding param <c:out value="${paramName}" /><br/>--%>
          </x:forEach>
          <c:set var="error" value="true" />
          <%--Just set error<br/>--%>
        </c:if>
      </x:forEach>
      <c:if test="${error}">
        <%--Adding param error<br/>--%>
        <c:param name="error" value="true" />
      </c:if>
    </c:url>

    <c:if test="${error}">
      <a href='<c:out value="${url}"/>'>Normally you'd be redirected here</a>
      <c:redirect url="${url}"/>
    </c:if>
    <c:if test="${not error}">
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

      <h1>Submitted Scores</h1>
      
      <p>SQL executed: <br>&nbsp;&nbsp;<c:out value="${sql}"/></p>
      
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
     </c:if>
                        
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
