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
  	<frame src='title.jsp' marginheight='0' marginwidth='0'scrolling=no>
	<frame src='<c:url value="allteams.jsp"><c:param name="scroll" value="true"/></c:url>' scrolling=no>
  </frameset>
  <frameset rows='350,*' border='1' framespacing='0'>
      <frame src='top10.jsp?vDivision=1' marginheight='0' marginwidth='0' noresize scrolling=no >
      <frame src='last8.jsp' marginheight='3' marginwidth='0' noresize scrolling=no >
  </frameset>
</frameset>

</HTML>
