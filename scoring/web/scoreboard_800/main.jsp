<%@ page errorPage="../errorHandler.jsp" %>
  
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<HTML>
<head>
<script language=javascript>
	window.resizeTo(1024,768);
</script>
</head>

<frameset cols="40%,*" border='1' framespacing='0'>
  <frameset rows='60,*' border='0' framespacing='0'>
  	<frame src='<c:url value="/scoreboard/title.jsp" />' marginheight='0' marginwidth='0'scrolling=no>
	<frame src='<c:url value="/scoreboard/allteams.jsp"><c:param name="scroll" value="true"/></c:url>' scrolling=no>
  </frameset>
  <frameset rows='350,*' border='1' framespacing='0'>
      <frame src='<c:url value="top10.jsp"><c:param name="vDivision" value="1" /></c:url>' marginheight='0' marginwidth='0' noresize scrolling=no >
      <frame src='<c:url value="last8.jsp" />' marginheight='3' marginwidth='0' noresize scrolling=no >
  </frameset>
</frameset>

</HTML>
