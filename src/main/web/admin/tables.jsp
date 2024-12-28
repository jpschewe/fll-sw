<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.admin.Tables"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.Tables.populateContext(application, pageContext);
%>

<html>
<head>
<title>Table Labels</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript' src='tables.js'></script>
</head>

<body>

    <h1>Table Labels</h1>

    <div>Table labels must be unique. These labels must occur in
        pairs, where a label refers to a single side of a table. The
        labels are expected to be of the form 'Color number' where
        'number' is 1 or 2. When generating reports the tables are
        sorted by the sort index and then by side A.</div>


    <form action="Tables" method="post">
        <table id='tables_table'>
            <tr>
                <th>Side A</th>
                <th>Side B</th>
                <th>Sort Index</th>
                <th>Delete?</th>
            </tr>

            <c:forEach items="${tables}" var="table">
                <tr>
                    <td>
                        <input class='SideA' type='text'
                            name='SideA${table.id}'
                            value='${table.sideA}' />
                    </td>
                    <td>
                        <input class='SideB' type='text'
                            name='SideB${table.id}'
                            value='${table.sideB}' />
                    </td>
                    <td>
                        <input class='SortOrder' type='number'
                            name='sortOrder${table.id}'
                            value='${table.sortOrder}' />
                    </td>
                    <td>
                        <input type='checkbox' value='checked'
                            name='delete${table.id}' />
                    </td>
            </c:forEach>
        </table>

        <button type='button' id='add_row'>Add Row</button>

        <input type='submit' name='submit_data' id='finished'
            value='Finished' />

    </form>
</body>

</body>
</html>
