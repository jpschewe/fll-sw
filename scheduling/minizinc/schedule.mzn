% Copyright (c) 2011 HighTechKids.  All rights reserved
% HighTechKids is on the web at: http://www.hightechkids.org
% This code is released under GPL; see LICENSE.txt for details.

include "globals.mzn";

% run with 'minizinc -G linear -b mip' - doesn't work because of 5d arrays
% mzn2fzn --no-output-ozn --globals-dir linear schedule.mzn <param file>.dzn && flatzinc -b mip -o schedule.result schedule.fzn

% parameters
int: TInc;
int: TMax_hours;
int: TMax_minutes;

% subjective setup
int: NSubjective;

% performance setup
int: NRounds;
int: NTables;

% judging groups
int: NGroups;

int: alpha_perf_minutes;

int: ct_minutes;
int: pct_minutes;


int: M = 10000;

% computed parameters

array [1..NSubjective] of int : subj_minutes;
array [1..NGroups] of int : group_counts;

array [1..NSubjective] of int : alpha_subj = [ x div TInc | x in subj_minutes ];

int: NTeams = sum(group_counts);
constraint assert(NTeams mod 2 == 0, "Number of teams must be even");

constraint assert( ((TMax_hours * 60) + TMax_minutes) mod TInc == 0, "TMax must be evenly divisible by TInc");

int: TMax = ( (TMax_hours * 60) + TMax_minutes) div TInc;
int: alpha_perf = alpha_perf_minutes div TInc;
int: ct = ct_minutes div TInc;
int: pct = pct_minutes div TInc;


int: MaxTeamsInGroup = max(group_counts);


% variables
array [1..NGroups, 1..MaxTeamsInGroup, 1..NSubjective, 1..TMax ] of var
0..1 : sz ::is_output;
array [1..NGroups, 1..MaxTeamsInGroup, 1..NSubjective, 1..TMax ] of var
0..1 : sy ::is_output;
array [1..NGroups, 1..MaxTeamsInGroup, 1..NTables, 1..2, 1..TMax ] of var
0..1 : pz ::is_output;
array [1..NGroups, 1..MaxTeamsInGroup, 1..NTables, 1..2, 1..TMax ] of var
0..1 : py ::is_output;

% zero out extra variables
constraint
sum(t in 1..TMax) (
  sum(g in 1..NGroups) (
    sum(i in 1..MaxTeamsInGroup) (
      if(i > group_counts[g]) then
        sum(n in 1..NSubjective) ( sz[g,i,n,t] + sy[g,i,n,t] )
        + sum(b in 1..NTables) (
          pz[g,i,b,1,t] + pz[g,i,b,2,t]
          + py[g,i,b,1,t] + py[g,i,b,2,t]
        )
      else
        0
      endif
))) = 0;

% stationBusySubjective
constraint
  forall(g in 1..NGroups) (
  forall (i in 1..group_counts[g]) (
    forall (n in 1..NSubjective) (
      forall (t in 1..TMax) (
        sum (u in 1..alpha_subj[n]) (
          if(t-u+1 >= 1) then
            sz[g,i,n,t-u+1]
          else
            0
          endif
        ) <= sy[g,i,n,t]
))));

% stationBusyPerformance
constraint
  forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in alpha_perf..TMax) (
	  sum(u in 1..alpha_perf) (
            if(t-u+1 >= 1) then
              pz[g,i,b,s,t-u+1]
            else
              0
            endif
           ) <= py[g,i,b,s,t]
)))));

% stationStartSubjective
constraint
  forall(g in 1..NGroups) (
  forall (i in 1..group_counts[g]) (
    forall (n in 1..NSubjective) (
      forall (t in 2..TMax) (
        sy[g,i,n,t] - sy[g,i,n,t-1] <= sz[g,i,n,t]
))));

% stationStartPerformance
constraint
  forall(g in 1..NGroups) (
  forall (i in 1..group_counts[g]) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 2..TMax) (
	  py[g,i,b,s,t] - py[g,i,b,s,t-1] <= pz[g,i,b,s,t]
)))));

% noOverlapSubjective
constraint
  forall(g in 1..NGroups) (
  forall (t in 1..TMax) (
    forall (n in 1..NSubjective) (
      sum(i in 1..group_counts[g]) (sy[g,i,n,t]) <= 1
)));

% noOverlapPerformance
constraint
  forall (t in 1..TMax) (
    forall (b in 1..NTables) (
      forall (s in 1..2) (
        sum(g in 1..NGroups) (sum(i in 1..group_counts[g]) (py[g,i,b,s,t])) <= 1
)));

% noOverlapTeam
constraint
forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(t in 1..TMax) (
      sum(n in 1..NSubjective) ( sz[g,i,n,t] )
      + sum(b in 1..NTables) ( pz[g,i,b,1,t] + pz[g,i,b,2,t] )
      <= 1
)));

% teamSubjective
constraint
forall(g in 1..NGroups) (
  forall (i in 1..group_counts[g]) (
    forall (n in 1..NSubjective) (
      sum (t in 1..TMax) (sz[g,i,n,t]) = 1
)));

% teamPerformance
constraint
forall(g in 1..NGroups) (
  forall (i in 1..group_counts[g]) (
    sum (b in 1..NTables) (
      sum (t in 1..TMax) (pz[g,i,b,1,t] + pz[g,i,b,2,t])
    ) = NRounds
));

% subjectiveEOS
constraint
sum(g in 1..NGroups) (
  sum (i in 1..group_counts[g]) (
    sum (n in 1..NSubjective) (
      sy[g,i,n,TMax]
))) = 0;

% performanceEOS
constraint
sum(g in 1..NGroups) (
  sum (i in 1..group_counts[g]) (
    sum (b in 1..NTables) (
      py[g,i,b,1,TMax] + py[g,i,b,2,TMax]
))) = 0;

% subjOrder
constraint
forall(g in 1..NGroups) (  
  forall(i in 1..group_counts[g]) (
    forall(t in 1..TMax) (
      sum(j in i..group_counts[g]) (
        if(j > i) then
          sum(p in 1..TMax) (
            if(p <= t + alpha_subj[1] - 1) then
              sz[g,j,1,p]
            else
              0
            endif
          )
        else
          0
        endif
	)
    <= ( 0 + ( ( 1  - sz[g,i,1,t] ) * M ) )
)));

% subjSubjChangetime
constraint
forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(n in 1..NSubjective) (
      forall(t in 1..TMax) (
        forall(d in 1..NSubjective) (
	  forall(u in 1..TMax) (
	    if(u <= alpha_subj[n] + ct - 1 /\ t + u <= TMax) then
	      sz[g,i,n,t] + sz[g,i,d,t+u] <= 1
	    else
	      true
	    endif
))))));

% subjPerfChangetime
constraint
forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(n in 1..NSubjective) (
      forall(t in 1..TMax) (
        forall(u in 1..TMax) (
          forall(b in 1..NTables) (
	    forall(s in 1..2) (
	      if(u <= alpha_subj[n] + ct - 1 /\ t + u <= TMax) then
 	        sz[g,i,n,t] + pz[g,i,b,s,t+u] <= 1
              else
                true
              endif
)))))));

% perfPerfChangetime
constraint
forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 1..TMax) (
          forall(u in 1..TMax) (
  	    forall(d in 1..NTables) (
  	      forall(e in 1..2) (
	        if(u <= alpha_perf + ct - 1 /\ t + u <= TMax) then
		  pz[g,i,b,s,t] + pz[g,i,d,e,t+u] <= 1
		else
		  true
		endif
))))))));

% perfSubjChangetime
constraint
forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 1..TMax) (
	  forall(u in 1..TMax) (
	    forall(n in 1..NSubjective) (
	      if(u <= alpha_perf + ct - 1 /\ t + u <= TMax) then
	        pz[g,i,b,s,t] + sz[g,i, n, t+u] <= 1
              else
                true
              endif
)))))));

% performanceChangetime
constraint
forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(t in 1..TMax) (
      forall(u in 1..TMax) (
        if(u <= alpha_perf + pct - 1 /\ t + u <= TMax) then
          sum(b in 1..NTables) (
	    py[g,i,b,1,t] + py[g,i,b,2,t] + py[g,i,b,1,t+u] + py[g,i,b,2,t+u]
	  ) <= 1
        else
          true
        endif
))));

% perfUseBothSides
constraint
  forall(b in 1..NTables) (
    forall(t in 1..TMax) (
      sum(g in 1..NGroups) (sum(i in 1..group_counts[g]) (pz[g,i,b,1,t]))
      = sum(g in 1..NGroups) (sum(i in 1..group_counts[g]) (pz[g,i,b,2,t]))
));

% performanceStart
int: perfStartWait = 60 div TInc;
constraint
sum(g in 1..NGroups) (
  sum(i in 1..group_counts[g]) (
    sum(b in 1..NTables) (
      sum(t in 1..perfStartWait) (
        pz[g,i,b,1,t] + pz[g,i,b,2,t]
)))) = 0;

