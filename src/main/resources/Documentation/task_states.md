@PLUGIN@ States
===============

Below is an exampe config file which illustrates how task states are affected
by their own criteria.

```
[root "Root straight PASS"]
  applicable = is:open
  pass = is:open

[root "Root straight FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open
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
               "name" : "Root straight PASS",
               "status" : "WAITING"
            },
            {
               "name" : "Root straight FAIL",
               "status" : "WAITING"
            }
         ]
      }
   ],
   ...
```
