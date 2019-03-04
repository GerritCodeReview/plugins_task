@PLUGIN@ States
===============

Below are sample config files which illustrate many examples of how task
states are affected by their own criteria and their subtasks' states.

`task.config` file in project `All-Project` on ref `refs/meta/config`.

```
[root "Root N/A"]
  applicable = is:closed

[root "Root PASS"]
  applicable = is:open
  pass = True

[root "Root FAIL"]
  applicable = is:open
  fail = True

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

[root "Root Properties"]
  fail = True
  fail-hint = Name(${_name})
  subtask = Subtask Properties

[root "INVALIDS"]
  applicable = is:open
  subtasks-file = invalids.config

[root "Root NA Pass"]
  applicable = -is:open
  pass = True

[root "Root NA Fail"]
  applicable = -is:open
  fail = True

[root "NA INVALIDS"]
  applicable = -is:open
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

[task "Subtask Properties"]
  fail = True
  fail-hint = Name(${_name})
  subtask = Chained ${_name}

[task "Chained Subtask Properties"]
  pass = True

[external "user special"]
  user = testuser
  file = special.config

[external "user missing"]
  user = missing
  file = special.config

[external "file missing"]
  user = testuser
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

[task "NA Bad PASS query"]
  applicable = -is:open
  fail = True
  pass = has:bad

[task "NA Bad FAIL query"]
  applicable = -is:open
  pass = True
  fail = has:bad

[task "NA Bad INPROGRESS query"]
  applicable = -is:open
  fail = True
  in-progress = has:bad

[task "Looping"]
  subtask = Looping

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
               "hasPass" : true,
               "name" : "Root PASS",
               "status" : "PASS"
            },
            {
               "hasPass" : true,
               "name" : "Root FAIL",
               "status" : "FAIL"
            },
            {
               "hasPass" : true,
               "name" : "Root straight PASS",
               "status" : "PASS"
            },
            {
               "hasPass" : true,
               "name" : "Root straight FAIL",
               "status" : "FAIL"
            },
            {
               "hasPass" : true,
               "name" : "Root PASS-fail",
               "status" : "PASS"
            },
            {
               "hasPass" : true,
               "name" : "Root pass-FAIL",
               "status" : "FAIL"
            },
            {
               "hasPass" : false,
               "name" : "Root grouping PASS (subtask PASS)",
               "status" : "PASS",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask PASS",
                     "status" : "PASS"
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Root grouping WAITING (subtask READY)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask READY",
                     "status" : "READY",
                     "subTasks" : [
                        {
                           "hasPass" : true,
                           "name" : "Subtask PASS",
                           "status" : "PASS"
                        }
                     ]
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Root grouping WAITING (subtask FAIL)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "hasPass" : true,
               "hint" : "You must now run the ready task",
               "name" : "Root READY (subtask PASS)",
               "status" : "READY",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask PASS",
                     "status" : "PASS"
                  }
               ]
            },
            {
               "hasPass" : true,
               "name" : "Root WAITING (subtask READY)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask READY",
                     "status" : "READY",
                     "subTasks" : [
                        {
                           "hasPass" : true,
                           "name" : "Subtask PASS",
                           "status" : "PASS"
                        }
                     ]
                  }
               ]
            },
            {
               "hasPass" : true,
               "name" : "Root WAITING (subtask FAIL)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "hasPass" : true,
               "inProgress" : true,
               "name" : "Root IN PROGRESS",
               "status" : "READY"
            },
            {
               "hasPass" : true,
               "inProgress" : false,
               "name" : "Root NOT IN PROGRESS",
               "status" : "READY"
            },
            {
               "hasPass" : false,
               "name" : "Subtasks File",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "file task/common.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "hasPass" : true,
                     "name" : "file task/common.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Subtasks File (Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "file task/common.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "hasPass" : true,
                     "name" : "file task/common.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Subtasks External",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Subtasks External (Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  },
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Subtasks External (User Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  },
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "Subtasks External (File Missing)",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config PASS",
                     "status" : "PASS"
                  },
                  {
                     "hasPass" : true,
                     "name" : "userfile task/special.config FAIL",
                     "status" : "FAIL"
                  }
               ]
            },
            {
               "hasPass" : true,
               "hint" : "Name(Root Properties)",
               "name" : "Root Properties",
               "status" : "FAIL",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "hint" : "Name(Subtask Properties)",
                     "name" : "Subtask Properties",
                     "status" : "FAIL",
                     "subTasks" : [
                        {
                           "hasPass" : true,
                           "name" : "Chained Subtask Properties",
                           "status" : "PASS"
                        }
                     ]
                  }
               ]
            },
            {
               "hasPass" : false,
               "name" : "INVALIDS",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : false,
                     "name" : "No PASS criteria",
                     "status" : "INVALID"
                  },
                  {
                     "hasPass" : true,
                     "name" : "WAITING (subtask INVALID)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : false,
                           "name" : "Subtask INVALID",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "hasPass" : true,
                     "name" : "WAITING (subtask missing)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : false,
                           "name" : "MISSING",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "hasPass" : false,
                     "name" : "Grouping WAITING (subtask INVALID)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : false,
                           "name" : "Subtask INVALID",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "hasPass" : false,
                     "name" : "Grouping WAITING (subtask missing)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : false,
                           "name" : "MISSING",
                           "status" : "INVALID"
                        }
                     ]
                  },
                  {
                     "hasPass" : false,
                     "name" : "Subtask INVALID",
                     "status" : "INVALID"
                  },
                  {
                     "hasPass" : false,
                     "name" : "Looping",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "name" : "UNKNOWN",
                           "status" : "INVALID"
                        }
                     ]
                  }
               ]
            }
         ]
      }
   ],
   ...
```
