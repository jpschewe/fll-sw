<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.Utilities"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.SessionAttributes"%>

<%@ page import="java.io.File"%>

<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.Slideshow.populateContext(application, session, pageContext);
%>

<!-- ${slideShowLastImage} -->
<html>
<head>
<style>
FONT {
    color: #ffffff;
    font-family: "Arial"
}

body {
    margin: 0;
    padding: 0;
}

div.img {
    background-image: url('<c:url value="/${slideShowLastImage}"/>');
    background-position: center center;
    background-repeat: no-repeat;
    background-size: contain;
    width: 100vw;
    height: 100vh;
}
</style>
<script type='text/javascript'>
    window.setInterval("location.href='slideshow.jsp'", ${slideShowInterval});
  </script>
</head>

<body style="vertical-align: middle; text-align: center;">
    <div class="img"></div>

</body>

</html>
