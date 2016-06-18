<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.setup.SetupIndex.populateContext(application, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript">
  // if this action will overwrite existing data, confirm this change with the user
  function confirmOverwrite() {
    <c:choose>
    <c:when test="${dbinitialized}">
    retval = confirm("This will erase ALL scores and team information in the database, are you sure?");
    </c:when>
    <c:otherwise>
    retval = true;
    </c:otherwise>
    </c:choose>

    return retval;
  }
</script>

<title>FLL (Database setup)</title>
</head>

<body>
  <h1>FLL (Database setup)</h1>

  <p>
  <div class='status-message'>${message}</div>
  </p>
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <c:set
    var="redirect_url"
    scope="session">
    <c:url value="/" />
  </c:set>

  <form
    id='setup'
    action='CreateDB'
    method='post'
    enctype='multipart/form-data'>

    <p>
      On this page you can setup the database used by the scoring
      software. You may find the <a
        href='<c:url value="/documentation/index.html"/>'
        target="_documentation">documentation</a> helpful (opens new
      window).
    </p>

    <p>You need to choose how you will initialize the software. It
      is most likely that you will want to choose the first or second
      option below.</p>

    <hr />
    <p>You can select a description that shipped with the software</p>
    <select
      id='description'
      name='description'>
      <c:forEach
        items="${descriptions}"
        var="description">
        <option value="${description.URL }">
          ${description.title }
          <c:if test='${not empty description.revision }'>
     (${description.revision })
     </c:if>
        </option>
      </c:forEach>
    </select> <input
      type='submit'
      name='chooseDescription'
      value='Choose Description'
      onclick='return confirmOverwrite()' />


    <hr />
    <p>Or initialize the database based upon a previously saved
      database that was created using the download database link on the
      administration page.</p>

    <input
      type='file'
      size='32'
      name='dbdump'> <input
      type='submit'
      name='createdb'
      value='Upload Saved Database'
      onclick='return confirmOverwrite()' />

    <hr />
    <p>Or provide your own custom challenge description file</p>
    <input
      type='file'
      size='32'
      name='xmldocument'> <input
      type='submit'
      name='reinitializeDatabase'
      value='Upload Description'
      onclick='return confirmOverwrite()' />



  </form>



</body>
</html>
