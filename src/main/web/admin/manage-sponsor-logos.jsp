<%@ include file="/WEB-INF/jspf/init.jspf"%>
<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.ManageSponsorLogos.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<title>Manage sponsor logos</title>
</head>

<body>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <p>To display well, a logo graphic should be approximately
        150-200 pixels wide and 45-60 pixels high.</p>

    <form id='sponsor_upload' action='ManageSponsorLogos' method='post'
        enctype='multipart/form-data'>

        <div>
            Upload images:
            <input type='file' multiple name='images' />
            <input type='submit' value='Submit Changes' />
        </div>

        <hr />
        <div>
            <div>Choose images to delete</div>
            <c:forEach var="file" items="${files}">
                <div>
                    <input type='checkbox'
                        name="${DELETE_PREFIX}${file}" />

                    <img src="<c:url value='/${file}'/>" />
                    <hr />
                </div>
            </c:forEach>

            <input type='submit' value='Submit Changes' />
        </div>

    </form>
</body>

</html>