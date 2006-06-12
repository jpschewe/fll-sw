<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
  
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>

<%@ page import="java.sql.Connection" %>
  
<%
final Connection connection = (Connection)application.getAttribute("connection");
      
final Map tournamentTeams = Queries.getTournamentTeams(connection);
final String currentTournament = Queries.getCurrentTournament(connection);
final List divisions = Queries.getDivisions(connection);
final int numSeedingRounds = Queries.getNumSeedingRounds(connection);

%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Playoff's)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Playoff menu)</h1>
      <ol>
        <li>If using the automatic table assignment feature for scoresheet generation, make
        certain to set up labels for each of your tables, available from the Admin page or by
        clicking <a href='/fll-sw/admin/tables.jsp'>here</a>.

        <li>Check to make sure all teams have scores entered for each seeding round.<br/>
          <form action='check.jsp' method='get'>
          Select a division and check seeding rounds
          <select name='division'>
          <option value='__all__' selected>All</option>
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
%>
          <option value='<%=div%>'><%=div%></option>
<%
}
}
%>
          </select>
          <input type='submit' value='Check Seeding Rounds'/>
          </form>

        <li>
          <b>WARNING: Do not initialize playoff brackets for a division until all seeding
          runs for that division have been recorded!</b><br>
          <form action='initializebrackets.jsp' method='post'>
          <select name='division'>
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
%>
          <option value='<%=div%>'><%=div%></option>
<%
}
}
%>
          </select><br>
          <input type='checkbox' name='enableThird' value='yes'/>Check to enable 3rd/4th place brackets<br>
          <input type='submit' value='Initialize Brackets'/>
          </form>

        <li>
          <form action='adminbrackets.jsp' method='get'>
            Go to the admin/printable bracket page for division <select name='division'>
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
%>
<option value='<%=div%>'><%=div%></option>
<%
}
}
%>
            </select>
            <input type='submit' value='Go to Playoffs'>
          </form>               
        </li>

        <li>
          <B>WARNING: Do not select brackets until all seeding runs have been recorded!</b><br>
          <form action='scoregenbrackets.jsp' method='get'>
            <B>EXPERIMENTAL!</B> Go to the scoresheet generation/admin/printable bracket page for division <select name='division'>
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
%>
<option value='<%=div%>'><%=div%></option>
<%
}
}
%>
            </select>
            <input type='submit' value='Go to Playoffs'>
          </form>
        </li>

        <li>
          <B>WARNING: Do not select brackets until all seeding runs have been recorded!</b><br>
          <a href="remoteMain.jsp">Go to remotely controlled brackets</a>
          These brackets can be controlled from the admin page.
        </li>
      </ol>
    </p>



<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
