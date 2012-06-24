<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.sql.Connection"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.ResultSet"%>

<c:if test="${not servletLoaded }">
<c:redirect url="index.jsp"/>
</c:if>
<c:remove var="servletLoaded"/>

<%
  final DataSource datasource = SessionAttributes.getDataSource(session);
  final Connection connection = datasource.getConnection();
  final int currentTournamentID = Queries.getCurrentTournament(connection);
  
%>

<c:if test="${not empty param.ScorePageText}">
  <c:set var="ScorePageText" value="${param.ScorePageText}" scope="application"/>
</c:if>


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
    an Excel file (xls and xslx supported). The
	filter functionality provided here is very basic and has very limited
	feedback. It's suggested that you edit the input file before upload to
	contain only the teams for your tournament(s). <b>This removes any existing teams from the database along with their scores.</b>

              <a href='javascript:display("UploadTeamsHelp")'>[help]</a>
	<div id='UploadTeamsHelp' class='help' style='display: none'>
          Each column of the input file needs to matched against the
          required data for teams. This information includes: team number,
          team name, organization, initial tournament, division. The team number
          must be a number and is required. The other columns are
          not required, but are a good idea to include. You will be prompted
          to pick a column from your data file to match against each piece
          of team data that the software uses. You can select the same column
          for multiple pieces of data.
	<a href='javascript:hide("UploadTeamsHelp")'>[hide]</a></div>
        
	  <input type="file" size="32" id='teams_file' name="file">
	  <input type='hidden' name='uploadRedirect' value="<c:url value='/admin/UploadTeams'/>" />
	  <input type="submit" id='upload_teams' value="Upload">
	</form>
	</li>
 

	<li><a href='<c:url value="tournaments.jsp"/>'>Add or Edit Tournaments</a>
  
 <a href='javascript:display("EditTournamentHelp")'>[help]</a>
	<div id='EditTournamentHelp' class='help' style='display: none'>
	This is an optional step. Use this page to modify the tournaments
	created by team import step
	above, to assign tournament advancement (e.g. teams may advance from
	regional tournaments to the state tournament), or to create new
	tournaments.<br>
	<a href='javascript:hide("EditTournamentHelp")'>[hide]</a></div>
	</li>


    <li><a  href='DisplayTournamentAssignments'>Display Tournament Assignments</a></li>

    
	<li>
	<form id='currentTournament' action='SetCurrentTournament'
		method="post">Current Tournament: <select id='currentTournamentSelect'
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
	</select> <input type='submit' name='change_tournament' value='Change tournament'></form>
	</li>
 
 
    <li>Upload schedule for current tournament.
    <a href='javascript:display("ScheduleHelp")'>[help]</a>
    <div id='ScheduleHelp' class='help' style='display: none'>
    Uploading the schedule isn't required, but if uploaded will be 
    used when displaying information in final reports.
    <a href='javascript:hide("ScheduleHelp")'>[hide]</a></div>
    <form id='uploadSchedule'
    action='<c:url value="/schedule/UploadSchedule"/>'
    METHOD="POST"
    ENCTYPE="multipart/form-data"
    >
    <input type="file" size="32" name="scheduleFile"/>
    <input type="submit" value="Upload Schedule"/>
    </form>
    </li>
    

	<li><a href='edit_event_division.jsp'> Assign
	event divisions to teams in current tournament</a>. <a
		href='javascript:display("EventDivisionHelp")'>[help]</a>
	<div id='EventDivisionHelp' class='help' style='display: none'>
	Typical tournaments have 2 groups of teams competing against each
	other, one for division 1 and one for division 2. If your tournament
	team groupings are not based solely on the division of the teams, e.g.
	you have 2 groups of teams that are all division 1, use this page to
	assign &ldquo;event divisions&rdquo; to divide your tournament&rsquo;s
	teams into the groups in which they will be competing.<br>
	<a href='javascript:hide("EventDivisionHelp")'>[hide]</a></div>
	</li>
 

	<li>
	<form id='changeScoresheetLayoutNUp'
		action='ChangeScoresheetLayout' method='post'>Select the
	number of scoresheets per printed page. <select
		name='scoresheetsPerPage'>
		<c:forEach begin="0" end="2" var="numSheets">
		<c:choose>
		<c:when test="${numSheets == scoressheetsPerPage}">
<option selected value='${numSheets}'>${numSheets }</option>
</c:when>
<c:otherwise>
<option value='${numSheets}'>${numSheets }</option>
</c:otherwise>
</c:choose>
					</c:forEach>
	</select> <input type='submit' name='changeScoresheetLayoutNUp' value='Commit'>
	</form>
	</li>
 

	<li>
	<form id='changeSeedingRounds' action='ChangeSeedingRounds'
		method='post'>Select the number of seeding runs. <select
		name='seedingRounds'>
		<c:forEach begin="0" end="10" var="numRounds">
		<c:choose>
		<c:when test="${numRounds == numSeedingRounds}">
<option selected value='${numRounds}'>${numRounds}</option>
</c:when>
<c:otherwise>
<option value='${numSheets}'>${numSheets }</option>
</c:otherwise>
</c:choose>

					</c:forEach>
	</select> <input type='submit' name='changeSeedingRounds' value='Commit'>
	</form>
	</li>


</ol>

<p>Tournament day:</p>
<ol>
      <li>
        <form action='index.jsp' method='post'>
          Score page text: 
          <input type='text' name='ScorePageText' value='<c:out value="${ScorePageText}"/>'>
          <input type='submit' value='Change text'>
        </form>
      </li>


	<li><a href='<c:url value="GatherJudgeInformation"/>' id='assign_judges'>Assign Judges</a></li>
	
 
	<li><a href='<c:url value="tables.jsp"/>'>Assign Table Labels</a>
	(for scoresheet printing during playoffs)</li>



    <c:if test="${not playoffsInitialized}">
	<li><a
		href='<c:url value="GatherTeamData">
                     <c:param name="addTeam" value="1"/>
                   </c:url>'>Add
	a team</a></li>
    </c:if>
    
    
	<li><a href='<c:url value="select_team.jsp"/>'>Edit team data</a></li>


	<li><a href="subjective-data.fll">Download
	the datafile for subjective score entry.</a> Should be downloaded after
	each subjective score upload to lessen chance of data loss due to
	overwrite.</li>
 
 
	<li>
	<form name='uploadSubjective'
		ACTION='<c:url value="UploadSubjectiveData"/>' METHOD="POST"
		ENCTYPE="multipart/form-data">Upload the datafile for
	subjective scores. <input type="file" size="32" name="subjectiveFile"/>
	<input type="submit" value="Upload"/></form>
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
		<input type="file" size="32" name="file"/>
		<input type='hidden' name='uploadRedirect' value="<c:url value='/admin/UploadAdvancingTeams'/>" />
		<input type="submit" value="Upload"/>
	</form>
	</li>
</ul>


</body>
</html>
