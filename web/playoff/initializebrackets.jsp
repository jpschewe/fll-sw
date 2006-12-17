<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.web.playoff.Playoff" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
/*
  Parameters:
    division - String for the division
    enableThird - has value 'yes' if we are to have 3rd/4th place brackets
*/
  
final Connection connection = (Connection)application.getAttribute("connection");
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

final String divisionStr = request.getParameter("division");
if(null == divisionStr) {
  throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
}
final String thirdFourthPlaceBrackets = request.getParameter("enableThird");
boolean enableThird;
if(null == thirdFourthPlaceBrackets)
  enableThird = false;
else
  enableThird = true;
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Initialize Brackets Results) Division: <%=divisionStr%></title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/></h1>
    <h2>Initialize Brackets Results, Division <%=divisionStr%>:</h2>
    <% Playoff.initializeBrackets(connection, challengeDocument, divisionStr, enableThird, out); %>
    Playoff table has been successfully initialized for division <%=divisionStr%>.<br/>
    <a href="index.jsp">Return to the Playoff page</a> to view brackets.
    <%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
