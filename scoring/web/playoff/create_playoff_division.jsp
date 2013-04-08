<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.playoff.CreatePlayoffDivision.populateContext(application,
					pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Create Playoff Division</title>


<script type='text/javascript' src='../extlib/jquery-1.7.1.min.js'></script>

<script type='text/javascript'>
$(document).ready(
        function() {
        
        <c:forEach items="${judgingStations }" var="station" varStatus="idx">
        <!--  select/unselect ${station} -->
$("#station_select_${idx.count }").change(function() {
        	<c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
        	   <c:if test="${team.judgingStation == station}">
        	   if($(this).is(":checked")) {
        		   $("#select_${team.teamNumber}").attr("checked", "checked");
        	   } else {
        	    $("#select_${team.teamNumber}").removeAttr("checked");
        	   }
        	   </c:if>
          </c:forEach>
          });
    </c:forEach>
});
</script>

</head>

<body>
 <h1>Create Playoff Division</h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <p>Choose the teams that you want to include in the custom event
  division. This group of teams will compete against each other in a
  single elimination playoff bracket.</p>

 <form method="POST" action="CreatePlayoffDivision">

  <label for="division_name">Name: </label><input name="division_name" />

  <div>Select/unselect teams by judging station</div>
  <c:forEach items="${judgingStations }" var="station" varStatus="idx">
   <div>
    <input type="checkbox" name="station_select_${idx.count}"
     id="station_select_${idx.count }" /> <label
     for="station_select_${idx.count}">${station }</label>
   </div>
  </c:forEach>

  <table border='1'>

   <tr>
    <th>Select</th>
    <th>Number</th>
    <th>Name</th>
    <th>Judging Station</th>
   </tr>
   <c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
    <tr>

     <td><input name="selected_team"
      id="select_${team.teamNumber }" type="checkbox"
      value="${team.teamNumber }" /></td>

     <td>${team.teamNumber }</td>

     <td>${team.teamName }</td>

     <td>${team.judgingStation }</td>

    </tr>
   </c:forEach>
  </table>

  <input type='submit' value='Submit' />

 </form>

</body>
</html>
