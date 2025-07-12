<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="true" />

<html>
<head>
<title>Schedule format</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />


<script type="text/javascript" src="index.js"></script>
</head>

<body>

    <h1>Schedule File Format</h1>

    <p>Before uploading a schedule you need to create the tournament
        and make sure it is selected.</p>

    <p>The file to be loaded may be an Excel file or a CSV file.</p>

    <p>There must be a row in the file that contains names of
        columns. It does not need to be the first row in the input file.
        During upload you will choose this row.</p>

    <p>Each row contains all the information for a single team.
        During upload teams are added to the database in the currently
        selected tournament.</p>

    <p>For string data, any string is valid. However it is best to
        avoid characters not on a standard US keyboard. Emojis and
        special Unicode characters are not well supported.</p>

    <h2>Time formats</h2>
    <p>Some columns are expecting times. The preferred time format
        is hours:minutes AM/PM. For the technical folks the format is
        "h:mm a". Any dates will be ignored and only the time portion of
        the cell is used.</p>

    <p>Other time formats are also supported however all output
        files will use the above format.</p>

    <h2>Columns</h2>
    <p>There are several required and optional columns for a
        schedule.</p>


    <h3>Team Number</h3>
    <p>A column must exist that has a unique number for each team.
        This is used internally as the identifier for the team. Use
        actual team numbers if available. If teams do not have actual
        team numbers, 1, 2, 3, etc. can be assigned.</p>


    <h3>Team Name</h3>
    <p>This is an optional field, although it's best that each team
        have a name. Very long team names get truncated in various
        places, so having really long team names isn't always a good
        experience.</p>

    <h3>Organization</h3>
    <p>Each team can have an optional organization. This is usually
        the school or community group that the team belongs to. This is
        helpful for parents that don't know the team name or number, but
        recognize the school or group name.</p>

    <h3>Award Group</h3>
    <p>
        Each team is assigned to an optional award group. There are
        usually 1 or 2 award groups at a tournament. See the <a
            href='<c:url value="/documentation/terminology.html" />'>Terms
            and Definitions page</a> for more information on what an Award
        Group is.
    </p>

    <h3>Judging Group</h3>
    <p>
        Each team is assigned an optional judging group. For most
        tournaments this is the same as the award group and the same
        column can be used for both pieces of information. See the <a
            href='<c:url value="/documentation/terminology.html" />'>Terms
            and Definitions page</a> for more information on what a Judging
        Group is.
    </p>

    <h3>Wave</h3>
    <p>This is an optional column. Waves are used when some of the
        teams are only at the tournament location for part of the day.
        If all teams are at the tournament all day, this column can be
        ignored.</p>

    <h3>Performance rounds</h3>
    <p>When uploading the schedule you are asked for the number of
        scheduled performance rounds. This includes practice rounds,
        regular match play and any additional performance rounds that
        are on the schedule.</p>

    <p>Each performance round must have 2 columns. The first column
        specifies the table and the second specifies the team's round
        time. See above for the format of the time column.</p>

    <p>The value in the table column must be made up of a string
        name without spaces, a space, then a number. The two sides of
        the table need to have the same name and different numbers. For
        example, one side of the table is "Red 1" and the other side is
        "Red 2".</p>

    <h3>Subjective categories</h3>
    <p>Each subjective category specified in the challenge
        description must be mapped to a column that specifies the time
        that this category is being judged. Optionally a second column
        can be specified for each subjective category. This is used when
        a subjective category is judged at 2 different times by
        different judges for the same teams.</p>
</body>
</html>