<%@page import="fll.web.admin.GatherJudgeInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Judge Assignments</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />

<script type='text/javascript' src='../extlib/jquery-1.7.1.min.js'></script>
</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
  (Judge Assignment Verification)
 </h1>


 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />


 <table border='1' id='data'>
  <tr>
   <th>ID</th>
   <th>Category</th>
   <th>Division</th>
  </tr>

  <c:forEach items="${JUDGES }" var="judge">

   <tr>

    <td>${judge.id }</td>
    <td>${judge.category }</td>
    <td>${judge.division }</td>

   </tr>

  </c:forEach>

 </table>

 <a href='CommitJudges'>Commit</a>
 <a href='index.jsp'>Cancel</a>
</body>
</html>
