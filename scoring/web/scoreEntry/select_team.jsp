<%@ include file="/WEB-INF/jspf/init.jspf" %>

<% fll.web.scoreEntry.SelectTeam.populateContext(application, pageContext);%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  <title>Score Entry [Select Team]</title>

  <style title='local_style' type='text/css'>
   SELECT {line-height:150%; font-size:10pt; font-weight:bold; background:black; color:#e0e0e0; }
   OPTION {color:#e0e0e0; }
   .dark_bg  {font-size:10pt; font-weight:bold; background-color:black; color:#e0e0e0; }
  </style>
<script type="text/javascript" src="<c:url value='/playoff/code.icepush'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type='text/javascript'>

function editFlagBoxClicked() {
  var text = document.getElementById('select_number_text');
  if (document.selectTeam.EditFlag.checked) {
    document.selectTeam.RunNumber.disabled=false;
		text.style.color = "black";
  } else {
    document.selectTeam.RunNumber.disabled=true;
		text.style.color = "gray";
  }
}

function reloadRuns() {
  document.body.removeChild(document.getElementById('reloadruns'));
  document.verify.TeamNumber.length = 0;
  var s = document.createElement('script');
  s.type='text/javascript';
  s.id = 'reloadruns';
  s.src='unverifiedRunsObject.jsp?' + Math.random();
  document.body.appendChild(s);
}


$(document).ready(function() {
  editFlagBoxClicked();
  reloadRuns();
});
</script>
<icep:register group="dataentry" callback="function(){reloadRuns();}"/>
  </head>
  <body>
  
      <!-- top info bar -->
      <table width="100%" border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td align="center" valign="middle" bgcolor="#e0e0e0" colspan='3'>
            <table border="1" cellpadding="0" cellspacing="0" width="100%">
            <tr align="center" valign="middle"><td>

            <table border="0" cellpadding="5" cellspacing="0" width="90%">
              <tr>
                <td valign="middle" align="center">
                  <font face="Arial" size="4">${challengeDescription.title }</font><br>
									<font face="Arial" size="2">Score Card Entry and Review Page</font>
                </td>
              </tr>
            </table>

              </td></tr>
              </table>
          </td>
        </tr>
      </table>
      
      ${message}
      <%-- clear out the message, so that we don't see it again --%>
      <c:remove var="message" />
      

      <table> <!-- outer table -->        
        <tr>
        <td>
        <form action="GatherScoreEntryData" method="POST" name="selectTeam">
        <table> <!-- left table -->
        
        <tr align='left' valign='top'>
          <!-- pick team from a list -->
          <td>
            <br>
            <font face='arial' size='4'>Select team for this scorecard:</font><br>
            <select size='20' id='select-teamnumber' name='TeamNumber' ondblclick='selectTeam.submit()'>
              <c:forEach items="${tournamentTeams }" var="team">
                <c:if test="${not team.internal }">
                  <option value="${team.teamNumber }">${team.teamNumber }&nbsp;&nbsp;&nbsp;[${team.trimmedTeamName }]</option>
                </c:if>
              </c:forEach>
            </select>
          </td>
        </tr>
        <tr><td>
        <table border='1'>
        <tr>
           <!-- check to edit -->
          <td align='left' valign='bottom'>
            <input type="checkbox"
                   name='EditFlag'
                   id='EditFlagLeft'
                   value="1"
                   onclick="editFlagBoxClicked()" />
            <b><label for="EditFlagLeft">Correct or double-check this score</label></b>
          </td>
        </tr>
        <tr>
           <!-- pick run number -->
          <td align='left'>
            <select name='RunNumber' disabled='disabled'>
			  <option value='0'>Last Run</option>
	          <c:forEach var="index" begin="1" end="${maxRunNumber}">
			    <option value='${index }'>${index }</option>
			  </c:forEach>			
            </select>
            <b><span id='select_number_text'>Select Run Number for editing</span></b>
          </td>
        </tr>
        </table></td></tr>
        <tr>
          <!-- submit button -->
          <td align='left'>
            <!--<font face='arial' size='4'><b>Submit</b></font>-->
            <input class='dark_bg' type="submit" value="Submit" id='enter_submit'>
          </td>
        </tr>
      
      </table>
      </form>
      </td> <!-- left table -->
      
      <td valign='top'> <!-- right table -->
      <form action="GatherScoreEntryData" method="POST" name="verify">
      <input type="hidden" name='EditFlag' value="1" />
      
      <table>
        <tr align='left' valign='top'>
          <td>
            <!-- pick team from a list -->
            <br>
            <font face='arial' size='4'>Unverified Runs:</font><br>         
            <select size='20' id='select-verify-teamnumber' name='TeamNumber' ondblclick='verify.submit()'>
            </select>
          </td>
        </tr>
        <tr>
          <!-- submit button -->
          <td align='left'>
            <!--<font face='arial' size='4'><b>Submit</b></font>-->
            <input class='dark_bg' type="submit" id="verify_submit" value="Verify Score">
          </td>
        </tr>
        
      </table>
      </form>
      </td> <!-- right table -->     
      </tr> 

</table> <!-- outer table -->

        <script type="text/javascript" id="reloadruns" src="unverifiedRunsObject.jsp"></script>
    
  </body>
</html>
