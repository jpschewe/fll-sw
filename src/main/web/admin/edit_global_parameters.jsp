<%@page import="fll.web.admin.GatherParameterInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%@ page import="fll.db.TournamentParameters"%>

<%
GatherParameterInformation.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Parameters</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<style>
.content table {
    border-collapse: collapse;
}

.content table, .content th, .content td {
    border: 1px solid black;
}

.content td, .content td {
    text-align: center;
}
</style>

<!-- functions to displaying and hiding help -->
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

</head>

<body>

    <div class='content'>

        <h1>Edit Global Parameters</h1>

        <p>These parameters are specified globally and apply to all
            tournaments in the database.</p>

        <div class='status-message'>${message}</div>
        <%-- clear out the message, so that we don't see it again --%>
        <c:remove var="message" />


        <p>This page is for advanced users only. Be careful changing
            parameters here.</p>

        <form name='edit_global_parameters' id='edit_global_parameters'
            action='StoreGlobalParameters' method='POST'>

            <table>

                <tr>
                    <th>Parameter</th>
                    <th>Value</th>
                </tr>

                <tr>
                    <th>
                        Award Group Flip Rate (seconds) <a
                            href='javascript:display("DivisionFlipRateHelp")'>[help]</a>
                        <div id='DivisionFlipRateHelp' class='help'
                            style='display: none'>
                            The number of seconds between when the
                            scoreboard's "Top Scores" panel switches
                            which division is shown. Default 30 seconds.
                            <a
                                href='javascript:hide("DivisionFlipRateHelp")'>[hide]</a>
                        </div>
                    </th>
                    <td>
                        <input type='number'
                            value="${gDivisionFlipRate}"
                            id='gDivisionFlipRate'
                            name='gDivisionFlipRate' min='1' required />
                    </td>
                </tr>

                <tr>
                    <th>
                        All teams scroll rate control. <a
                            href='javascript:display("gAllTeamsScrollRateHelp")'>[help]</a>
                        <div id='gAllTeamsScrollRateHelp' class='help'
                            style='display: none'>The value is
                            number of seconds between calls to scroll 2
                            pixels.</div>
                    </th>

                    <td>
                        <input type='number'
                            value="${gAllTeamsScrollRate }"
                            id='gAllTeamsScrollRate'
                            name='gAllTeamsScrollRate' min='0.0000001'
                            step='any' required />
                    </td>
                </tr>

                <tr>
                    <th>
                        Head to head scroll rate control. <a
                            href='javascript:display("gHeadToHeadSecondsPerRowHelp")'>[help]</a>
                        <div id='gHeadToHeadSecondsPerRowHelp'
                            class='help' style='display: none'>The
                            value is number of seconds between calls to
                            scroll 2 pixels. The remote control brackets
                            page needs to be refreshed for this
                            parameter to take effect.</div>
                    </th>

                    <td>
                        <input type='number'
                            value="${gHeadToHeadSecondsPerRow }"
                            id='gHeadToHeadSecondsPerRow'
                            name='gHeadToHeadSecondsPerRow'
                            min='0.0000001' step='any' required />
                    </td>
                </tr>

            </table>


            <input type='submit' value='Save Changes' id='submit_data'
                name='submit_data' />
        </form>

    </div>

</body>
</html>
