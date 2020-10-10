# Normalized Scores

Raw scores simply can't be added for a number of reasons:
  - All scores must be brought to the same scale (Performance ranges from 1 - 400, but judged scores may only range from 1 - 50)
  - Even out judges who scale differently. Some judges score teams lower than others.
  
The master score is put together with weighting given to certain categories to emphasize certain aspects of the competition, normalization must be done to preserve that weighting

## Normalization Details

A ratings scale is developed to normalize around an average score of 100 for each category.
We normalize based on how far your score is from the average score in that group of scores.
So it’s how far you are above or below the average score in each judged category that counts.
A score of 120, is one standard deviation above the mean (average).

This  accounts for the amount of variability of judges scores as well as their average.
There are a number of ways to normalize, but normalization to the average is generally considered best by statisticians.


## Deep normalization details

Compute a z score for each team for each event.
A z score is the number of standard deviations the team's score is away from the mean of the group being averaged.
Z-scores by definition have a mean of zero and a standard deviation of 1.

To get a mean of 100 and a standard deviation of 20, we multiply the Z score by 20 and add that result to 100.

This not only centers the mean at 100, but it makes sure that a score that is better than 66% of the competitors in that category is always 120 and a score that is better than 95% of the competitors in that category is always 140, etc.

This accounts for the amount of variability of judges scores as well as their average.
