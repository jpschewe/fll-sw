<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<HTML>
<head>
<script type='text/javascript'>
  var width = screen.width;
  var height = screen.height;
  if(screen.width < 1024) {
    //redirect to 800x600 scoreboard
    location.href='<c:url value="/scoreboard_800/main.jsp"/>';
  } else {
    window.resizeTo(width, height);
  }
</script>
</head>

<frameset cols="40%,*" border='1' framespacing='0'>
  <frameset rows='60,*' border='0' framespacing='0'>
    <frame src='<c:url value="title.jsp" />' marginheight='0' marginwidth='0'scrolling=no>
    <frame src='<c:url value="allteams.jsp"><c:param name="scroll" value="true"/></c:url>' scrolling=no>
  </frameset>
  <frameset rows='350,*' border='1' framespacing='0'>
    <frame src='<c:url value="top10.jsp"/>' marginheight='0' marginwidth='0' noresize scrolling=no >
    <frame src='<c:url value="last8.jsp" />' marginheight='3' marginwidth='0' noresize scrolling=no >
  </frameset>
</frameset>

</HTML>
