include "globals.mzn";

% parameters

% run with 'minizinc -G linear -b mip'

% FIXME need judging groups

%int: nTeams = 2;
%int: NSubjective = 1;
%int: NRounds = 1;
%int: NTables = 1;
% 1.1s

%int: nTeams = 2;
%int: NSubjective = 3;
%int: NRounds = 3;
%int: NTables = 1;
% 1m24.133s

int: nTeams = 4;
int: NSubjective = 3;
int: NRounds = 3;
int: NTables = 1;


int: TInc = 5;
int: TMax_hours = 8;
int: NGroups = 1;
int: alpha_subj_minutes = 20;
int: alpha_perf_minutes = 5;
int: ct_minutes = 15;
int: pct_minutes = 45;

int: M = 10000;

% computed parameters
int: TMax = TMax_hours * 60 div TInc;
int: alpha_subj = alpha_subj_minutes div TInc;
int: alpha_perf = alpha_perf_minutes div TInc;
int: ct = ct_minutes div TInc;
int: pct = pct_minutes div TInc;


% variables
array [ 1..nTeams, 1..NSubjective, 1..TMax ] of var 0..1 : sz;
array [ 1..nTeams, 1..NSubjective, 1..TMax ] of var 0..1 : sy;
array [ 1..nTeams, 1..NTables, 1..2, 1..TMax ] of var 0..1 : pz;
array [ 1..nTeams, 1..NTables, 1..2, 1..TMax ] of var 0..1 : py;
var int: objective;

% stationBusySubjective
constraint
  forall (i in 1..nTeams) (
    forall (n in 1..NSubjective) (
      forall (t in alpha_subj..TMax) (
        sum (u in 1..alpha_subj) (sz[i,n,t-u+1]) <= sy[i,n,t]
        )
    )
  )
;

% stationBusyPerformance
constraint
  forall(i in 1..nTeams) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in alpha_perf..TMax) (
	  sum(u in 1..alpha_perf) (pz[i,b,s,t-u+1]) <= py[i,b,s,t]
))));

% stationStartSubjective
constraint
  forall (i in 1..nTeams) (
    forall (n in 1..NSubjective) (
      forall (t in 2..TMax) (
        sy[i,n,t] - sy[i,n,t-1] <= sz[i,n,t]
      )
    )
  )
;

% stationStartPerformance
constraint
  forall (i in 1..nTeams) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 2..TMax) (
	  py[i,b,s,t] - py[i,b,s,t-1] <= pz[i,b,s,t]
))));

% noOverlapSubjective
constraint
  forall (t in 1..TMax) (
    forall (n in 1..NSubjective) (
      sum(i in 1..nTeams) (sy[i,n,t]) <= 1
    )
  )
;

% noOverlapPerformance
constraint
  forall (t in 1..TMax) (
    forall (b in 1..NTables) (
      forall (s in 1..2) (
        sum(i in 1..nTeams) (py[i,b,s,t]) <= 1
)));

% teamSubjective
constraint
  forall (i in 1..nTeams) (
    forall (n in 1..NSubjective) (
      sum (t in 1..TMax) (sz[i,n,t]) = 1
    )
  )
;

% teamPerformance
constraint
  forall (i in 1..nTeams) (
    sum (b in 1..NTables) (
      sum (t in 1..TMax) (pz[i,b,1,t] + pz[i,b,2,t])
    ) = 1
);

% subjectiveEOS
constraint
  sum (i in 1..nTeams) (
    sum (n in 1..NSubjective) (
      sum (t in TMax-alpha_subj+1..TMax) (
        sz[i,n,t]
      )
    )
  )
  = 0;

% performanceEOS
constraint
  sum (i in 1..nTeams) (
    sum (b in 1..NTables) (
      sum (t in TMax-alpha_perf+1..TMax) (
       pz[i,b,1,t] + pz[i,b,2,t]
))) = 0;

% subjOrder
%DEBUG constraint
%DEBUG   forall(i in 1..nTeams) (
%DEBUG     forall(t in 1..TMax) (
%DEBUG       sum(j in 1..nTeams) (
%DEBUG         sum(p in 1..t) (
%DEBUG 	if j != i then
%DEBUG 	  sz[j,1,p]
%DEBUG 	else
%DEBUG 	  0
%DEBUG 	endif
%DEBUG 	)
%DEBUG       ) <= ( 0 + ( ( 1  - sz[i,1,t] ) * M ) )
%DEBUG ));

% TODO can these be inline in the foralls?
int: subj_ulimit = ct-1;
int: subj_tlimit = TMax-subj_ulimit;

% subjSubjChangetime
constraint
  forall(i in 1..nTeams) (
    forall(n in 1..NSubjective) (
      forall(t in 1..subj_tlimit) (
        forall(d in 1..NSubjective) (
	  forall(u in 0..subj_ulimit) (
	    if(d != n) then
	      sy[i,n,t] + sy[i,d,t+u] <= 1
	    else
	      true
	    endif
)))));

% subjPerfChangetime
constraint
  forall(i in 1..nTeams) (
    forall(n in 1..NSubjective) (
      forall(t in 1..subj_tlimit) (
        forall(u in 0..subj_ulimit) (
          forall(b in 1..NTables) (
	    forall(s in 1..2) (
	      sy[i,n,t] + py[i,b,s,t+u] <= 1
))))));

% TODO can these be inline in the foralls?
int: perf_ulimit = ct-1;
int: perf_tlimit = TMax-perf_ulimit;

% perfPerfChangetime
constraint
  forall(i in 1..nTeams) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 1..perf_tlimit) (
          forall(u in 0..perf_ulimit) (
  	    forall(d in 1..NTables) (
  	      forall(e in 1..2) (
	        if(b != d /\ s != e) then
		  py[i,b,s,t] + py[i,d,e,t+u] <= 1
		else
		  true
		endif
)))))));

% perfSubjChangetime
constraint
  forall(i in 1..nTeams) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 1..perf_tlimit) (
	  forall(u in 0..perf_ulimit) (
	    forall(n in 1..NSubjective) (
	      py[i,b,s,t] + sy[i, n, t+u] <= 1
))))));

% TODO can these be inline in the foralls?
int: perf_pct_ulimit = pct-1;
int: perf_pct_tlimit = TMax-perf_pct_ulimit;

% performanceChangetime
constraint
  forall(i in 1..nTeams) (
    forall(t in 1..perf_pct_tlimit) (
      forall(u in 1..perf_pct_ulimit) (
        sum(b in 1..NTables) (
	  py[i,b,1,t] + py[i,b,2,t] + py[i,b,1,t+u] + py[i,b,2,t+u]
	) <= 1
)));

% perfUseBothSides
constraint
  forall(b in 1..NTables) (
    forall(t in 1..TMax) (
      sum(i in 1..nTeams) (py[i,b,1,t]) = sum(i in 1..nTeams) (py[i,b,2,t])
));

% teamJudging
constraint
  forall(i in 1..nTeams) (
    sum(t in 1..TMax) (
      sum(n in 1..NSubjective) (
        sz[i,n,t] +
	sum(b in 1..NTables) (
	  pz[i,b,1,t] + pz[i,b,2,t]
	))) = NSubjective + NRounds
);


% performanceStart
int: perfStartWait = 60 div TInc;
constraint
  sum(i in 1..nTeams) (
    sum(b in 1..NTables) (
      sum(t in 1..perfStartWait) (
        pz[i,b,1,t] + pz[i,b,2,t]
))) = 0;

% objective
constraint
  sum(t in 1..TMax) (
    sum(i in 1..nTeams) (
      sum(n in 1..NSubjective) (
        sy[i,n,t] * t
      ) +
      sum(b in 1..NTables) (
        (py[i,b,1,t] + py[i,b,2,t]) * t
      )
)) = objective;


solve minimize objective;
    
output
[ if fix(sz[i,n,t]) == 1 then
    "sz[" ++ show(i) ++ "][" ++ show(n) ++ "][" ++ show(t) ++ "] = " ++
      show(sz[i, n, t]) ++ "\n"
    else
      ""
    endif
       | i in 1..nTeams, n in 1..NSubjective, t in 1..TMax
       ]
++ [ if fix(pz[i,b,s,t]) == 1 then
    "pz[" ++ show(i) ++ "][" ++ show(b) ++ "][" ++ show(s) ++ "][" ++ show(t) ++ "] = " ++
      show(pz[i, b, s, t]) ++ "\n"
    else
      ""
    endif
       | i in 1..nTeams, b in 1..NTables, s in 1..2, t in 1..TMax
       ] 
%correct ++ [ "perf_pct_tlimit " ++ show(perf_pct_tlimit) ] ++ ["\n"]
%correct ++ [ "perf_pct_ulimit " ++ show(perf_pct_ulimit) ] ++ ["\n"]
%correct ++ [ show(alpha_perf) ] ++ ["\n"]
%correct ++ [ show(pct) ] ++ ["\n"]
++ ["\n"]
;
