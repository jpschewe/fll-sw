<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
  
  <%
  final DataSource datasource = SessionAttributes.getDataSource(session);
  final Connection connection = datasource.getConnection();
 final String currentTournament = Queries.getCurrentTournament(connection);

 final String divisionKey = "playoffDivision";
 final String roundNumberKey = "playoffRoundNumber";
 final String displayName = (String)session.getAttribute("displayName");

 final String sessionDivision;
 final Number sessionRoundNumber;
 if (null != displayName) {
   sessionDivision = (String) application.getAttribute(displayName
       + "_" + divisionKey);
   sessionRoundNumber = (Number) application.getAttribute(displayName
       + "_" + roundNumberKey);
 } else {
   sessionDivision = null;
   sessionRoundNumber = null;
 }

 final String division;
 if (null != sessionDivision) {
   division = sessionDivision;
 } else if (null == application.getAttribute(divisionKey)) {
   final List<String> divisions = Queries.getDivisions(connection);
   if (!divisions.isEmpty()) {
     division = divisions.get(0);
   } else {
     throw new RuntimeException("No division specified and no divisions in the database!");
   }
 } else {
   division = (String) application.getAttribute(divisionKey);
 }

  final int playoffRoundNumber;
  if (null != sessionRoundNumber) {
    playoffRoundNumber = sessionRoundNumber.intValue();
  } else if (null == application.getAttribute(roundNumberKey)) {
    playoffRoundNumber = 1;
  } else {
    playoffRoundNumber = ((Number) application.getAttribute(roundNumberKey)).intValue();
  }
  %>
  
<html>
<head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  <meta http-equiv='refresh' content='90'>
        
</head>
  <body style="background:white">



<table border='0' cellpadding='0' cellspacing='0' width='98%'>
<tr>
<td align='center'>
<font face='arial' size='3'><b><x:out select="$challengeDocument/fll/@title" /> (Playoff
Round <%=playoffRoundNumber%>, Division <%=division%>)</b></font>
</td>
</tr>
<tr>
  <td align='center'>
    <font face='arial' size='3'>
      <b><c:out value="${ScorePageText}" /></b>
    </font>
  </td>
</tr>
</table>

</body>
</html>
