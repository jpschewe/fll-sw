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
