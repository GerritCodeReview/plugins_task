@PLUGIN@ States
===============

Below are sample config files which illustrate many examples of how task
states are affected by their own criteria and their subtasks' states.

`task.config` file in project `All-Project` on ref `refs/meta/config`.

```
[root "Root straight PASS"]
  applicable = is:open
  pass = is:open

[root "Root straight FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open

[root "Root PASS-fail"]
  applicable = is:open
  fail = NOT is:open

[root "Root pass-FAIL"]
  applicable = is:open
  fail = is:open

[root "Root grouping PASS (subtask PASS)"]
  applicable = is:open
  subtask = Subtask PASS

[root "Root grouping WAITING (subtask READY)"]
  applicable = is:open
  subtask = Subtask READY

[root "Root grouping WAITING (subtask FAIL)"]
  applicable = is:open
  subtask = Subtask FAIL

[root "Root grouping NA (subtask NA)"]
  applicable = is:open
  subtask = Subtask NA

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

[root "Root IN PROGRESS"]
   applicable = is:open
   in-progress = is:open
   pass = -is:open

[root "Root NOT IN PROGRESS"]
   applicable = is:open
   in-progress = -is:open
   pass = -is:open

[root "Subtasks File"]
  applicable = is:open
  subtasks-file = common.config

[root "Subtasks File (Missing)"]
  applicable = is:open
  subtasks-file = common.config
  subtasks-file = missing

[root "Subtasks External"]
  applicable = is:open
  subtasks-external = user special

[root "Subtasks External (Missing)"]
  applicable = is:open
  subtasks-external = user special
  subtasks-external = missing

[root "Subtasks External (User Missing)"]
  applicable = is:open
  subtasks-external = user special
  subtasks-external = user missing

[root "Subtasks External (File Missing)"]
  applicable = is:open
  subtasks-external = user special
  subtasks-external = file missing

[root "INVALIDS"]
  applicable = is:open
  subtasks-file = invalids.config

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

[task "Subtask NA"]
  applicable = NOT is:open

[external "user special"]
  user = current-user
  file = special.config

[external "user missing"]
  user = missing
  file = special.config

[external "file missing"]
  user = current-user
  file = missing
```

`task/common.config` file in project `All-Projects` on ref `refs/meta/config`.

```
[task "file task/common.config PASS"]
  applicable = is:open
  pass = is:open

[task "file task/common.config FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open
```

`task/invalids.config` file in project `All-Projects` on ref `refs/meta/config`.

```
[task "No PASS criteria"]
  applicable = is:open

[task "WAITING (subtask INVALID)"]
  applicable = is:open
  pass = is:open
  subtask = Subtask INVALID

[task "WAITING (subtask missing)"]
  applicable = is:open
  pass = is:open
  subtask = MISSING # security bug: subtask name appears in output

[task "Grouping WAITING (subtask INVALID)"]
  applicable = is:open
  subtask = Subtask INVALID

[task "Grouping WAITING (subtask missing)"]
  applicable = is:open
  subtask = MISSING  # security bug: subtask name appears in output

[task "Subtask INVALID"]
  applicable = is:open

[external "external missing"]
  user = mfick
  file = missing
```

`task/special.config` file in project `All-Users` on ref `refs/users/self`.

```
[task "userfile task/special.config PASS"]
  applicable = is:open
  pass = is:open

[task "userfile task/special.config FAIL"]
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
               "status" : "PASS"
            },
            {
               "name" : "Root straight FAIL",
               "status" : "FAIL"
            },
            {
               "name" : "Root PASS-fail",
               "status" : "PASS"
            },
            {
               "name" : "Root pass-FAIL",
               "status" : "FAIL"
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
               "inProgress" : true,
               "name" : "Root IN PROGRESS",
               "status" : "READY"
            },
            {
               "inProgress" : false,
               "name" : "Root NOT IN PROGRESS",
               "status" : "READY"
            },
            {
               "name" : "Subtasks File",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "file task/common.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "name" : "file task/common.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Subtasks File (Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "file task/common.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "name" : "file task/common.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Subtasks External",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Subtasks External (Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  },
                  {
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Subtasks External (User Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  },
                  {
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "Subtasks External (File Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "name" : "INVALIDS",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "name" : "No PASS criteria",
                     "status" : "INVALID"
                  },
                  {
                     "name" : "WAITING (subtask INVALID)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "name" : "Subtask INVALID",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "name" : "WAITING (subtask missing)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "name" : "MISSING",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "name" : "Grouping WAITING (subtask INVALID)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "name" : "Subtask INVALID",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "name" : "Grouping WAITING (subtask missing)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "name" : "MISSING",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "name" : "Subtask INVALID",
                     "status" : "INVALID"
                  }
               ]
            }
         ]
      }
   ],
   ...
```
