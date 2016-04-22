@PLUGIN@ States
===============

Below is an sample config file which illustrates how task applicability works.

```
[root "Root APPLICABLE"]
  applicable = is:open

[root "Root N/A"]
  applicable = is:closed
```

The expected output for the above task config looks like:

```
 $  ssh -x -p 29418 review-example gerrit query is:open \
     --task--applicable --format json|head -1 |json_pp
{
   ...,
   "plugins" : [
      {
         "name" : "task",
         "roots" : [
            {
               "name" : "Root APPLICABLE",
               "status" : "WAITING"
            }
         ]
      }
   ],
   ...
```
