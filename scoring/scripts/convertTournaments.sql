-- convert Regions to Tournaments and add support for heirarchial
-- tournaments (707874 & 707876)

ALTER TABLE Regions RENAME Tournaments;
ALTER TABLE Tournaments CHANGE Region Name varchar(16) NOT NULL default '';
ALTER TABLE Tournaments DROP Date;
ALTER TABLE Tournaments DROP Contact;
ALTER TABLE Tournaments DROP Phone;
ALTER TABLE Tournaments DROP Description;
ALTER TABLE Tournaments DROP Directions;
ALTER TABLE Tournaments ADD NextTournament varchar(16) default NULL;
ALTER TABLE Teams ADD CurrentTournament varchar(16) NOT NULL default 'DUMMY';
UPDATE TournamentParameters SET Param = 'CurrentTournament' WHERE Param = 'Region';
UPDATE Teams SET CurrentTournament = Region;
UPDATE Teams SET CurrentTournament = 'STATE' WHERE ToState = 1;
ALTER TABLE Teams DROP ToState;
UPDATE Tournaments SET NextTournament = 'STATE' WHERE Name != 'STATE';
ALTER TABLE Teams CHANGE Region EntryTournament varchar(16) NOT NULL default 'DUMMY';
ALTER TABLE TournamentTeams CHANGE Region EntryTournament varchar(16) NOT NULL default 'DUMMY';

ALTER TABLE Teams CHANGE EntryTournament Region varchar(16) NOT NULL default 'DUMMY';

DROP TABLE TournamentTeams;
CREATE TABLE TournamentTeams (
  TeamNumber integer NOT NULL,
  Tournament varchar(16) NOT NULL,
  advanced bool NOT NULL default 0,
  PRIMARY KEY (TeamNumber, Tournament)
);

INSERT INTO TournamentTeams (TeamNumber, Tournament) SELECT Teams.TeamNumber, Teams.Region FROM Teams;
INSERT INTO TournamentTeams (TeamNumber, Tournament) SELECT Teams.TeamNumber, Teams.CurrentTournament FROM Teams WHERE Teams.CurrentTournament <> Teams.Region;

ALTER TABLE Teams DROP City;
ALTER TABLE Teams DROP Coach;
ALTER TABLE Teams DROP CurrentTournament;
ALTER TABLE Teams DROP Email;
ALTER TABLE Teams DROP HowFoundOut;
ALTER TABLE Teams DROP NumBoys;
ALTER TABLE Teams DROP NumGirls;
ALTER TABLE Teams DROP Phone;

ALTER TABLE Teams CHANGE TeamNumber TeamNumber integer NOT NULL;
ALTER TABLE Teams CHANGE Division Division varchar(32) NOT NULL default '1';
ALTER TABLE Teams CHANGE NumMedals NumMedals integer;
ALTER TABLE SummarizedScores CHANGE TeamNumber TeamNumber integer NOT NULL;

UPDATE Tournaments SET NextTournament = NULL WHERE NextTournament = '';

ALTER TABLE Performance DROP Corrected;
ALTER TABLE Performance DROP Verified;
-- applied to here
