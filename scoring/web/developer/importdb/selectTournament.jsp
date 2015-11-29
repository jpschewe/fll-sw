<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.developer.importdb.SelectTournament.populateContext(application, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/style.jsp'/>" />
<title>Select Tournament to import</title>
</head>

<body>
  ${message}
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <form
    name="selectTournament"
    action="CheckTournamentExists">
    <p>Select a tournament to import</p>
    <select name="tournament">
      <c:forEach
        var="tournament"
        items="${tournaments}">
        <option value="${tournament.name}">${tournament.location}
          [ ${tournament.name} ]</option>
      </c:forEach>
    </select> <input
      name='submit'
      type='submit'
      value='Select Tournament' />
  </form>

  <p>
    If you're don't want to import any of these tournaments you can <a
      href="<c:url value='/developer/index.jsp'/>">return to the
      developer index</a>.
  </p>


</body>
</html>
