<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="org.w3c.dom.Document"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.playoff.Playoff"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="java.sql.Connection"%>

<%
  /*
   Parameters:
   division - String for the division
   enableThird - has value 'yes' if we are to have 3rd/4th place brackets
   */

  final DataSource datasource = ApplicationAttributes.getDataSource(application);
  final Connection connection = datasource.getConnection();
  final Document challengeDocument = (Document) application.getAttribute("challengeDocument");

  final String divisionStr = request.getParameter("division");
  if (null == divisionStr) {
    throw new RuntimeException(
                               "No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
  }
  final String thirdFourthPlaceBrackets = request.getParameter("enableThird");
  boolean enableThird;
  if (null == thirdFourthPlaceBrackets)
    enableThird = false;
  else
    enableThird = true;
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Initialize Brackets Results - Division: <%=divisionStr%></title>
</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
 </h1>
 <h2>
  Initialize Brackets Results, Division
  <%=divisionStr%>:
 </h2>
 <%
   if (Queries.isPlayoffDataInitialized(connection, divisionStr)) {
 %>
 <p>Playoffs have already been initialized for this division.</p>
 <%
   } else {
 %>
 <%
   Playoff.initializeBrackets(connection, challengeDocument, divisionStr, enableThird);
 %>
 Playoff table has been successfully initialized for division
 <%=divisionStr%>.
 <br />
 <a href="index.jsp">Return to the Playoff page</a> to view brackets.
 <%
   }
 %>
</body>
</html>
