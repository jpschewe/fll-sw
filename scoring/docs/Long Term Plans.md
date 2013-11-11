JNDI Datasource
===============

Currently the database is created inside the `fll-sw` web application. My understanding is that database connections should be created as JNDI resources. This means that the database probably needs to live outside the `fll-sw` web application's filesystem. 

Status
------
An attempt has been made at this in the branch `feature.jndi-datasource`, but the integration tests have been failing with errors about times outs and locking.

In Feburary of 2013 I changed the locking model on the database to `MVCC`, which may help with this.

ticket:151 is open for this task.


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


SourceForge Migration
=====================
Migrate to the new Allura software that is the current SourceForge front end. This is supported by SourceForge and the hosted apps that I'm currently using to get Trac is going away as a supported option. Allura contains a ticket tracker and wiki as well as git integration, so this should be better all around. 

Status
-------
The change over will be in 2 stages. The first is to tell SourceForge to migrate the project to the Allura software. This will likely change the URLs to our Trac page and to our git repository. I plan on doing this towards the end of February 2013 once the current season is done. At this point I will also move the wiki over. I will remove everyone's write privileges to the Trac wiki at this point. 
As of 2/18/2013 this stage has been completed.

The second step is to migrate all of our ticket information out of Trac and into Allura. The tickets will be mostly migrated using a bulk import into Allura with some manual cleanup after. I will disable writes to the Trac ticket system when this is started.
