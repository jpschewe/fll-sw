<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Developer Database Commands</title>

<script type='text/javascript' src='../extlib/jquery-1.11.1.min.js'></script>
<script type='text/javascript' src='../extlib/jquery.json-2.5-pre.min.js'></script>
<script type='text/javascript' src='query.js'></script>

<style>
.content table {
	border-collapse: collapse;
}

.content table,.content th,.content td {
	border: 1px solid black;
}
</style>
</head>

<body>
 <div class='content'>

  <h1>Developer Database Commands
  </h1>

  <p>
   <font color='red'><b>This page is intended for developers
     only. If you don't know what you're doing, LEAVE THIS PAGE!</b></font>
  </p>

  <c:if test="${not empty param.message}">
   <p>
    <i><c:out value="${param.message}" /></i>
   </p>
  </c:if>

  <p>
   Enter query
   <!--  must be on single line -->
   <textarea id='query' name='query' rows='5' cols='60'></textarea>
   <br />
   <button id='execute_query'>Execute Query</button>
  </p>
  <table id='query_result'>
  </table>

  <form name='update' method='post'>
   <p>
    Enter update
    <textarea name='update' rows='5' cols='60'>
     <c:out value="${param.update}" />
    </textarea>
   </p>
   <c:if test="${not empty param.update}">
    <sql:update dataSource="${datasource}" var="update_result"
     scope="page" sql="${param.update}" />
    <p>
     Modified rows:
     <c:out value="${update_result}" />
    </p>
   </c:if>
   <input type='submit' value='Execute Update' />
  </form>

 </div>

</body>
</html>
