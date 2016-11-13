@PLUGIN@ States
===============

Below are sample config files which illustrate many examples of how task
states are affected by their own criteria and their subtasks' states.

`task.config` file in project `All-Project` on ref `refs/meta/config`.

```
[root "Root PASS"]
  applicable = has:draft
  pass = True

[root "Root FAIL"]
  applicable = has:draft
  fail = True
  fail-hint = Change has a draft

[root "Root straight PASS"]
  applicable = has:draft
  pass = has:draft

[root "Root straight FAIL"]
  applicable = has:draft
  fail = has:draft
  pass = has:draft

[root "Root PASS-fail"]
  applicable = has:draft
  fail = NOT has:draft

[root "Root pass-FAIL"]
  applicable = has:draft
  fail = has:draft

[root "Root grouping PASS (subtask PASS)"]
  applicable = has:draft
  subtask = Subtask PASS

[root "Root grouping WAITING (subtask READY)"]
  applicable = has:draft
  subtask = Subtask READY

[root "Root grouping WAITING (subtask FAIL)"]
  applicable = has:draft
  subtask = Subtask FAIL

[root "Root grouping NA (subtask NA)"]
  applicable = has:draft
  subtask = Subtask NA

[root "Root READY (subtask PASS)"]
  applicable = has:draft
  pass = -has:draft
  subtask = Subtask PASS
  ready-hint = You must now run the ready task

[root "Root WAITING (subtask READY)"]
  applicable = has:draft
  pass = has:draft
  subtask = Subtask READY

[root "Root WAITING (subtask FAIL)"]
  applicable = has:draft
  pass = has:draft
  subtask = Subtask FAIL

[root "Root IN PROGRESS"]
   applicable = has:draft
   in-progress = has:draft
   pass = -has:draft

[root "Root NOT IN PROGRESS"]
   applicable = has:draft
   in-progress = -has:draft
   pass = -has:draft

[root "Subtasks File"]
  applicable = has:draft
  subtasks-file = common.config

[root "Subtasks File (Missing)"]
  applicable = has:draft
  subtasks-file = common.config
  subtasks-file = missing

[root "Subtasks External"]
  applicable = has:draft
  subtasks-external = user special

[root "Subtasks External (Missing)"]
  applicable = has:draft
  subtasks-external = user special
  subtasks-external = missing

[root "Subtasks External (User Missing)"]
  applicable = has:draft
  subtasks-external = user special
  subtasks-external = user missing

[root "Subtasks External (File Missing)"]
  applicable = has:draft
  subtasks-external = user special
  subtasks-external = file missing

[root "INVALIDS"]
  applicable = has:draft
  subtasks-file = invalids.config

[task "Subtask FAIL"]
  applicable = has:draft
  fail = has:draft
  pass = has:draft

[task "Subtask READY"]
  applicable = has:draft
  pass = -has:draft
  subtask = Subtask PASS

[task "Subtask PASS"]
  applicable = has:draft
  pass = has:draft

[task "Subtask NA"]
  applicable = NOT has:draft

[external "user special"]
  user = mfick
  file = special.config

[external "user missing"]
  user = missing
  file = special.config

[external "file missing"]
  user = mfick
  file = missing
```

`task/common.config` file in project `All-Projects` on ref `refs/meta/config`.

```
[task "file task/common.config PASS"]
  applicable = has:draft
  pass = has:draft

[task "file task/common.config FAIL"]
  applicable = has:draft
  fail = has:draft
  pass = has:draft
```

`task/invalids.config` file in project `All-Projects` on ref `refs/meta/config`.

```
[task "No PASS criteria"]
  applicable = has:draft

[task "WAITING (subtask INVALID)"]
  applicable = has:draft
  pass = has:draft
  subtask = Subtask INVALID

[task "WAITING (subtask missing)"]
  applicable = has:draft
  pass = has:draft
  subtask = MISSING # security bug: subtask name appears in output

[task "Grouping WAITING (subtask INVALID)"]
  applicable = has:draft
  subtask = Subtask INVALID

[task "Grouping WAITING (subtask missing)"]
  applicable = has:draft
  subtask = MISSING  # security bug: subtask name appears in output

[task "Subtask INVALID"]
  applicable = has:draft

```

`task/special.config` file in project `All-Users` on ref `refs/users/01/1000001`.

```
[task "userfile task/special.config PASS"]
  applicable = has:draft
  pass = has:draft

[task "userfile task/special.config FAIL"]
  applicable = has:draft
  fail = has:draft
  pass = has:draft
```

The expeced output for the above task configs looks like:

```
 $  ssh -x -p 29418 review-example gerrit query has:draft \
     --task--applicable --format json|head -1 |json_pp
{
   ...,
   "plugins" : [
      {
         "roots" : [
            {
               "status" : "PASS",
               "name" : "Root PASS"
            },
            {
               "hint" : "Change has a draft",
               "status" : "FAIL",
               "name" : "Root FAIL"
            },
            {
               "status" : "PASS",
               "name" : "Root straight PASS"
            },
            {
               "status" : "FAIL",
               "name" : "Root straight FAIL"
            },
            {
               "status" : "PASS",
               "name" : "Root PASS-fail"
            },
            {
               "status" : "FAIL",
               "name" : "Root pass-FAIL"
            },
            {
               "subTasks" : [
                  {
                     "status" : "PASS",
                     "name" : "Subtask PASS"
                  }
               ],
               "status" : "PASS",
               "name" : "Root grouping PASS (subtask PASS)"
            },
            {
               "subTasks" : [
                  {
                     "subTasks" : [
                        {
                           "status" : "PASS",
                           "name" : "Subtask PASS"
                        }
                     ],
                     "status" : "READY",
                     "name" : "Subtask READY"
                  }
               ],
               "status" : "WAITING",
               "name" : "Root grouping WAITING (subtask READY)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "FAIL",
                     "name" : "Subtask FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Root grouping WAITING (subtask FAIL)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "PASS",
                     "name" : "Subtask PASS"
                  }
               ],
               "hint" : "You must now run the ready task",
               "status" : "READY",
               "name" : "Root READY (subtask PASS)"
            },
            {
               "subTasks" : [
                  {
                     "subTasks" : [
                        {
                           "status" : "PASS",
                           "name" : "Subtask PASS"
                        }
                     ],
                     "status" : "READY",
                     "name" : "Subtask READY"
                  }
               ],
               "status" : "WAITING",
               "name" : "Root WAITING (subtask READY)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "FAIL",
                     "name" : "Subtask FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Root WAITING (subtask FAIL)"
            },
            {
               "inProgress" : true,
               "status" : "READY",
               "name" : "Root IN PROGRESS"
            },
            {
               "inProgress" : false,
               "status" : "READY",
               "name" : "Root NOT IN PROGRESS"
            },
            {
               "subTasks" : [
                  {
                     "status" : "PASS",
                     "name" : "file task/common.config PASS"
                  },
                  {
                     "status" : "FAIL",
                     "name" : "file task/common.config FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Subtasks File"
            },
            {
               "subTasks" : [
                  {
                     "status" : "PASS",
                     "name" : "file task/common.config PASS"
                  },
                  {
                     "status" : "FAIL",
                     "name" : "file task/common.config FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Subtasks File (Missing)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "PASS",
                     "name" : "userfile task/special.config PASS"
                  },
                  {
                     "status" : "FAIL",
                     "name" : "userfile task/special.config FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Subtasks External"
            },
            {
               "subTasks" : [
                  {
                     "status" : "INVALID",
                     "name" : "UNKNOWN"
                  },
                  {
                     "status" : "PASS",
                     "name" : "userfile task/special.config PASS"
                  },
                  {
                     "status" : "FAIL",
                     "name" : "userfile task/special.config FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Subtasks External (Missing)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "INVALID",
                     "name" : "UNKNOWN"
                  },
                  {
                     "status" : "PASS",
                     "name" : "userfile task/special.config PASS"
                  },
                  {
                     "status" : "FAIL",
                     "name" : "userfile task/special.config FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Subtasks External (User Missing)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "PASS",
                     "name" : "userfile task/special.config PASS"
                  },
                  {
                     "status" : "FAIL",
                     "name" : "userfile task/special.config FAIL"
                  }
               ],
               "status" : "WAITING",
               "name" : "Subtasks External (File Missing)"
            },
            {
               "subTasks" : [
                  {
                     "status" : "INVALID",
                     "name" : "No PASS criteria"
                  },
                  {
                     "subTasks" : [
                        {
                           "status" : "INVALID",
                           "name" : "Subtask INVALID"
                        }
                     ],
                     "status" : "WAITING",
                     "name" : "WAITING (subtask INVALID)"
                  },
                  {
                     "subTasks" : [
                        {
                           "status" : "INVALID",
                           "name" : "MISSING"
                        }
                     ],
                     "status" : "WAITING",
                     "name" : "WAITING (subtask missing)"
                  },
                  {
                     "subTasks" : [
                        {
                           "status" : "INVALID",
                           "name" : "Subtask INVALID"
                        }
                     ],
                     "status" : "WAITING",
                     "name" : "Grouping WAITING (subtask INVALID)"
                  },
                  {
                     "subTasks" : [
                        {
                           "status" : "INVALID",
                           "name" : "MISSING"
                        }
                     ],
                     "status" : "WAITING",
                     "name" : "Grouping WAITING (subtask missing)"
                  },
                  {
                     "status" : "INVALID",
                     "name" : "Subtask INVALID"
                  }
               ],
               "status" : "WAITING",
               "name" : "INVALIDS"
            }
         ],
         "name" : "task"
      }
   ],
   ...
```
