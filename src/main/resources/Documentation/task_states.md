@PLUGIN@ States
===============

Below is an exampe config file which illustrates many examples of how task
states are affected by their own criteria and their subtasks' states.

```
[root "Root straight PASS"]
  applicable = is:open
  pass = is:open

[root "Root straight FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open

[root "Root straight INVALID"]
  applicable = is:open

[root "Root grouping PASS (subtask PASS)"]
  applicable = is:open
  subtask = Subtask PASS

[root "Root grouping WAITING (subtask READY)"]
  applicable = is:open
  subtask = Subtask READY

[root "Root grouping WAITING (subtask FAIL)"]
  applicable = is:open
  subtask = Subtask FAIL

[root "Root grouping WAITING (subtask INVALID)"]
  applicable = is:open
  subtask = Subtask INVALID

[root "Root grouping WAITING (subtask missing)"]
  applicable = is:open
  subtask = MISSING

[root "Root READY (subtask PASS)"]
  applicable = is:open
  pass = -is:open
  subtask = Subtask PASS
  ready-hint = You must now run the ready task

[root "Root WAITING (subtask READY)"]
  applicable = is:open
  pass = is:open
  subtask = Subtask READY

[root "Root WAITING (subtask FAIL)"]
  applicable = is:open
  pass = is:open
  subtask = Subtask FAIL

[root "Root WAITING (subtask INVALID)"]
  applicable = is:open
  pass = is:open
  subtask = Subtask INVALID

[root "Root WAITING (subtask missing)"]
  applicable = is:open
  pass = is:open
  subtask = MISSING

[root "Root IN PROGRESS"]
   applicable = is:open
   in-progress = is:open
   pass = -is:open

[root "Root NOT IN PROGRESS"]
   applicable = is:open
   in-progress = -is:open
   pass = -is:open

[task "Subtask FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open

[task "Subtask READY"]
  applicable = is:open
  pass = -is:open
  subtask = Subtask PASS

[task "Subtask PASS"]
  applicable = is:open
  pass = is:open

[task "Subtask INVALID"]
  applicable = is:open
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
               "status" : "PASS"
            },
            {
               "name" : "Root straight FAIL",
               "status" : "FAIL"
            },
            {
               "name" : "Root straight INVALID",
               "status" : "INVALID"
            },
            {
               "name" : "Root grouping PASS (subtask PASS)",
               "status" : "PASS",
               "subTasks" : [
                  {
                     "name" : "Subtask PASS",
                     "status" : "PASS"
                  }
               ]
            },
            {
               "name" : "Root grouping WAITING (subtask READY)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "Subtask READY",
                     "status" : "READY",
                     "subTasks" : [
                        {
                           "name" : "Subtask PASS",
                           "status" : "PASS"
                        }
                     ]
                  }
               ]
            },
            {
               "name" : "Root grouping WAITING (subtask FAIL)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "Subtask FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Root grouping WAITING (subtask INVALID)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "Subtask INVALID",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "name" : "Root grouping WAITING (subtask missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "MISSING",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "name" : "Root READY (subtask PASS)",
               "readyHint" : "You must now run the ready task",
               "status" : "READY",
               "subTasks" : [
                  {
                     "name" : "Subtask PASS",
                     "status" : "PASS"
                  }
               ]
            },
            {
               "name" : "Root WAITING (subtask READY)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "Subtask READY",
                     "status" : "READY",
                     "subTasks" : [
                        {
                           "name" : "Subtask PASS",
                           "status" : "PASS"
                        }
                     ]
                  }
               ]
            },
            {
               "name" : "Root WAITING (subtask FAIL)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "Subtask FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Root WAITING (subtask INVALID)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "Subtask INVALID",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "name" : "Root WAITING (subtask missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "MISSING",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "name" : "Root IN PROGRESS",
               "status" : "READY"
            },
            {
               "name" : "Root NOT IN PROGRESS",
               "status" : "READY"
            }
         ]
      }
   ],
   ...
```
