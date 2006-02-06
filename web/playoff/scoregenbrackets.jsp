<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
<%@ page import="fll.web.playoff.Playoff" %>
  
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Connection" %>
  
<%
/*
  Parameters:
    division - String for the division
*/
  
final Connection connection = (Connection)application.getAttribute("connection");
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

final String divisionStr = request.getParameter("division");
if(null == divisionStr) {
  throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
}
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets) Division: <%=divisionStr%></title>
  </head>
  <style type='text/css'>
      TD.Leaf {font-family:Arial;border-bottom: solid}
      TD.Bridge {border-left: solid; border-right: solid; border-bottom: solid}
      TD.BridgeTop {border-bottom: solid}
      FONT {font-family:Arial}
      FONT.TeamNumber {font-weight:bold}
      FONT.TeamName {font-weight:bold}
      FONT.TeamScore {font-weight:bold;font-size:10pt}
      FONT.TIE {color:#ff0000;font-weight:bold}
  </style>

  <body>
    <h2><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets Division: <%=divisionStr%>)</h2>
    <% Playoff.displayScoresheetGenerationBrackets(connection, challengeDocument, divisionStr, out); %>
    <%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
