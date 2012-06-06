<%@page import="fll.web.admin.GatherJudgeInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Judge Assignments</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />

<script type='text/javascript' src='../extlib/jquery-1.7.1.min.js'></script>

<script type='text/javascript'>
	
	var maxIndex = ${fn:length(JUDGES)};

	var judging_stations = [];
	<c:forEach items="${JUDGING_STATIONS}" var="stationName">
	judging_stations.push("${stationName}");
	</c:forEach>

	var categories = {};
	<c:forEach items="${CATEGORIES}" var="cat">
	categories["${cat.key}"] = "${cat.value}";
	</c:forEach>
</script>

<script type='text/javascript' src='judges.js'></script>



</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
  (Judge Assignments)
 </h1>

 <p>Judges ID's must be unique. They can be just the name of the
  judge. Keep in mind that this ID needs to be entered on the judging
  forms. There must be at least 1 judge for each category.</p>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />


 <form action='VerifyJudges' method='POST' name='judges'>

 <input type='hidden' name='total_num_rows' id='total_num_rows' />

  <table border='1' id='data'>
   <tr>
    <th>ID</th>
    <th>Category</th>
    <th>Judging Station</th>
   </tr>

   <c:forEach items="${JUDGES }" var="judge" varStatus="loopStatus">

    <tr>

     <td><input type='text' value='${judge.id }'
      name='id${loopStatus.count}' /></td>
     <td><select name='cat${loopStatus.count }'>
       <c:forEach items="${CATEGORIES}" var="cat">

        <c:choose>
         <c:when test="${judge.category == cat.key }">
          <option value="${cat.key}" selected>${cat.value }</option>
         </c:when>
         <c:otherwise>
          <option value="${cat.key}">${cat.value }</option>
         </c:otherwise>
        </c:choose>

       </c:forEach>

     </select></td>

     <td><select name='station${loopStatus.count }'>
       <c:forEach items="${JUDGING_STATIONS}" var="stationSel">

        <c:choose>
         <c:when test="${judge.station == stationSel }">
          <option value="${stationSel}" selected>${stationSel }</option>
         </c:when>
         <c:otherwise>
          <option value="${stationSel}">${stationSel }</option>
         </c:otherwise>
        </c:choose>

       </c:forEach>
     </select></td>

    </tr>

   </c:forEach>

  </table>

  <input type='text' name='num_rows' id='num_rows' value='1' size='10' />
  <button id='add_rows'>Add Rows</button>
  <br /> <input type='submit' id='finished' name='finished' value='Finished' /><br />

 </form>

</body>
</html>
