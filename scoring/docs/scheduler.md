# Scheduler

The scheduler built into FLL-SW is implemented as a greedy solver. I
originally tried to solve the problem using an MILP, but had trouble
representing all of the constraints. Furthermore once I did represent most
of the constraints even an 8 team ournament was taking hours to solve. With
a greedy solver the solution isn't optimal, but we get a solution very
quickly. An 80 team tournament takes less than 5 minutes to come up with a solution.

To keep teams from competing against each other more than is necessary and
the change which tables each team runs on there is a table optimizer. This
just swaps around which table and side each team is on trying to minimize
the number of warnings.


## SchedulerUI

In the bin directory you will find an application `SchedulerUI`. This application has 2 tabs, description and schedule.

### Description tab

The description tab is used to load and edit a file with schedule parameters. The edited file can be saved with the disk icon. The lightbulb icon runs the scheduler and table optimizer on the resulting schedule and then loads it in the schedule tab.

You can find more examples in our [previous datafiles](../../scheduling/datafiles). You can start with one of these and modify them.

### Schedule tab

The schedule tab is used to open a schedule file (CSV or spreadsheet) and view 

Once you have loaded the schedule in via the open icon or from the description tab, you will see problems highlighted in red and yellow. The details of the problems will be shown at the bottom of the screen. If you haven't changed any times and you started with one of our blank schedules, you should not have any problems highlighted.

You can then write out the detailed schedules by clicking on the icon in the toolbar with an up arrow. This will write out PDF files with schedules suitable for use by an MC in the performance area and the judges in each subjective scoring area. The PDF files will be written to the same directory that the schedule was loaded from and will have the same base filename.

The scroll icon is used to display information about the general schedule. 

#### Load an existing schedule
The file being asked for is a spreadsheet of your schedule. At this point only Excel spreadsheets can be read and they must match the expected format. The easiest way to get this format is to take one of our [blank schedules](../../scheduling/datafiles) and just fill it in with your team information. The schedules are named `#-#.xls` to state how many teams are in each judging group. Unless you're running a large tournament with finalist judging you need to have all teams for a given division seen by the same judge. So the schedule named `11-6.xls` is a schedule that has 11 teams in one judging group and 6 teams in the other judging group. Do not change the "Team #" header loading into the `SchedulerUI`.


## Configurable Parameters

TInc - Time increment in minutes. Generally leave at 1. Must be an integer.

TMax\_hours, TMax\_minutes - The longest that the tournament can run.

NSubjective - The number of subjective stations to schedule.

subj\_minutes - Array stating the number of minutes that each team is at
each of the subjective judging stations. The number of elements in this
array *must* match NSubjective.

NRounds - The number of performance rounds each team needs to be scheduled for.

NTables - The number for performance tables that can  be scheduled.

NGroups - The number of judging groups. Each judging group can see a team in a subjective judging station at the same time.

group_counts - Array showing the number of teams in each judging group. The number of elements in this array *must* match NGroups.

alpha\_perf\_minutes - The amount of time between performance runs on a table.

alternate\_tables - 0 to use all tables at the same time, 1 to alternate
sets of tables. Alternating tables means that the number of tables and the
performance duration both must be even. Then half of the tables are
scheduled right away and the other half are scheduled at half the
performance duration. This is sometimes used at our state tournaments to
decrease the amount of time the tournament takes and keep the tables full
longer and give the refs more time to reset the tables.

perf\_attempt\_offset\_minutes - What interval to try scheduling the tables
on. Setting this to 5 will ensure that tables are only scheduled at 8:05,
8:10, 8:15, ...

subjective\_attempt\_offset\_minutes - What interval to try scheduling the
subjective judging on. Setting this to 5 will ensure that rooms are only
scheduled at 8:05, 8:10, 8:15, ...

start\_time - The start time of the first scheduled event. Use a 24 hour clock.

ct\_minutes - Change time in minutes. A team must have at least this much time from the end of one event to the start of their next event.

pct\_minutes - Performance change time in minutes. A team must have at least this much time from the end of one performance run to the start of their next.

### Breaks

Breaks can be forced into the schedule. This is useful to allow the judges
or refs to catch up or to schedule a lunch break. There are subjective
breaks and performance breaks. It is suggested to make lunch breaks at
least 45 minutes long. Less than that and the judges and refs are typically
rushed for lunch.

num\_subjective\_breaks - The number of subjective breaks

subjective\_break\_0\_start - The start time of the first subjective break

subjective\_break\_0\_duration - The minute of minutes in the first subjective break

performance\_break\_0\_start - The start time of the first performance break

performance\_break\_0\_duration - The minute of minutes in the first performance break

## Assumptions

These are the current assumptions that are made. These will limit how much
this scheduler can be used for non-FLL schedules.

  * Performance runs are done on a table with 2 teams competing at the same
    time and the table must always be full.
  * Subjective breaks are for all subjective stations at the same time.
  * Performance breaks are for all tables at the same time.

## Non-FLL tournaments

If you would like to use the scheduler for a non-FLL tournament chances are
that you want to set the number of performance rounds to 0 and just use the
subjective judging stations.
