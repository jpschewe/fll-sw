<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
  
<html>
  <head>
    <title>Advance Teams Column Selection</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1>${challengeDescription.title } (Advance Team Column Selection)</h1>

	<p>Select the column that specifies the team numbers to be advanced to the next tournament.</p>
      
    <form action="ProcessAdvancingTeamsUpload" method="POST" name="advanceTeamColumnSelection">
      <table border='1'>

        <tr bgcolor='yellow'>
          <td>TeamNumber</td>
          <td>
            <select name='teamNumber'>
              <c:forEach items="${fileHeaders }" var="fileHeader">
                <option value="${fileHeader }">${fileHeader }</option>
              </c:forEach>
            </select>
          </td>
        </tr>
        
        <tr>
          <td colspan='2'><input type='submit' value='Submit'></td>
        </tr>
          
      </table> 
    </form>


  </body>
</html>
