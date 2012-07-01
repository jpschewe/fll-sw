<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List" %>

<%@ page import="java.sql.Connection" %>
  
<%
final DataSource datasource = ApplicationAttributes.getDataSource();
final Connection connection = datasource.getConnection();
final List<String> divisions = Queries.getEventDivisions(connection);
pageContext.setAttribute("divisions", divisions);
final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection);
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Playoff's</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Playoff menu)</h1>
      <ol>
        <li>If using the automatic table assignment feature for scoresheet generation, make
        certain to set up labels for each of your tables, available from the Admin page or by
        clicking <a href='<c:url value="/admin/tables.jsp"/>'>here</a>.</li>

        <li>Check to make sure all teams have scores entered for each seeding round.<br/>
          <form name='check' action='check.jsp' method='get'>
          Select Division:
          <select name='division'>
            <c:forEach items="${divisions }" var="division">
            <option value='${division}'>${division}</option>
            </c:forEach>
          </select>
          <input type='submit' id='check_seeding_rounds' value='Check Seeding Rounds'/>
          </form>

        <li>
          <b>WARNING: Do not initialize playoff brackets for a division until all seeding
          runs for that division have been recorded!</b><br>
          <form name='initialize' action='initializebrackets.jsp' method='post'>
          Select Division: <select name='division'>
            <c:forEach items="${divisions }" var="division">
            <option value='${division}'>${division}</option>
            </c:forEach>
          </select><br>
          <input type='checkbox' name='enableThird' value='yes'/>Check to enable 3rd/4th place brackets<br>
          <input type='submit' id='initialize_brackets' value='Initialize Brackets'/>
          </form>

        <li>
          <form name='admin' action='adminbrackets.jsp' method='get'>
            <b>Printable Brackets</b><br/>
            Select Division: <select name='division'>
            <c:forEach items="${divisions }" var="division">
            <option value='${division}'>${division}</option>
            </c:forEach>
            </select>
            from round <select name='firstRound'>
<%
for(int numRounds = 1; numRounds <= numPlayoffRounds; numRounds++) {
  out.print("<option value='" + numRounds + "'");
  if(numRounds == 1) {
    out.print(" selected");
  }
  out.println(">" + numRounds + "</option>");
}
%>
            </select> to <select name='lastRound'>
<%
// numPlayoffRounds+1 == the column in which the 1st place winner is displayed
for(int numRounds = 2; numRounds <= numPlayoffRounds+1; numRounds++) {
  out.print("<option value='" + numRounds + "'");
  if(numRounds == numPlayoffRounds+1) {
    out.print(" selected");
  }
  out.println(">" + numRounds + "</option>");
}
%>
</select>
            <input type='submit' id='display_printable_brackets' value='Display Brackets'>
          </form>               
        </li>

        <li>
          <form name='printable' action='scoregenbrackets.jsp' method='get'>
            <b>Scoresheet Generation Brackets</b><br/>
            Select Division: <select name='division'>
            <c:forEach items="${divisions }" var="division">
            <option value='${division}'>${division}</option>
            </c:forEach>            </select>
            from round <select name='firstRound'>
<%
for(int numRounds = 1; numRounds <= numPlayoffRounds; numRounds++) {
  out.print("<option value='" + numRounds + "'");
  if(numRounds == 1) {
    out.print(" selected");
  }
  out.println(">" + numRounds + "</option>");
}
%>
            </select> to <select name='lastRound'>
<%
// numPlayoffRounds+1 == the column in which the 1st place winner is displayed
for(int numRounds = 2; numRounds <= numPlayoffRounds+1; numRounds++) {
  out.print("<option value='" + numRounds + "'");
  if(numRounds == numPlayoffRounds+1) {
    out.print(" selected");
  }
  out.println(">" + numRounds + "</option>");
}
%>
            </select>
            <input type='submit' id='display_scoregen_brackets' value='Display Brackets'>
          </form>
        </li>

        <li>
          <b>Scrolling Brackets</b> (as on big screen display)<br/>
          <a href="remoteMain.jsp">Display brackets</a><br/>
          Division and round must be selected from the big screen display
          <a href="<c:url value='/admin/remoteControl.jsp'/>">remote
          control</a> page.
        </li>
      </ol>


  </body>
</html>
