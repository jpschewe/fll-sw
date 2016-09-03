# Challenge Description

The main input to the software is a challenge description.
This file specifies how the tournament is scored.
This includes both the performance and all subjective judging categories.
It also contains information about how the categories are weighted to compute an overall score.

We wanted to make sure that the score entry page was generated from the description and that we would not need to modify any code to support any new FIRST challenge. 
This was the driving force behind creating the description format in the first place. 
Since then this software and description format has been used for tournaments other than FLL (drag race, renewable energy).
  
Since 2012 we have added the ability to specify the judging rubric information in the challenge description.
This allows me to use the description to create a subjective judging application.

The file formant is XML.
We choose this format because it is structured and can be validated when it is parsed so that we didn't need to write a lot of validation code.
There are also XML parsers for many languages, so others can read this as well.

To allow the parser to validate the XML file we have created a [schema](../../src/fll/resources/fll.xsd).
The schema is written to do as much validation as possible inside the parser.
There are annotation elements in the schema to describe what the elements mean and how they are to be used.
  
There is also [a CSS stylesheet](../../web/fll.css) for visualizing a challenge description.
This stylized version can be seen from a linke to the challenge description on the index page.
One can use this stylized version to create a PDF that can be shared with non-developers that are checking if the description is written properly.

As the challenge description format has grown over time we have wanted to make sure that we can load any previous challenge description. 
Because of this there are many optional attributes and the code the uses the XML needs to handle this. 
we have also needed to add some schema version information to the header so that the software can automatically transform the XML document on load to support modified features that couldn't be handled with optional attributes.
       
There are no special considerations for internationalization. 
Someone can create a different version of the document for each language replacing all of the display and title attributes as well as the text for the rubrics.

All of the challenge descriptions since 2003 are available [in the repository](../../src/fll/resources/challenge-descriptors).

There is a Java object version of the challenge description available.
The top level class is [ChallengeDescription.java](../../src/fll/xml/ChallengeDescription.java).

Eventually we should have a graphical tool for editing and creating the challenge descriptions.
See issue #173 for the progress on this.
  
## Features
* Code reuse is done where possible, so you will see that the same elements are used for both the performance missions and the subjective categories.
* Goals have a raw value and a computed value. The raw value is the count and the computed value is what is used to add to the score. Some goals have a 0 multiplier so that the computed value is zero and the raw value is used in a computed goal to correctly compute the score.
* Computed goals exist that can create a score from the values of other goals. This is useful for bonus points and for times when one wants to break down a complicated mission into multiple separate goals that are easier for the refs to score.
* Goals are either counts with a multiplier or enumerated with a multiplier or are computed goals which depend on other goals.
* Goals have both names and titles. The name is used internally for the database schema while the title is what the user sees.
* There is the concept of a tie breaker in the performance part of the description. This is because we (in Minnesota) run a single elimination tournament at the end of the day to keep the kids busy while we are deliberating on judging scores. So if 2 teams end up with the same score we have a way to break the tie unless they complete exactly the same missions.
* Score values can be integers or floats. We needed floats first for the drag race and later for one of the FIRST challenge bonus missions. 
* When defining floating point goals one can decide how the values are converted to integers by specifying if the value should be rounded or truncated.
* There is support for restrictions that validate constraints that apply across multiple goals such as the number of bricks on the performance table.


