#!/usr/bin/perl -w

# fix case of table names that get whacked by windows

use strict;

foreach my $file qw(AllTeams FilteredTeams TestTeams FinalScores Judges Performance SummarizedScores Teams TournamentParameters TournamentTeams Tournaments) {
  my $filelc = lc $file;
  foreach my $ext qw(MYD MYI frm) {
    my $old = $filelc . '.' . $ext;
    my $new = $file . '.' . $ext;
    if( -e $old ) {
      print "Renaming " . $old . " to " . $new . "\n";
      rename $old, $new;
    } 
  }
}
    
