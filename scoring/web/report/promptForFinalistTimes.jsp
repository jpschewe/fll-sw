<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%-- 
Prompt for finalist time information.
--%>


<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Finalist Scheduling (pick times)</title>
  </head>

  <body>
    <p>${message}</p>
    <c:remove var="message"/>    

    <form action='FinalistSchedulerUI' method='POST'>
    <p>Choose the start time for the finalist scheduling and the interval in minutes</p>    
      <h2>Division ${division}</h2>
      <p>Start time (hour and minute): <input type="text" name="hour" size="2" maxlength="2"/> : <input type="text" name="minute" size="2" maxlength="2"/></p>
      <p>Interval (minutes): <input type="text" name="interval"/></p>
      
      <input type='submit' name='submit-times' value='Submit'/><br/>
    </form>

  </body>
</html>
