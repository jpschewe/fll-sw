<%--
  This page is used for commiting actions of editTeam.jsp.  The request parameter
  addTeam is set when this page is being used to add a team.
  --%>
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="java.sql.Connection" %>

<%@ page import="java.text.NumberFormat" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
%>
  
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Commit Team)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Commit Team)</h1>
                  
<%
//parse the numbers first so that we don't get a partial commit
final int teamNumber = NumberFormat.getInstance().parse(request.getParameter("teamNumber")).intValue();
final String division = request.getParameter("division");
final String numBoysStr = request.getParameter("numBoys");
final int numBoys;
if(null != numBoysStr && !"".equals(numBoysStr) && !"null".equals(numBoysStr)) {
  numBoys = NumberFormat.getInstance().parse(numBoysStr).intValue();
} else {
  numBoys = -1;
}
final String numGirlsStr = request.getParameter("numGirls");
final int numGirls;
if(null != numGirlsStr && !"".equals(numGirlsStr) && !"null".equals(numGirlsStr)) {
  numGirls = NumberFormat.getInstance().parse(numGirlsStr).intValue();
} else {
  numGirls = -1;
}
final String numMedalsStr = request.getParameter("numMedals");
final int numMedals;
if(null != numMedalsStr && !"".equals(numMedalsStr) && !"null".equals(numMedalsStr)) {
  numMedals = NumberFormat.getInstance().parse(numMedalsStr).intValue();
} else {
  numMedals = -1;
}
%>

<%-- check which button was pushed --%> 
<c:choose>
  <c:when test='${not empty param.delete}'>
    <%Queries.deleteTeam(teamNumber, challengeDocument, connection, application);%>
    <a href="select_team.jsp">Normally you'd be redirected here</a>
    <c:redirect url='select_team.jsp' />
  </c:when> <%-- end delete --%>
  <c:when test='${not empty param.advance}'>
    <%
    final boolean result = Queries.advanceTeam(connection, teamNumber);
    if(result) {
    %>
      <a href="select_team.jsp">Normally you'd be redirected here</a>
      <c:redirect url='select_team.jsp' />
    <%
    } else {
    %>
        <font color='red'>Error advancing team</font>
    <%
    }
    %>
  </c:when> <%-- end advance --%>
  <c:when test='${not empty param.demote}'>
    <%Queries.demoteTeam(connection, challengeDocument, teamNumber);%>
    <a href="select_team.jsp">Normally you'd be redirected here</a>
    <c:redirect url='select_team.jsp' />
  </c:when>
  <c:when test='${not empty param.commit}'>
    <c:if test="${not empty param.addTeam}" var="addingTeam">
                added a team
<%
      pageContext.setAttribute("dup", Queries.addTeam(connection,
                                   teamNumber,
                                   request.getParameter("teamName"),
                                   request.getParameter("organization"),
                                   request.getParameter("region"),
                                   division,
                                   numMedals));
%>
      <c:if test="${null != dup}">
        <c:redirect url='index.jsp'>
          <c:param name='message'>
            <font color='red'>Error, team number <%=teamNumber%> is already assigned to <c:out value="${dup}"/>.</font>
          </c:param>
        </c:redirect>
      </c:if>

      <a href="index.jsp">Normally you'd be redirected here</a>
      <c:redirect url='index.jsp' />
    </c:if>
    <c:if test="${not addingTeam}">
<%
      Queries.updateTeam(connection,
                         teamNumber,
                         request.getParameter("teamName"),
                         request.getParameter("organization"),
                         request.getParameter("region"),
                         division,
                         numMedals);
%>
      <c:if test="${not empty param.currentTournament}">
<%
        Queries.changeTeamCurrentTournament(connection, challengeDocument, teamNumber, request.getParameter("currentTournament"));
%>
      </c:if>
                      
      <a href="select_team.jsp">Normally you'd be redirected here</a>
      <c:redirect url='select_team.jsp' />
    </c:if>
                    
  </c:when> <%-- end commit --%>
  <c:otherwise>
    <p>Internal error, cannot not figure out what editTeam.jsp did!</p>
  </c:otherwise>
</c:choose> <%-- end checking which button --%>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>    
  </body>
</html>
