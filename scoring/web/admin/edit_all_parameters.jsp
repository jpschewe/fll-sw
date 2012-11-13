<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.TournamentParameters"%>

<c:if test="${not servletLoaded }">
 <c:redirect url="GatherParameterInformation" />
</c:if>
<c:remove var="servletLoaded" />

<html>
<head>
<title>Edit Parameters</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />

<style>
.content table {
	border-collapse: collapse;
}

.content table,.content th,.content td {
	border: 1px solid black;
}

.content td,.content td {
	text-align: center;
}
</style>

</head>

<body>

 <div class='content'>

  <h1>Edit All Parameters</h1>

  <p>This page is for advanced users only. Be careful changing
   parameters here.</p>

  <form name='edit_parameters' action='ChangeParameters' method='POST'>
   <h2>Tournament Parameters</h2>
   <p>These parameters are specified per tournament. Each of them
    has a default value that is used if no value is specified for the
    tournament</p>

   <table>

    <tr>
     <th>Parameter</th>
     <th>Default Value</th>

     <c:forEach items="${tournaments }" var="tournament">
      <th>${tournament.name }</th>
     </c:forEach>

    </tr>

    <tr>
     <th>Seeding Rounds</th>

     <td><select name='seeding_rounds_default'>
       <c:forEach begin="0" end="10" var="numRounds">
        <c:choose>
         <c:when test="${numRounds == numSeedingRounds_default}">
          <option selected value='${numRounds}'>${numRounds}</option>
         </c:when>
         <c:otherwise>
          <option value='${numRounds}'>${numRounds }</option>
         </c:otherwise>
        </c:choose>
       </c:forEach>
     </select></td>

     <c:forEach items="${tournaments }" var="tournament">

      <td><select name='seeding_rounds_${tournament.tournamentID }'>
        <c:choose>
         <c:when
          test="${empty numSeedingRounds[tournament.tournamentID]}">
          <option selected value="default">Default</option>
         </c:when>
         <c:otherwise>
          <option value="default">Default</option>
         </c:otherwise>
        </c:choose>

        <c:forEach begin="0" end="10" var="numRounds">
         <c:choose>
          <c:when
           test="${numRounds == numSeedingRounds[tournament.tournamentID]}">
           <option selected value='${numRounds}'>${numRounds}</option>
          </c:when>
          <c:otherwise>
           <option value='${numRounds}'>${numRounds }</option>
          </c:otherwise>
         </c:choose>
        </c:forEach>
      </select></td>

     </c:forEach>

    </tr>


    <tr>
     <th>Max Scoreboard Round</th>

     <td><select name='max_scoreboard_round_default'>
       <c:forEach begin="0" end="10" var="numRounds">
        <c:choose>
         <c:when test="${numRounds == maxScoreboardRound_default}">
          <option selected value='${numRounds}'>${numRounds}</option>
         </c:when>
         <c:otherwise>
          <option value='${numRounds}'>${numRounds }</option>
         </c:otherwise>
        </c:choose>
       </c:forEach>
     </select></td>

     <c:forEach items="${tournaments }" var="tournament">

      <td><select
       name='max_scoreboard_round_${tournament.tournamentID }'>
        <c:choose>
         <c:when
          test="${empty maxScoreboardRound[tournament.tournamentID]}">
          <option selected value="default">Default</option>
         </c:when>
         <c:otherwise>
          <option value="default">Default</option>
         </c:otherwise>
        </c:choose>

        <c:forEach begin="0" end="10" var="numRounds">
         <c:choose>
          <c:when
           test="${numRounds == maxScoreboardRound[tournament.tournamentID]}">
           <option selected value='${numRounds}'>${numRounds}</option>
          </c:when>
          <c:otherwise>
           <option value='${numRounds}'>${numRounds }</option>
          </c:otherwise>
         </c:choose>
        </c:forEach>
      </select></td>

     </c:forEach>

    </tr>


   </table>


   <!--  FIXME global parameters -->

   <input type='submit' value='Save Changes' />
  </form>

 </div>

</body>
</html>
