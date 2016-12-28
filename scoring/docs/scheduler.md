# Scheduler

The scheduler built into FLL-SW is implemented as a greedy solver. I
originally tried to solve the problem using an MILP, but had trouble
representing all of the constraints. Furthermore once I did represent most
of the constraints even an 8 team tournament was taking hours to solve. With
a greedy solver the solution isn't optimal, but we get a solution very
quickly. An 80 team tournament takes less than 5 minutes to come up with a solution.

To keep teams from competing against each other more than is necessary and
the change which tables each team runs on there is a table optimizer. This
just swaps around which table and side each team is on trying to minimize
the number of warnings.

## SchedulerUI

 1. [Install the server software](InstallServerSoftware.md) on your computer
 1. Open up the install directory
 1. Double click on fll-sw.exe (fll-sw.sh for Linux and Mac)
 1. Click on Scheduler
    * This application has 2 tabs, description and schedule.

### Description tab

The description tab is used to load and edit a file with schedule parameters. The edited file can be saved with the disk icon. The lightbulb icon runs the scheduler and optionally the table optimizer on the resulting schedule and then loads it in the schedule tab.

You can find some examples in our [previous datafiles](../../scheduling/blank-schedules). Look for the properties files. You can start with one of these and modify them.

### Schedule tab

The schedule tab is used to open a schedule file (CSV or spreadsheet) and
it's details and any problems with it.

Once you have loaded the schedule in via the open icon or from the description tab, you will see problems highlighted in red and yellow. The details of the problems will be shown at the bottom of the screen. If you haven't changed any times and you started with one of our blank schedules, you should not have any problems highlighted.

You can then write out the detailed schedules by clicking on the icon in the toolbar with an up arrow. This will write out PDF files with schedules suitable for use by an MC in the performance area and the judges in each subjective scoring area. The PDF files will be written to the same directory that the schedule was loaded from and will have the same base filename.

The scroll icon is used to display information about the general schedule. 

#### Load an existing schedule
The file being asked for is a spreadsheet of your schedule. At this point only Excel spreadsheets can be read and they must match the expected format. The easiest way to get this format is to take one of our [blank schedules](../../scheduling/datafiles) and just fill it in with your team information. The schedules are named `#-#.xls` to state how many teams are in each judging group. Unless you're running a large tournament with finalist judging you need to have all teams for a given division seen by the same judge. So the schedule named `11-6.xls` is a schedule that has 11 teams in one judging group and 6 teams in the other judging group. Do not change the "Team #" header loading into the `SchedulerUI`.


## Videos on running the scheduler

1. [Creating a 32 team schedule](https://vimeo.com/album/4327453/video/197293207) (external)
1. [Scheduler outputs](https://vimeo.com/197305835) (external)
1. [Adding breaks to a schedule](https://vimeo.com/album/4327453/video/197294593) (external)
1. [Advanced scheduling techniques](https://vimeo.com/album/4327453/video/197298274) (external)
1. [Populating a schedule](https://vimeo.com/172255789) (external) - The scheduler creates a template schedule without specific team information. This explains how to create a fully populated schedule from the template.


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

## Detailed file format

The [details of the file format](scheduler-file-format.md) are available for those that want to edit the description file by hand.


