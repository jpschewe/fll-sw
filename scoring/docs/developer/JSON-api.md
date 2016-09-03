# JSON API

This document describes the JSON objects available through the REST api.

## Version

URL: http://localhost:9080/fll-sw/api/Version

Java class: fll.web.api.VersionServlet

GET returns the version of the software running on the server as a string.
    
    "10.8-79-gfa9223c"


## Check authentication

Check if the current session is authenticated.
If the session is not authenticated a login is required using the form at http://localhost:9080/fll-sw//login.jsp.


Java class: fll.web.api.CheckAuthServlet

URL: http://localhost:9080/fll-sw/api/CheckAuth

GET returns if the session is authenticated (Java class fll.web.api.CheckAuthServlet.AuthResult).

    {"authenticated":true}

## Judges

Access the list of judges.

Java class: fll.web.api.JudgesServlet

URL: http://localhost:9080/fll-sw/api/Judges

GET returns a list of all of the judges (Java class fll.JudgeInformation). 

    [
      {
        "id":"ALAN YOUSHA",
        "category":"robot_design",
        "group":"Lakes"
      },
      {
        "id":"ALEX DIETZ",
        "category":"project",
        "group":"Marshes"
      },
      ... other judges
    ]
    
POST expects a list of judges and will add the new judges to the database. 
New is determined by checking if the judge's id is already in the database.


## Tournaments

URL: http://localhost:9080/fll-sw/api/Tournaments

Java class: fll.web.api.TournamentsServlet

GET returns the list of all tournaments (Java class fll.Tournament) known to the software.  

    [
      {
        "tournamentID":2,
        "name":"1/10/16 - Elk River",
        "location":"1/10/16 - Elk River"
      }
    ]

URL: http://localhost:9080/fll-sw/api/Tournaments/<id>

GET returns the tournament object with the specified id.

    {"tournamentID":2,"name":"1/10/16 - Elk River","location":"1/10/16 - Elk River"}

## Tournament Teams

Access the list of teams at the current tournament.

Java class: fll.web.api.TournamentTeamsServlet

URL: http://localhost:9080/fll-sw/api/TournamentTeams

GET returns the list of teams (Java class fll.TournamentTeam) at the current tournament.
   
    [
      {
        "teamNumber":17,
        "organization":"Friends",
        "teamName":"The Brick Bombers",
        "awardGroup":"State",
        "judgingGroup":"Woods"
      },
    ... other teams
    ]


## Subjective scores

Access the subjective scores.

Java class: fll.web.api.SubjectiveScoresServlet

URL: http://localhost:9080/fll-sw/api/SubjectiveScores

GET returns : {category, {judge, {teamNumber, SubjectiveScore (Java class fll.SubjectiveScore)}}}

    {
      "robot_design":{
        "ANDREW ELDRED":{
          "2610":{
            "scoreOnServer":true,
            "deleted":false,
            "judge":"ANDREW ELDRED",
            "modified":false,
            "teamNumber":2610,
            "noShow":false,        
            "standardSubScores"{
              "innovation":8.0,
              "design_process":8.0,
              "attachments":10.0,
              "durability":9.0,
              "kids_work":12.0,
              "mission_strategy":10.0,
              "mechanical_efficiency":8.0,
              "mechanization":8.0
            },
            "enumSubScores":{},
            "note":null
            },
            ... other teams and scores
          },
          ... other judges
        },
        ... other subjective categories
      }
    }

 POST expects data in the same format as GET and returns status of saving scores (Java class fll.web.api.SubjectiveScoresServlet.UploadResult).
 Any scores marked modified or deleted will overwrite any scores in the database for the specified team, category, judge.

    {
      "success":true,
      "message":"error message goes here",
      "numModified":10
    }


## Schedule

Access to the tournament schedule.

Java class: fll.web.api.ScheduleServlet

URL: http://localhost:9080/fll-sw/api/Schedule

GET returns the schedule (Java class fll.scheduler.TournamentSchedule)

    {
      "subjectiveStations":["Design","Project","Core Values"],
      "name":"Sample8",
      "numberOfRounds":3,
      "awardGroups":["Lakes"],
      "judgingGroups":["Lakes"],
      "tableColors":["Red"],
      "schedule":[
        {
          "teamNumber":100,
          "teamName":"Team 100",
          "organization":"Org 100",
          "awardGroup":"Lakes",
          "numberOfRounds":3,
          "subjectiveTimes":[
            {
              "name":"Design",
              "time":{"hour":10,"minute":40,"second":0,"nano":0}
            },
            {"name":"Project","time":{"hour":8,"minute":30,"second":0,"nano":0}},
            {"name":"Core Values","time":{"hour":9,"minute":10,"second":0,"nano":0}}
          ],
          "judgingGroup":"Lakes",
          "knownSubjectiveStations":["Design","Project","Core Values"]
        },
        ... other schedule entries
      ]
    }
    

## Category schedule mapping

Used to map subjective categories to columns in the schedule.

Java class: fll.web.api.CategoryScheduleMappingServlet

URL: http://localhost:9080/fll-sw/api/CategoryScheduleMapping

GET returns the a list of mappings of category names to columns (subjective stations) in the schedule (Java class fll.db.CategoryColumnMapping).

    [
      {
        "categoryName":"core_values",
        "scheduleColumn":"Core Values"
      },
      {
        "categoryName":"project",
        "scheduleColumn":"Project"
      },
      {
        "categoryName":"robot_design",
        "scheduleColumn":"Design"
      },
      {
        "categoryName":"robot_programming",
        "scheduleColumn":"Design"
      }
    ]
    

## Challenge Description

Access the challenge description. 
This describes everything about the tournament.

Java class: fll.web.api.ChallengeDescriptionServlet

URL: http://localhost:9080/fll-sw/api/ChallengeDescription

GET returns the challenge description (Java class fll.xml.ChallengeDescription).
Example output not show as it's quite large.
It's best to just walk the Java object and not that all properties (get methods) are JSON attributes.


## Subjective categories

Used to get the list of subjective categories in the challenge description.
This information is also available through the challenge description API call.

Java class: fll.web.api.challengeDescription.SubjectiveCategories

URL: http://localhost::9080/fll-sw/api/ChallengeDescription/SubjectiveCategories

Result is the list of subjective categories (Java class fll.xml.ScoreCategory) and the rubric for each category.
Each score category has a title for display, name for internal reference, weight and a list of goals ("goals").
Each goal has a title for display, long description, a name for internal reference, information to score the goal and as a list of rubric ranges ("rubric"). 
Each rubric range has a title, short description, long description, min value and max value. These are the ranges on the subjective score sheets.


    [{"title":"Robot Design",
      "goals":[
        {"rubric":[
          {"fullDescription":"Quite fragile; breaks a lot. Base weak, falls apart when handled or run. Difficult to assemble.",
        "title":"Beginning","min":1,"shortDescription":"Quite fragile; breaks a lot","max":3,"description":"Base weak, falls apart when handled or run. Difficult to assemble."},
          {"fullDescription":"Frequent or significant faults/repairs. Robot has some stability. Assembles with few errors.","title":"Developing","min":4,"shortDescription":"Frequent or significant faults/repairs","max":6,"description":"Robot has some stability. Assembles with few errors."},
          {"fullDescription":"Rare faults/repairs. Robot stable, but not robust. Assembles with no errors.","title":"Accomplished","min":7,"shortDescription":"Rare faults/repairs","max":9,"description":"Robot stable, but not robust. Assembles with no errors."},
          {"fullDescription":"Sound construction; no repairs. Robot stable and robust. Robot assembles easily.","title":"Exemplary","min":10,"shortDescription":"Sound construction; no repairs","max":12,"description":"Robot stable and robust. Robot assembles easily."}
        ],
        "initialValue":0.0,
        "category":"Mechanical Design",
        "enumerated":false,
        "computed":false,
        "values":[],
        "scoreType":"INTEGER",
        "min":0.0,
        "multiplier":1.0,
        "max":12.0,
        "title":"Durability",
        "required":false,
        "yesNo":false,
        "sortedValues":[],
        "description":"Evidence of structural integrity; ability to\n        withstand rigors of competition.\n      ","name":"durability"},
      ... other goals
      ],          
      "weight":0.5,
      "name":"robot_design"},
      ... other subjective categories
      ]
