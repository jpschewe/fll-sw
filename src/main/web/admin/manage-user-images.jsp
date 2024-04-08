<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.admin.ManageUserImages.populateContext(pageContext);
fll.web.Welcome.populateContext(pageContext);
%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet" type="text/css"
    href="<c:url value='/welcome.css'/>" />

<title>Manage Images</title>

</head>

<body>

    <p>All images are to be jpegs (ends with .jpg).</p>

    <form action='ManageUserImages' method='post'
        enctype='multipart/form-data'>

        <h1>Partner logo</h1>
        <p>Specify the partner logo that is displayed on the welcome
            page.</p>
        <table class="center partner_logo">
            <tr>
                <td>
                    <img src="<c:url value='/${partner_logo}'/>?${uuid}" />
                </td>
        </table>
        <div>
            Upload image:
            <input type='file' name='partner_logo' />
        </div>
        <div>
            <input type="checkbox" name='partner_logo_default'
                id='partner_logo_default' />
            <label for='partner_logo_default'>Use default image</label>
        </div>
        <hr />

        <h1>FLL logo</h1>
        <p>Specify the FLL logo that is displayed on the welcome
            page.</p>

        <table class="center fll_logo">
            <tr>
                <td>
                    <img src="<c:url value='/${fll_logo}'/>?${uuid}" />
                </td>
            </tr>
        </table>
        <div>
            Upload image:
            <input type='file' name='fll_logo' />
        </div>
        <div>
            <input type="checkbox" name='fll_logo_default'
                id='fll_logo_default' />
            <label for='fll_logo_default'>Use default image</label>
        </div>

        <hr />

        <h1>FLL Subjective Logo</h1>
        <p>Logo used at the top of the subjective rubrics.</p>
        <table class="center fll_logo">
            <tr>
                <td>
                    <img
                        src="<c:url value='/${fll_subjective_logo}'/>?${uuid}" />
                </td>
            </tr>
        </table>
        <div>
            Upload image:
            <input type='file' name='fll_subjective_logo' />
        </div>
        <div>
            <input type="checkbox" name='fll_subjective_logo_default'
                id='fll_subjective_logo_default' />
            <label for='fll_subjective_logo_default'>Use default
                image</label>
        </div>

        <h1>Challenge logo</h1>
        <p>Logo used at the top of the pit signs.</p>
        <table class="center">
            <tr>
                <td>
                    <img
                        src="<c:url value='/${challenge_logo}'/>?${uuid}" />
                </td>
            </tr>
        </table>
        <div>
            Upload image:
            <input type='file' name='challenge_logo' />
        </div>
        <div>
            <input type="checkbox" name='challenge_logo_default'
                id='challenge_default' />
            <label for='challenge_logo_default'>Use default
                image</label>
        </div>


        <!--  end of form -->
        <input type='submit' value='Submit Changes' />
    </form>

</body>
</html>
