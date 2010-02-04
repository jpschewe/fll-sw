<%@ include file="/WEB-INF/jspf/init.jspf" %>
<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.sql.Connection" %>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();

pageContext.setAttribute("divisions", Queries.getEventDivisions(connection));
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Finalist Scheduling</title>
    <script type='text/javascript'>
      function check_text() {
        var value = document.getElementById('num-finalists').value;
        if(!isInteger(value)) {
          alert("You must enter a number")
          document.getElementById('num-finalists').value = ""
        }   
      }
    </script>
  </head>

  <body>
    <p>${message}</p>
    <c:remove var="message"/>

    <form action='FinalistSchedulerUI'>
      <p>How many teams should be picked from each score group (per division) for finalists? 
      This number limits how many teams you can enter in non-scored categories. This
      number is also used as the number of teams from each score category that you can choose. You 
      will only be presented with this number of teams to choose to schedule. You can choose fewer 
      than this number of teams for the actual schedule, but never more.</p>
      <input type='hidden' name='init' value='1'/>
      <input type="text" id='num-finalists' name="num-finalists" onblur='check_text' size="10"/><br/>
      
      <p>Choose the division to create the schedule for.</p>
      <select name='division'>
        <c:forEach items="${divisions}" var="division">
          <option value='${division}'>${division }</option>
        </c:forEach>  
      </select><br/>
      
      <input type='submit' />
    </form>


  </body>
</html>
