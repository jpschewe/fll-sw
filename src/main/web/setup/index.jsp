<%@ include file="/WEB-INF/jspf/init.jspf"%>
<fll-sw:required-roles roles="ADMIN" allowSetup="true" />

<%
fll.web.setup.SetupIndex.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
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

    <div class='status-message'>${message}</div>

    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <c:set var="redirect_url" scope="session">
        <c:url value="/" />
    </c:set>

    <form id='setup' action='CreateDB' method='post'
        enctype='multipart/form-data'>

        <p>
            On this page you can setup the database used by the scoring
            software. You may find the <a
                href='<c:url value="/documentation/index.html"/>'
                target="_documentation">documentation</a> helpful (opens
            new window).
        </p>

        <p>You need to choose how you will initialize the software.
            It is most likely that you will want to choose the first or
            second option below.</p>

        <hr />

        <p>
            Initialize the database based upon a previously saved
            database. If you want to load a backup database you can
            access them <a
                href="<c:url value='/admin/database-backups.jsp' />"
                target="_blank">here</a>.
        </p>

        <input type='file' size='32' name='dbdump'>
        <input type='submit' name='createdb' value='Upload Database'
            onclick='return confirmOverwrite()' />

        <hr />

        <p>
            Select a description that shipped with the software. Use
            this option if a database has not been provided. If you want
            to see the details of the challenge descriptions, you can
            view them at <a
                href="<c:url value='/challenge-descriptions.jsp'/>"
                target="_blank">this page</a>.
        </p>
        <select id='description' name='description'>
            <c:forEach items="${descriptions}" var="description">
                <option value="${description.URL }">
                    ${description.display}</option>
            </c:forEach>
        </select>
        <input type='submit' name='chooseDescription'
            value='Choose Description'
            onclick='return confirmOverwrite()' />


        <hr />

        <p>
            Provide your own custom challenge description file (advanced
            users only). If you want to modify an existing challenge
            description, you can view and download them at <a
                href="<c:url value='/challenge-descriptions.jsp'/>"
                target="_blank">this page</a>.
        </p>
        <input type='file' size='32' name='xmldocument'>
        <input type='submit' name='reinitializeDatabase'
            value='Upload Description'
            onclick='return confirmOverwrite()' />



    </form>



</body>
</html>
