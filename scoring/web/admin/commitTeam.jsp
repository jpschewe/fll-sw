<%@ page errorPage="../errorHandler.jsp" %>
  
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Types" %>

<%@ page import="java.text.NumberFormat" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("adminConnection");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Commit Team)</title>
  </head>

  <body background='../images/bricks1.gif' bgcolor='#ffffff' topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Commit Team)</h1>

<%
//parse the numbers first so that we don't get a partial commit
final int teamNumber = NumberFormat.getInstance().parse(request.getParameter("teamNumber")).intValue();
final String divisionStr = request.getParameter("division");
final int division;
if(null != divisionStr && !"".equals(divisionStr)) {
  division = NumberFormat.getInstance().parse(divisionStr).intValue();
} else {
  division = -1;
}
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

if("1".equals(request.getParameter("deleteTeam"))) { //check for the delete flag
  //check for reallyDelete
  if("1".equals(request.getParameter("reallyDelete"))) {
    //delete
    Queries.deleteTeam(teamNumber, challengeDocument, connection, application);
%>
    <a href="select_team.jsp">Normally you'd be redirected here</a>
    <% response.sendRedirect(response.encodeRedirectURL("select_team.jsp")); %>
<%
  } else {
    //create hidden form
%>
  <form action='commitTeam.jsp' method='post'>
  <p>Are you sure you want to delete team <%=teamNumber%>?  Any data associated with that team will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while a tournament is running.</p>
  <input type='hidden' name='teamNumber' value='<%=teamNumber%>'>
  <input type='hidden' name='division' value='<%=division%>'>
  <input type='hidden' name='numBoys' value='<%=numBoys%>'>
  <input type='hidden' name='numGirls' value='<%=numGirls%>'>
  <input type='hidden' name='numMedals' value='<%=numMedals%>'>
  <input type='hidden' name='teamName' value='<%=request.getParameter("teamName")%>'>
  <input type='hidden' name='organization' value='<%=request.getParameter("organization")%>'>
  <input type='hidden' name='coach' value='<%=request.getParameter("coach")%>'>
  <input type='hidden' name='email' value='<%=request.getParameter("email")%>'>
  <input type='hidden' name='phone' value='<%=request.getParameter("phone")%>'>
  <input type='hidden' name='city' value='<%=request.getParameter("city")%>'>
  <input type='hidden' name='tournament' value='<%=request.getParameter("tournament")%>'>
  <input type='hidden' name='reallyDelete' value='1'>
  <input type='hidden' name='deleteTeam' value='1'>
  <input type='submit' value='Delete'>
  </form>
<%
  }
} else {
  final PreparedStatement prep;
  //check for addTeam flag
  if("1".equals(request.getParameter("addTeam"))) {
    //need to check for duplicate teamNumber
    final Statement stmt = connection.createStatement();
    final ResultSet rs = stmt.executeQuery("Select TeamName FROM Teams Where TeamNumber = " + teamNumber);
    if(rs.next()) {
      prep = null;
%>
      <p>Error, team number <%=teamNumber%> is already assigned to <%=rs.getString(1)%>.
      <a href="index.jsp">Return to the admin menu.</a></p>
<%
    } else {
      prep = connection.prepareStatement("INSERT INTO Teams (TeamName, Organization, Coach, Email, Phone, City, Region, Division, NumBoys, NumGirls, NumMedals, HowFoundOut, TeamNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }
  } else {
    prep = connection.prepareStatement("UPDATE Teams SET TeamName = ?, Organization = ?, Coach = ?, Email = ?, Phone = ?, City = ?, Region = ?, Division = ?, NumBoys = ?, NumGirls = ?, NumMedals = ?, HowFoundOut = ? WHERE TeamNumber = ?");
  }
      
  prep.setString(1, request.getParameter("teamName"));
  prep.setString(2, request.getParameter("organization"));
  prep.setString(3, request.getParameter("coach"));
  prep.setString(4, request.getParameter("email"));
  prep.setString(5, request.getParameter("phone"));
  prep.setString(6, request.getParameter("city"));
  prep.setString(7, request.getParameter("tournament"));
  if(-1 == division) {
    prep.setNull(8, Types.INTEGER);
  } else {
    prep.setInt(8, division);
  }
  if(-1 == numBoys) {
    prep.setNull(9, Types.INTEGER);
  } else {
    prep.setInt(9, numBoys);
  }
  if(-1 == numGirls) {
    prep.setNull(10, Types.INTEGER);
  } else {
    prep.setInt(10, numGirls);
  }
  if(-1 == numMedals) {
    prep.setNull(11, Types.INTEGER);
  } else {
    prep.setInt(11, numMedals);
  }
  prep.setString(12, request.getParameter("howFoundOut"));
  prep.setInt(13, teamNumber);
  prep.executeUpdate();
  Utilities.closePreparedStatement(prep);

  Queries.initializeTournamentTeams(connection);
  Queries.populateTournamentTeams(application);
%>

<%if("1".equals(request.getParameter("addTeam"))) {%>
    application.removeAttribute("tournamentTeams");
    <a href="index.jsp">Normally you'd be redirected here</a>
    <% response.sendRedirect(response.encodeRedirectURL("index.jsp")); %>
<%} else {%>
    <a href="select_team.jsp">Normally you'd be redirected here</a>
    <% response.sendRedirect(response.encodeRedirectURL("select_team.jsp")); %>
<%}%>
<%}%>
      
<%@ include file="../WEB-INF/jspf/footer.jspf" %>    
  </body>
</html>
