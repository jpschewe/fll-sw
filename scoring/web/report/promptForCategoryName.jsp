<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%-- 
Prompt for how many teams should be considered from each scored category.
--%>


<%@ page import="fll.Team" %>
<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List" %>

<%@ page import="java.sql.Connection" %>


<%
	final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Finalist Scheduling</title>
    <script type='text/javascript'>
      function lookup_name(num_field, name_field) {
        var team_data = new Array();
        <%-- fill in team_data with current tournament team data --%>
        <%
        for(final Team team : Queries.getTournamentTeams(connection).values()) {
          out.print("team_data['");
          out.print(String.valueOf(team.getTeamNumber()));
          out.print("'] = '");
          out.print(String.valueOf(team.getTeamName()));
          out.println("';");
        }
        %>        

        var num = document.getElementById(num_field).value
        var name = team_data[num];
        if(typeof(name) != 'undefined') {
          document.getElementById(name_field).value = name;
        } else {
          document.getElementById(name_field).value = '';
          alert("Team number " + num + " does not exist");
        }
      }
    </script>
  </head>

  <body>
    <p>${message}</p>
    <c:remove var="message"/>
    

    <form action='FinalistSchedulerUI' method='POST'>
    <p>Would you like to define another category to schedule finalists for? Note that the team numbers entered are checked against the current tournament, but not against the appropriate division.<br/>
      Category name: <input type="text" name="new-category" size="30"/><br/>      
      <h2>Division ${division}</h2>
      <table>
      <tr><th>Place</th><th>Team Number</th><th>Team Name</th></tr>
      <c:forEach var='i' begin='1' end='${numFinalists}'>
        <tr>
          <td>${i}</td>
          <td>
            <input id='finalist-${i}' name='finalist-${i}' type='text' size='10' onblur="lookup_name('finalist-${i}', 'name-${i}')"/>
          </td>
          <td>
            <input id='name-${i}' type='text' size='30' readonly/>
          </td>
        </tr>
      </c:forEach> <%-- numFinalists --%>

      </table>
      
      <input type='submit' name='create-category' value='Add To Finalists'/><br/>
      <input type='submit' name='done' value='done'/>
    </form>


  </body>
</html>
