% variables
array [ 1..TMax ] of var 0..1 : yMax;
var int: objective;

% objectiveYMaxSubjective
constraint
  forall(g in 1..NGroups) (
  forall (i in 1..group_counts[g]) (
    forall (n in 1..NSubjective) (
      forall (t in 1..TMax) (
        yMax[t] >= sy[g,i,n,t]
))));

% objectiveYMaxPerfS1
constraint
  forall(g in 1..NGroups) (
  forall(i in 1..group_counts[g]) (
    forall(b in 1..NTables) (
      forall(s in 1..2) (
        forall(t in 1..TMax) (
          yMax[t] >= py[g,i,b,s,t]
)))));

% objectiveYMax
constraint
forall(t in 1..TMax) (
  objective >= yMax[t] * t
);

solve minimize objective;
