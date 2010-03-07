<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.sql.Connection"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.ResultSet"%>

<%
  final DataSource datasource = SessionAttributes.getDataSource(session);
  final Connection connection = datasource.getConnection();
  final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
  final int scoresheetsPerPage = Queries.getScoresheetLayoutNUp(connection);
  final int currentTournamentID = Queries.getCurrentTournament(connection);
%>

<html>
<head>
<title>Administration</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" />
(Administration)</h1>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<p>Before tournament day:</p>
<ol>
	<li>
	<form id='uploadTeams' 
	    ACTION="<c:url value='/UploadSpreadsheet'/>"
		METHOD="POST" 
		ENCTYPE="multipart/form-data">
		Upload the datafile
	for teams. This file can be tab separated or comma separated or 
    an Excel file (xls and xslx supported - although only the first sheet is read). The
	filter functionality provided here is very basic and has very limited
	feedback. It's suggested that you edit the input file before upload to
	contain only the teams for your tournament(s).
	  <input type="file" size="32" name="file">
	  <input type='hidden' name='uploadRedirect' value="<c:url value='/admin/UploadTeams'/>" />
	  <input type="submit" value="Upload">
	</form>
	</li>

	<li><a
		href='AddTournamentsForRegions'>Add
	tournaments for all Regions</a> <a
		href='javascript:display("AddForAllHelp")'>[help]</a>
	<div id='AddForAllHelp' class='help' style='display: none'>Click
	here to automatically create a tournament for each unique region name
	from the imported team data file. If you choose not to use this link,
	you must manually create tournaments through the &ldquo;Edit
	Tournaments&rdquo; interface, below.<br>
	<a href='javascript:hide("AddForAllHelp")'>[hide]</a></div>
	</li>

	<li><a href='<c:url value="tournamentInitialization.jsp"/>'>
	Initialize tournament teams by region</a> <a
		href='javascript:display("InitByRegionHelp")'>[help]</a>
	<div id='InitByRegionHelp' class='help' style='display: none'>
	Teams are initially assigned to the DUMMY tournament. Click here to
	automatically assign each team to the tournament with the exact same
	name as the region to which the team is assigned.<br>
	<a href='javascript:hide("InitByRegionHelp")'>[hide]</a></div>
	</li>

	<li><a href='<c:url value="tournaments.jsp"/>'>Edit
	Tournaments</a> <a href='javascript:display("EditTournamentHelp")'>[help]</a>
	<div id='EditTournamentHelp' class='help' style='display: none'>
	This is an optional step. Use this page to modify the tournaments
	created by the &ldquo;Add tournaments for all Regions&rdquo; step
	above, to assign tournament advancement (e.g. teams may advance from
	regional tournaments to the state tournament), or to create new
	tournaments.<br>
	<a href='javascript:hide("EditTournamentHelp")'>[hide]</a></div>
	</li>

    <li><a  href='DisplayTournamentAssignments'>Display Tournament Assignments</a></li>
    
	<li>
	<form id='currentTournament' action='SetCurrentTournament'
		method="post">Current Tournament: <select
		name='currentTournament'>
		<%
		  final Statement stmt = connection.createStatement();
		  final ResultSet rs = stmt.executeQuery("Select tournament_id,Name,Location from Tournaments ORDER BY Name");
		  while (rs.next()) {
		    final int tournamentID = rs.getInt(1);
		    final String tournament = rs.getString(2);
		    final String location = rs.getString(3);
		    out.print("<option value='"
		        + tournamentID + "'");
		    if (currentTournamentID == tournamentID) {
		      out.print(" selected");
		    }
		    out.println(">"
		        + location + " [ " + tournament + " ]</option>");
		  }
		  rs.close();
		  stmt.close();
		%>
	</select> <input type='submit' value='Change tournament'></form>
	</li>

	<li><a href='<c:url value="edit_event_division.jsp"/>'> Assign
	event divisions to teams in current tournament</a>. <a
		href='javascript:display("EventDivisionHelp")'>[help]</a>
	<div id='EventDivisionHelp' class='help' style='display: none'>
	Typical tournaments have 2 groups of teams competing against each
	other, one for division 1 and one for division 2. If your tournament
	team groupings are not based solely on the divsion of the teams, e.g.
	you have 2 groups of teams that are all division 1, use this page to
	assign &ldquo;event divsions&rdquo; to divide your tournament&rsquo;s
	teams into the groups in which they will be competing.<br>
	<a href='javascript:hide("EventDivisionHelp")'>[hide]</a></div>
	</li>

	<li>
	<form id='changeScoresheetLayoutNUp'
		action='ChangeScoresheetLayout' method='post'>Select the
	number of scoresheets per printed page. <select
		name='scoresheetsPerPage'>
		<%
		  for (int i = 1; i <= 2; i++) {
		    out.print("<option value='"
		        + i + "'");
		    if (scoresheetsPerPage == i) {
		      out.print(" selected");
		    }
		    out.println(">"
		        + i + "</option>");
		  }
		%>
	</select> <input type='submit' name='changeScoresheetLayoutNUp' value='Commit'>
	</form>
	</li>

	<li>
	<form id='changeSeedingRounds' action='ChangeSeedingRounds'
		method='post'>Select the number of seeding runs. <select
		name='seedingRounds'>
		<%
		  for (int i = 0; i <= 10; i++) {
		    out.print("<option value='"
		        + i + "'");
		    if (numSeedingRounds == i) {
		      out.print(" selected");
		    }
		    out.println(">"
		        + i + "</option>");
		  }
		%>
	</select> <input type='submit' name='changeSeedingRounds' value='Commit'>
	</form>
	</li>

</ol>

<p>Tournament day:</p>
<ol>
	<li><a href='<c:url value="judges.jsp"/>'>Assign Judges</a></li>
	
	<li><a href='<c:url value="tables.jsp"/>'>Assign Table Labels</a>
	(for scoresheet printing during playoffs)</li>


	<li><a
		href='<c:url value="GatherTeamData">
                     <c:param name="addTeam" value="1"/>
                   </c:url>'>Add
	a team</a></li>

	<li><a href='<c:url value="select_team.jsp"/>'>Edit team data</a></li>

	<li><a href="subjective-data.fll">Download
	the datafile for subjective score entry.</a> Should be downloaded after
	each subjective score upload to lessen chance of data loss due to
	overwrite.</li>
	<li>
	<form name='uploadSubjective'
		ACTION='<c:url value="UploadSubjectiveData"/>' METHOD="POST"
		ENCTYPE="multipart/form-data">Upload the datafile for
	subjective scores. <input type="file" size="32" name="subjectiveFile">
	<input type="submit" value="Upload"></form>
	</li>

	<li><a href='remoteControl.jsp'>Remote control of display</a></li>
</ol>

<p>After the tournament</p>
<ul>
	<li><a href='database.flldb'>Download database</a></li>
	<li><a href="GatherAdvancementData">Advance teams</a></li>
	<li>
	<form id='uploadAdvancingTeams' 
	    ACTION="<c:url value='/UploadSpreadsheet'/>"
		METHOD="POST" 
		ENCTYPE="multipart/form-data">
		Upload CSV or Excel of teams to advance		 
		<input type="file" size="32" name="file">
		<input type='hidden' name='uploadRedirect' value="<c:url value='/admin/UploadAdvancingTeams'/>" />
		<input type="submit" value="Upload">
	</form>
	</li>
</ul>


</body>
</html>
