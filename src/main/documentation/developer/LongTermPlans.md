Isolate SQL
===========
Try and keep the SQL code isolated to inside methods that only interact with the database and do other processing in separate methods. Most of the SQL is inside `fll.db.Queries`, however I've been slowly moving it out into separate classes to keep related SQL together.

Status
-------
See `fll.db.Queries` and `fll.db.GlobalParameters` and `fll.db.TournamentParameters`.


Session Variables
=================
Over time I've noticed that we have a number of session variables. Some of these really should hang around all of the time, but some of them should go away once a workflow is finished. 

Status
-------
See `fll.web.playoff.PlayoffSessionData` for an example of this. The idea is that all variables for a workflow/task can be stored in a separate object that has one entry into the session. This makes it easy to remove this when the task is done. It also has the benefit of giving us generics type safety for the variables.


Java Scriptlets
===============
There are a number of JSPs that have Java code in them. The goal here is to move all of this custom logic out to servlets that put the data into a session variable and then access the data using JSTL syntax and JSTL tags.

SQL in JSP
==========
There are a number of JSPs that have SQL in them. 
I'd like to remove all of the SQL from the JSPs and isolate it in Java code.
