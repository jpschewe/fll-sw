<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />

</head>

<body>
 <h1>Choose Subjective Headers</h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <form name="choose_headers" method='POST'
  action='ProcessSubjectiveHeaders'>
  <p>Match the columns in the schedule with the subjective
   categories that they contain the schedule for. If a schedule column
   is for a subjective category, then the amount of time must be
   specified as well.</p>

  <p>You cannot have the same subjective category mapped to 2
   schedule columns.</p>

  <table border='1'>
   <tr>
    <th>Schedule Column</th>
    <th>Duration in Minutes</th>

    <c:forEach items="${challengeDescription.subjectiveCategories }"
     var="subcat">
     <th>${subcat.title}</th>
    </c:forEach>

   </tr>
   <c:forEach items="${uploadSchedule_unusedHeaders }" var="subjHeader"
    varStatus="loopStatus">
    <c:if test="${fn:length(subjHeader) > 0 }">
     <tr>
      <th>${subjHeader}</th>

      <!--  TODO ticket:94 need to validate that this is a number -->
      <td><input type="text" name="${loopStatus.index}:duration"
       value="${default_duration}" /></td>

      <c:forEach items="${challengeDescription.subjectiveCategories }"
       var="subcat">
       <td><input type="checkbox"
        name="${loopStatus.index}:${subcat.name}" /></td>
      </c:forEach>

     </tr>
    </c:if>
   </c:forEach>
  </table>

  <input type="submit" />
 </form>

</body>
</html>