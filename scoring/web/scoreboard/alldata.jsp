<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Utilities" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = (String)application.getAttribute("currentTournament");
final Statement stmt = connection.createStatement();
final String sql = "SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.Region, Teams.Division, Performance.Tournament, Performance.RunNumber, Performance.Bye, Performance.NoShow, Performance.ComputedTotal"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " ORDER BY Teams.Organization, Teams.TeamNumber, Performance.RunNumber";
final ResultSet rs = stmt.executeQuery(sql);
%>

<HTML>
  <head>
    <style>
      FONT {color: #ffffff; font-family: "Arial"}
      TABLE.A {background-color:#000080 }
      TABLE.B {background-color:#0000d0 }
    </style>
  </head>

  <body bgcolor='#000080'>
<%@ include file="teams_body.jsp" %>
  </body>
<%
  Utilities.closeResultSet(rs);
  Utilities.closeStatement(stmt);
%>
</HTML>
