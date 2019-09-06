@PLUGIN@ States
===============

Below are sample config files which illustrate many examples of how task
states are affected by their own criteria and their subtasks' states.

`task.config` file in project `All-Project` on ref `refs/meta/config`.

```
[root "Root N/A"]
  applicable = is:closed # Assumes test query is "is:open"

[root "Root APPLICABLE"]
  applicable = is:open # Assumes test query is "is:open"
  pass = True
  subtask = Subtask APPLICABLE

[root "Root PASS"]
  pass = True

[root "Root FAIL"]
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
  subtask = Subtask PASS

[root "Root grouping WAITING (subtask READY)"]
  subtask = Subtask READY

[root "Root grouping WAITING (subtask FAIL)"]
  subtask = Subtask FAIL

[root "Root grouping NA (subtask NA)"]
  applicable = is:open # Assumes Subtask NA has "applicable = NOT is:open"
  subtask = Subtask NA

[root "Root READY (subtask PASS)"]
  applicable = is:open
  pass = NOT is:open
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
   pass = NOT is:open

[root "Root NOT IN PROGRESS"]
   applicable = is:open
   in-progress = NOT is:open
   pass = NOT is:open

[root "Root Optional subtasks"]
   subtask = OPTIONAL MISSING |
   subtask = Subtask Optional |

[root "Subtasks File"]
  subtasks-file = common.config

[root "Subtasks File (Missing)"]
  subtasks-file = common.config
  subtasks-file = missing

[root "Subtasks External"]
  subtasks-external = user special

[root "Subtasks External (Missing)"]
  subtasks-external = user special
  subtasks-external = missing

[root "Subtasks External (User Missing)"]
  subtasks-external = user special
  subtasks-external = user missing

[root "Subtasks External (File Missing)"]
  subtasks-external = user special
  subtasks-external = file missing

[root "Root Properties"]
  set-root-property = root-value
  export-root = ${_name}
  fail = True
  fail-hint = Name(${_name})
  subtask = Subtask Properties

[root "Root Preload"]
   preload-task = Subtask FAIL
   subtask = Subtask Preload

[root "INVALIDS"]
  subtasks-file = invalids.config

[root "Root NA Pass"]
  applicable = NOT is:open # Assumes test query is "is:open"
  pass = True

[root "Root NA Fail"]
  applicable = NOT is:open # Assumes test query is "is:open"
  fail = True

[root "NA INVALIDS"]
  applicable = NOT is:open # Assumes test query is "is:open"
  subtasks-file = invalids.config

[task "Subtask APPLICABLE"]
  applicable = is:open
  pass = True

[task "Subtask FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open

[task "Subtask READY"]
  applicable = is:open
  pass = NOT is:open
  subtask = Subtask PASS

[task "Subtask PASS"]
  applicable = is:open
  pass = is:open

[task "Subtask Optional"]
   subtask = Subtask PASS |
   subtask = OPTIONAL MISSING | Subtask FAIL
   subtask = OPTIONAL MISSING | OPTIONAL MISSING |
   subtask = OPTIONAL MISSING | OPTIONAL MISSING | Subtask READY

[task "Subtask NA"]
  applicable = NOT is:open # Assumes test query is "is:open"

[task "Subtask Properties"]
  export-subtask = ${_name}
  subtask = Subtask Properties Hints
  subtask = Chained ${_name}
  subtask = Subtask Properties Reset

[task "Subtask Properties Hints"]
  set-first-property = first-value
  set-second-property = ${first-property} second-extra ${third-property}
  set-third-property = third-value
  fail = True
  fail-hint = Name(${_name}) root-property(${root-property}) first-property(${first-property}) second-property(${second-property}) root(${root})

[task "Chained Subtask Properties"]
  pass = True

[task "Subtask Properties Reset"]
  pass = True
  set-first-property = reset-first-value
  fail-hint = first-property(${first-property})

[task "Subtask Preload"]
  preload-task = Subtask READY
  subtask = Subtask Preload Preload
  subtask = Subtask Preload Hints PASS
  subtask = Subtask Preload Hints FAIL
  subtask = Subtask Preload Override Pass
  subtask = Subtask Preload Override Fail
  subtask = Subtask Preload Extend Subtasks
  subtask = Subtask Preload Optional
  subtask = Subtask Preload Properties

[task "Subtask Preload Preload"]
  preload-task = Subtask Preload with Preload

[task "Subtask Preload with Preload"]
  preload-task = Subtask PASS

[task "Subtask Preload Hints PASS"]
  preload-task = Subtask Hints
  pass = False

[task "Subtask Preload Hints FAIL"]
  preload-task = Subtask Hints
  fail = True

[task "Subtask Preload Override Pass"]
  preload-task = Subtask PASS
  pass = False

[task "Subtask Preload Override Fail"]
  preload-task = Subtask FAIL
  fail = False

[task "Subtask Preload Extend Subtasks"]
  preload-task = Subtask READY
  subtask = Subtask APPLICABLE

[task "Subtask Preload Optional"]
  preload-task = Missing | Subtask PASS

[task "Subtask Preload Properties"]
  preload-task = Subtask Properties Hints
  set-fourth-property = fourth-value
  fail-hint = second-property(${second-property}) fourth-property(${fourth-property})

[task "Subtask Hints"] # meant to be preloaded, not a test case in itself
  ready-hint = Task is ready
  fail-hint = Task failed

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
```

`task/invalids.config` file in project `All-Projects` on ref `refs/meta/config`.

```
[task "No PASS criteria"]
  fail-hint = Invalid without Pass criteria and without subtasks

[task "WAITING (subtask INVALID)"]
  pass = is:open
  subtask = Subtask INVALID

[task "WAITING (subtask duplicate)"]
  subtask = Subtask INVALID
  subtask = Subtask INVALID

[task "WAITING (subtask missing)"]
  pass = is:open
  subtask = MISSING # security bug: subtask name appears in output

[task "Grouping WAITING (subtask INVALID)"]
  subtask = Subtask INVALID

[task "Grouping WAITING (subtask missing)"]
  subtask = MISSING  # security bug: subtask name appears in output

[task "Subtask INVALID"]
  fail-hint = Use when an INVALID subtask is needed, not meant as a test case in itself

[task "Subtask Optional"]
   subtask = MISSING | MISSING

[task "NA Bad PASS query"]
  applicable = NOT is:open # Assumes test query is "is:open"
  fail = True
  pass = has:bad

[task "NA Bad FAIL query"]
  applicable = NOT is:open # Assumes test query is "is:open"
  pass = True
  fail = has:bad

[task "NA Bad INPROGRESS query"]
  applicable = NOT is:open # Assumes test query is "is:open"
  fail = True
  in-progress = has:bad

[task "Looping"]
  subtask = Looping

[task "Looping Properties"]
  set-A = ${B}
  set-B = ${A}
  fail = True
```

`task/special.config` file in project `All-Users` on ref `refs/users/self`.

```
[task "userfile task/special.config PASS"]
  applicable = is:open
  pass = is:open

[task "userfile task/special.config FAIL"]
  applicable = is:open
  fail = is:open
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
               "name" : "Root APPLICABLE",
               "status" : "PASS",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask APPLICABLE",
                     "status" : "PASS"
                  }
               ]
            },
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
               "name" : "Root Optional subtasks",
               "status" : "WAITING",
               "subTasks" : [
                  {
                     "hasPass" : false,
                     "name" : "Subtask Optional",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : true,
                           "name" : "Subtask PASS",
                           "status" : "PASS"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask FAIL",
                           "status" : "FAIL"
                        },
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
                  }
               ]
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
               "exported" : {
                  "root" : "Root Properties"
               },
               "hasPass" : true,
               "hint" : "Name(Root Properties)",
               "name" : "Root Properties",
               "status" : "FAIL",
               "subTasks" : [
                  {
                     "exported" : {
                        "subtask" : "Subtask Properties"
                     },
                     "hasPass" : false,
                     "name" : "Subtask Properties",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : true,
                           "hint" : "Name(Subtask Properties Hints) root-property(root-value) first-property(first-value) second-property(first-value second-extra third-value) root(Root Properties)",
                           "name" : "Subtask Properties Hints",
                           "status" : "FAIL"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Chained Subtask Properties",
                           "status" : "PASS"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask Properties Reset",
                           "status" : "PASS"
                        }
                     ]
                  }
               ]
            },
            {
               "hasPass" : true,
               "name" : "Root Preload",
               "status" : "FAIL",
               "subTasks" : [
                  {
                     "hasPass" : true,
                     "name" : "Subtask Preload",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : true,
                           "name" : "Subtask PASS",
                           "status" : "PASS"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask Preload Preload",
                           "status" : "PASS"
                        },
                        {
                           "hasPass" : true,
                           "hint" : "Task is ready",
                           "name" : "Subtask Preload Hints PASS",
                           "status" : "READY"
                        },
                        {
                           "hasPass" : true,
                           "hint" : "Task failed",
                           "name" : "Subtask Preload Hints FAIL",
                           "status" : "FAIL"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask Preload Override Pass",
                           "status" : "READY"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask Preload Override Fail",
                           "status" : "PASS"
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask Preload Extend Subtasks",
                           "status" : "READY",
                           "subTasks" : [
                              {
                                 "hasPass" : true,
                                 "name" : "Subtask PASS",
                                 "status" : "PASS"
                              },
                              {
                                 "hasPass" : true,
                                 "name" : "Subtask APPLICABLE",
                                 "status" : "PASS"
                              }
                           ]
                        },
                        {
                           "hasPass" : true,
                           "name" : "Subtask Preload Optional",
                           "status" : "PASS"
                        },
                        {
                           "hasPass" : true,
                           "hint" : "second-property(first-value second-extra third-value) fourth-property(fourth-value)",
                           "name" : "Subtask Preload Properties",
                           "status" : "FAIL"
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
                     "hasPass" : false,
                     "name" : "WAITING (subtask duplicate)",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "hasPass" : false,
                           "name" : "Subtask INVALID",
                           "status" : "INVALID"
                        },
                        {
                           "name" : "UNKNOWN",
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
                           "name" : "UNKNOWN",
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
                           "name" : "UNKNOWN",
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
                     "name" : "Subtask Optional",
                     "status" : "WAITING",
                     "subTasks" : [
                        {
                           "name" : "UNKNOWN",
                           "status" : "INVALID"
                        }
                     ]
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
                  },
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  }
               ]
            }
         ]
      }
   ],
   ...
```
