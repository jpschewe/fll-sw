<%@ include file="/WEB-INF/jspf/init.jspf"%>
<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.ManageSlideshow.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<title>Manage slideshow</title>
</head>

<body>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <form id='sponsor_upload' action='ManageSlideshow' method='post'
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