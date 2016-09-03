<%@page import="fll.web.admin.GatherJudgeInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Judge Assignments</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript' src='../extlib/jquery-1.11.1.min.js'></script>

<script type='text/javascript'>
$(document).ready(function() {

    $("#commit").click(function() {
    	location.href='CommitJudges';
    });

    $("#cancel").click(function() {
        location.href='index.jsp';
    });

}); // end ready function

</script>

</head>

<body>
 <h1>
  ${challengeDescription.title } (Judge Assignment Verification)
 </h1>


 <div class='status-message'>${message}</div>
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

<p>Please verify that these judges look correct and then commit the changes. 
If changes need to be made to the judges, press the back button.</p>

 <table border='1' id='data'>
  <tr>
   <th>ID</th>
   <th>Category</th>
   <th>Judging Group</th>
  </tr>

  <c:forEach items="${JUDGES }" var="judge">

   <tr>

    <td>${judge.id }</td>
    <td>${judge.category }</td>
    <td>${judge.group }</td>

   </tr>

  </c:forEach>

 </table>

<br/>
 <button id='commit'>Commit the judges</button>
 
 <button id='cancel'>Cancel</button>
 
</body>
</html>
