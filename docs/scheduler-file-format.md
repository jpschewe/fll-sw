# schedule properties file format

This explanation is from version 10.0. Some of the fields have been removed and some added since then. There is now a user interface for editing the schedule description files. It's best to use that instead. 

With the user interface you mostly don't need to worry about these, but if you end up looking at the resulting properties file here is what the variables mean.

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
