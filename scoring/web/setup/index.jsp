<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%
  fll.web.setup.SetupIndex.populateContext(pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>FLL (Database setup)</title>
</head>

<body>
 <h1>FLL (Database setup)</h1>

 <p>${message}</p>
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <c:set var="redirect_url" scope="session">
  <c:url value="/" />
 </c:set>

 <form id='setup' action='CreateDB' method='post'
  enctype='multipart/form-data'>

  <p>On this page you can setup the database used by the scoring
   software.</p>

  <p>You can select a description that shipped with the software</p>
  <select id='description' name='description'>
   <c:forEach items="${descriptions}" var="description">
    <option value="${description.URL }">
     ${description.title }
     <c:if test='${not empty description.revision }'>
     (${description.revision })
     </c:if>
    </option>
   </c:forEach>
  </select> <input type='submit' name='chooseDescription'
   value='Choose Description'
   onclick='return confirm("This will erase ALL scores in the database fll (if it already exists), are you sure?")' />



  <p>Or provide your own challenge description file</p>

  XML description document <input type='file' size='32'
   name='xmldocument'><br /> <input type='checkbox'
   name='force_rebuild' value='1' /> Rebuild the whole database,
  including team data?<br /> <input type='submit'
   name='reinitializeDatabase' value='Upload Description'
   onclick='return confirm("This will erase ALL scores in the database fll (if it already exists), are you sure?")' />



  <p>This will allow one to initialize the database based upon a
   previous database dump that was created using the download database
   link on the administration page.</p>

  <input type='file' size='32' name='dbdump'> <input
   type='submit' name='createdb' value='Upload Dump' />

 </form>



</body>
</html>
