% objective
constraint
sum(t in 1..TMax) (
  sum(g in 1..NGroups) (
    sum(i in 1..group_counts[g]) (
      sum(n in 1..NSubjective) (
        sy[g,i,n,t] * t
      ) +
      sum(b in 1..NTables) (
        (py[g,i,b,1,t] + py[g,i,b,2,t]) * t
      )
))) = objective;


solve minimize objective;
