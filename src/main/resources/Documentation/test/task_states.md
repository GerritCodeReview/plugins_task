@PLUGIN@ States
===============

Below are sample config files which illustrate many examples of how task
states are affected by their own criteria and their subtasks' states.
To better help visualize the output that each `root` example leads to,
the json output for the root definition (and any definitions that it
refers to) will be placed inline right after the root. Naturally, this
json is not a part of the config, however it is part of the expected
output for task config when running the following:

```
 $  ssh -x -p 29418 review-example gerrit query is:open \
     --task--all --format json|head -1 |json_pp
```

The config below is expected to be in the `task.config` file in project
`All-Projects` on ref `refs/meta/config`.

```
[root "Root N/A"]
  applicable = is:closed # Assumes test query is "is:open"

{
   "applicable" : false,
   "hasPass" : false,
   "name" : "Root N/A",
   "status" : "INVALID"
}

[root "Root APPLICABLE"]
  applicable = is:open # Assumes test query is "is:open"
  pass = True
  subtask = Subtask APPLICABLE

[task "Subtask APPLICABLE"]
  applicable = is:open
  pass = True

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root APPLICABLE",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask APPLICABLE",
         "status" : "PASS"
      }
   ]
}

[root "Root PASS"]
  pass = True

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root PASS",
   "status" : "PASS"
}

[root "Root FAIL"]
  fail = True

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root FAIL",
   "status" : "FAIL"
}

[root "Root straight PASS"]
  applicable = is:open
  pass = is:open

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root straight PASS",
   "status" : "PASS"
}

[root "Root straight FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root straight FAIL",
   "status" : "FAIL"
}

[root "Root PASS-fail"]
  applicable = is:open
  fail = NOT is:open

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root PASS-fail",
   "status" : "PASS"
}

[root "Root pass-FAIL"]
  applicable = is:open
  fail = is:open

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root pass-FAIL",
   "status" : "FAIL"
}

[root "Root PASS-waiting-fail"]
  applicable = is:open
  fail = NOT is:open
  subtask = Subtask PASS

[task "Subtask PASS"]
  applicable = is:open
  pass = is:open

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root PASS-waiting-fail",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask PASS",
         "status" : "PASS"
      }
   ]
}

[root "Root pass-WAITING-fail"]
  applicable = is:open
  fail = NOT is:open
  subtask = Subtask FAIL

[task "Subtask FAIL"]
  applicable = is:open
  fail = is:open
  pass = is:open

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root pass-WAITING-fail",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Root pass-waiting-FAIL"]
  applicable = is:open
  fail = is:open
  subtask = Subtask PASS

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root pass-waiting-FAIL",
   "status" : "FAIL",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask PASS",
         "status" : "PASS"
      }
   ]
}

[root "Root grouping PASS (subtask PASS)"]
  subtask = Subtask PASS

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root grouping PASS (subtask PASS)",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask PASS",
         "status" : "PASS"
      }
   ]
}

[root "Root grouping WAITING (subtask READY)"]
  subtask = Subtask READY

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root grouping WAITING (subtask READY)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask READY",
         "status" : "READY",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask PASS",
               "status" : "PASS"
            }
         ]
      }
   ]
}

[root "Root grouping WAITING (subtask FAIL)"]
  subtask = Subtask FAIL

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root grouping WAITING (subtask FAIL)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Root grouping NA (subtask NA)"]
  applicable = is:open # Assumes Subtask NA has "applicable = NOT is:open"
  subtask = Subtask NA

[task "Subtask NA"]
  applicable = NOT is:open # Assumes test query is "is:open"

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root grouping NA (subtask NA)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : false,
         "hasPass" : false,
         "name" : "Subtask NA",
         "status" : "INVALID"
      }
   ]
}

[root "Root READY (subtask PASS)"]
  applicable = is:open
  pass = NOT is:open
  subtask = Subtask PASS
  ready-hint = You must now run the ready task

{
   "applicable" : true,
   "hasPass" : true,
   "hint" : "You must now run the ready task",
   "name" : "Root READY (subtask PASS)",
   "status" : "READY",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask PASS",
         "status" : "PASS"
      }
   ]
}

[root "Root WAITING (subtask READY)"]
  applicable = is:open
  pass = is:open
  subtask = Subtask READY

[task "Subtask READY"]
  applicable = is:open
  pass = NOT is:open
  subtask = Subtask PASS

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root WAITING (subtask READY)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask READY",
         "status" : "READY",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask PASS",
               "status" : "PASS"
            }
         ]
      }
   ]
}

[root "Root WAITING (subtask FAIL)"]
  applicable = is:open
  pass = is:open
  subtask = Subtask FAIL

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root WAITING (subtask FAIL)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Root IN PROGRESS"]
   applicable = is:open
   in-progress = is:open
   pass = NOT is:open

{
   "applicable" : true,
   "hasPass" : true,
   "inProgress" : true,
   "name" : "Root IN PROGRESS",
   "status" : "READY"
}

[root "Root NOT IN PROGRESS"]
   applicable = is:open
   in-progress = NOT is:open
   pass = NOT is:open

{
   "applicable" : true,
   "hasPass" : true,
   "inProgress" : false,
   "name" : "Root NOT IN PROGRESS",
   "status" : "READY"
}

[root "Root OPTIONAL MISSING"]
   subtask = OPTIONAL MISSING |
   pass = True

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root OPTIONAL MISSING",
   "status" : "PASS"
}

[root "Root Optional subtask EXISTS"]
   subtask = Subtask Optional EXISTS |

[task "Subtask Optional EXISTS"]
   subtask = Subtask PASS |

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Optional subtask EXISTS",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Optional EXISTS",
         "status" : "PASS",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask PASS",
               "status" : "PASS"
            }
         ]
      }
   ]
}

[root "Root Optional subtask MISSING then EXISTS"]
   subtask = Subtask Optional MISSING then EXISTS |

[task "Subtask Optional MISSING then EXISTS"]
   subtask = OPTIONAL MISSING | Subtask FAIL

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Optional subtask MISSING then EXISTS",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Optional MISSING then EXISTS",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask FAIL",
               "status" : "FAIL"
            }
         ]
      }
   ]
}

[root "Root Optional subtask MISSING then MISSING"]
   subtask = Subtask Optional MISSING then MISSING |

[task "Subtask Optional MISSING then MISSING"]
   subtask = OPTIONAL MISSING | OPTIONAL MISSING |

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Optional subtask MISSING then MISSING",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Optional MISSING then MISSING"
      }
   ]
}

[root "Root Optional subtask MISSING then MISSING then EXISTS"]
   subtask = Subtask Optional MISSING then MISSING then EXISTS |

[task "Subtask Optional MISSING then MISSING then EXISTS"]
   subtask = OPTIONAL MISSING | OPTIONAL MISSING | Subtask READY

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Optional subtask MISSING then MISSING then EXISTS",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Optional MISSING then MISSING then EXISTS",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask READY",
               "status" : "READY",
               "subTasks" : [
                  {
                     "applicable" : true,
                     "hasPass" : true,
                     "name" : "Subtask PASS",
                     "status" : "PASS"
                  }
               ]
            }
         ]
      }
   ]
}

[root "Subtasks File"]
  subtasks-file = common.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks File",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config PASS",
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Subtasks File (Missing)"]
  subtasks-file = common.config
  subtasks-file = missing

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks File (Missing)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config PASS",
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Subtasks External"]
  subtasks-external = user special

[external "user special"]
  user = testuser
  file = special.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks External",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config PASS",
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Subtasks External (Missing)"]
  subtasks-external = user special
  subtasks-external = missing

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks External (Missing)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config PASS",
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config FAIL",
         "status" : "FAIL"
      },
      {
         "name" : "UNKNOWN",
         "status" : "INVALID"
      }
   ]
}

[root "Subtasks External (User Missing)"]
  subtasks-external = user special
  subtasks-external = user missing

[external "user missing"]
  user = missing
  file = special.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks External (User Missing)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config PASS",
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config FAIL",
         "status" : "FAIL"
      },
      {
         "name" : "UNKNOWN",
         "status" : "INVALID"
      }
   ]
}

[root "Subtasks External (File Missing)"]
  subtasks-external = user special
  subtasks-external = file missing

[external "file missing"]
  user = testuser
  file = missing

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks External (File Missing)",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config PASS",
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "userfile task/special.config FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Root tasks-factory STATIC"]
  subtasks-factory = tasks-factory static

[tasks-factory "tasks-factory static"]
  names-factory = names-factory static list
  fail = True

[names-factory "names-factory static list"]
  type = static
  name = my a task
  name = my b task
  name = my c task

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root tasks-factory STATIC",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "my a task",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "my b task",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "my c task",
         "status" : "FAIL"
      }
   ]
}

[root "Root tasks-factory CHANGE"]
  subtasks-factory = tasks-factory change

[tasks-factory "tasks-factory change"]
  names-factory = names-factory change list
  fail = True

[names-factory "names-factory change list"]
  changes = change:_change1_number OR change:_change2_number
  type = change

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root tasks-factory CHANGE",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "_change1_number",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "_change2_number",
         "status" : "FAIL"
      }
   ]
}

[root "Root tasks-factory static (empty name)"]
  subtasks-factory = tasks-factory static (empty name)
  # Grouping task since it has no pass criteria, not output since it has no subtasks

[tasks-factory "tasks-factory static (empty name)"]
  names-factory = names-factory static (empty name list)
  fail = True

[names-factory "names-factory static (empty name list)"]
  type = static

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root tasks-factory static (empty name)"
}

[root "Root tasks-factory static (empty name PASS)"]
  pass = True
  subtasks-factory = tasks-factory static (empty name)

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root tasks-factory static (empty name PASS)",
   "status" : "PASS"
}

[root "Root Same Name - Different Tasks-Factory"]
  subtasks-factory = parent tasks-factory Same Name - Different Tasks-Factory

[tasks-factory "parent tasks-factory Same Name - Different Tasks-Factory"]
  names-factory = parent names-factory Same Name - Different Tasks-Factory
  fail = True
  subtasks-factory = child tasks-factory Same Name - Different Tasks-Factory

[names-factory "parent names-factory Same Name - Different Tasks-Factory"]
  type = static
  name = Same Name

[tasks-factory "child tasks-factory Same Name - Different Tasks-Factory"]
  names-factory = child names-factory Same Name - Different Tasks-Factory
  fail = False

[names-factory "child names-factory Same Name - Different Tasks-Factory"]
  type = static
  name = Same Name

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Same Name - Different Tasks-Factory",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Same Name",
         "status" : "FAIL",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Same Name",
               "status" : "PASS"
            }
         ]
      }
   ]
}

[root "Root Property References"]
  set-first-property = first-value
  set-backward-reference = first-[${first-property}]
  set-forward-reference = last-[${last-property}]
  set-last-property = last-value
  fail = True
  fail-hint = backward-reference(${backward-reference}) forward-reference(${forward-reference})

{
   "applicable" : true,
   "hasPass" : true,
   "hint" : "backward-reference(first-[first-value]) forward-reference(last-[last-value])",
   "name" : "Root Property References",
   "status" : "FAIL"
}

[root "Root Deep Property References"]
  set-first-property = first-value
  set-direct-reference = first-[${first-property}]
  set-deep-reference = deep-{${direct-reference}}
  fail = True
  fail-hint = deep-reference(${deep-reference})

{
   "applicable" : true,
   "hasPass" : true,
   "hint" : "deep-reference(deep-{first-[first-value]})",
   "name" : "Root Deep Property References",
   "status" : "FAIL"
}

[root "Root Properties Referenced Twice"]
  set-first-property = first-value
  set-referenced-twice = first-[${first-property}] first-[${first-property}]
  fail = True
  fail-hint = first-[${first-property}] referenced-twice(${referenced-twice}) referenced-twice(${referenced-twice})

{
   "applicable" : true,
   "hasPass" : true,
   "hint" : "first-[first-value] referenced-twice(first-[first-value] first-[first-value]) referenced-twice(first-[first-value] first-[first-value])",
   "name" : "Root Properties Referenced Twice",
   "status" : "FAIL"
}

[root "Root Inherited Properties"]
  set-root-property = root-value
  subtask = Subtask Parent Inherited Properties

[task "Subtask Parent Inherited Properties"]
  set-parent-property = parent-value
  subtask = Subtask Inherited Properties

[task "Subtask Inherited Properties"]
  set-my-property = my-value
  fail = True
  fail-hint = root-property(${root-property}) parent-property(${parent-property}) my-property(${my-property})

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Inherited Properties",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Parent Inherited Properties",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "hint" : "root-property(root-value) parent-property(parent-value) my-property(my-value)",
               "name" : "Subtask Inherited Properties",
               "status" : "FAIL"
            }
         ]
      }
   ]
}

[root "Root Inherited Distant Properties"]
  set-root-property = root-value
  set-root-change-property = ${_change_number}
  subtask = Subtask Parent Inherited Distant Properties

[task "Subtask Parent Inherited Distant Properties"]
  subtask = Subtask Inherited Distant Properties

[task "Subtask Inherited Distant Properties"]
  fail = True
  fail-hint = root-property(${root-property}) root-change-property(${root-change-property})

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Inherited Distant Properties",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Parent Inherited Distant Properties",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "hint" : "root-property(root-value) root-change-property(_change_number)",
               "name" : "Subtask Inherited Distant Properties",
               "status" : "FAIL"
            }
         ]
      }
   ]
}

[root "Root Properties Reset By Subtask"]
  set-root-to-reset-by-subtask = reset-my-root-value
  subtask = Subtask Properties Reset

[task "Subtask Properties Reset"]
  fail = True
  set-root-to-reset-by-subtask = reset-by-subtask-root-value
  fail-hint = root-to-reset-by-subtask:(${root-to-reset-by-subtask})

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Properties Reset By Subtask",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "root-to-reset-by-subtask:(reset-by-subtask-root-value)",
         "name" : "Subtask Properties Reset",
         "status" : "FAIL"
      }
   ]
}

[root "Root Inherited Property References"]
  set-root-property = root-value
  subtask = Subtask Parent Inherited Property References

[task "Subtask Parent Inherited Property References"]
  set-parent-property = parent-value
  set-parent-inherited-root-reference = root-property(${root-property})
  subtask = Subtask Inherited Property References

[task "Subtask Inherited Property References"]
  set-inherited-root-reference = root-[${root-property}]
  set-inherited-parent-reference = parent-[${parent-property}]
  set-inherited-root-deep-reference = parent-inherited-root-reference-[${parent-inherited-root-reference}]
  fail = True
  fail-hint = inherited-root-reference(${inherited-root-reference}) inherited-parent-reference(${inherited-parent-reference}) inherited-root-deep-reference(${inherited-root-deep-reference})

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Inherited Property References",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Parent Inherited Property References",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "hint" : "inherited-root-reference(root-[root-value]) inherited-parent-reference(parent-[parent-value]) inherited-root-deep-reference(parent-inherited-root-reference-[root-property(root-value)])",
               "name" : "Subtask Inherited Property References",
               "status" : "FAIL"
            }
         ]
      }
   ]
}

[root "Root Properties Exports"]
  export-root-exported = ${_name}
  subtask = Subtask Properties Exports
  fail = True
  fail-hint = root-exported(${root-exported})

[task "Subtask Properties Exports"]
  export-subtask-exported = ${_name}
  fail = True
  fail-hint = root-exported(${root-exported}) subtask-exported(${subtask-exported})

{
   "applicable" : true,
   "exported" : {
      "root-exported" : "Root Properties Exports"
   },
   "hasPass" : true,
   "hint" : "root-exported(Root Properties Exports)",
   "name" : "Root Properties Exports",
   "status" : "FAIL",
   "subTasks" : [
      {
         "applicable" : true,
         "exported" : {
            "subtask-exported" : "Subtask Properties Exports"
         },
         "hasPass" : true,
         "hint" : "root-exported(Root Properties Exports) subtask-exported(Subtask Properties Exports)",
         "name" : "Subtask Properties Exports",
         "status" : "FAIL"
      }
   ]
}

[root "Root Internal Properties"]
  export-root = Name(${_name}) Change Number(${_change_number}) Change Id(${_change_id}) Change Project(${_change_project}) Change Branch(${_change_branch}) Change Status(${_change_status}) Change Topic(${_change_topic})
  set-root-internals = Name(${_name}) Change Number(${_change_number}) Change Id(${_change_id}) Change Project(${_change_project}) Change Branch(${_change_branch}) Change Status(${_change_status}) Change Topic(${_change_topic})
  fail = True
  fail-hint = Name(${_name}) Change Number(${_change_number}) Change Id(${_change_id}) Change Project(${_change_project}) Change Branch(${_change_branch}) Change Status(${_change_status}) Change Topic(${_change_topic})
  subtask = Subtask Internal Properties

[task "Subtask Internal Properties"]
  fail = True
  fail-hint = root-internals(${root-internals}) Name(${_name}) Change Number(${_change_number}) Change Id(${_change_id}) Change Project(${_change_project}) Change Branch(${_change_branch}) Change Status(${_change_status}) Change Topic(${_change_topic})

{
   "applicable" : true,
   "exported" : {
      "root" : "Name(Root Internal Properties) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)"
   },
   "hasPass" : true,
   "hint" : "Name(Root Internal Properties) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)",
   "name" : "Root Internal Properties",
   "status" : "FAIL",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "root-internals(Name(Root Internal Properties) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)) Name(Subtask Internal Properties) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)",
         "name" : "Subtask Internal Properties",
         "status" : "FAIL"
      }
   ]
}

[root "Root Subtask Via Property"]
  set-subtask = Subtask
  subtask = ${subtask} Via Property

[task "Subtask Via Property"]
  subtask = Second ${_name}

[task "Second Subtask Via Property"]
  fail = True

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Subtask Via Property",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask Via Property",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Second Subtask Via Property",
               "status" : "FAIL"
            }
         ]
      }
   ]
}

[root "Root Properties tasks-factory STATIC"]
  subtasks-factory = tasks-factory STATIC Properties

[tasks-factory "tasks-factory STATIC Properties"]
  set-welcome-message = Welcome to the jungle
  names-factory = names-factory static list
  fail-hint = ${welcome-message} Name(${_name}) Change Number(${_change_number}) Change Id(${_change_id}) Change Project(${_change_project}) Change Branch(${_change_branch}) Change Status(${_change_status}) Change Topic(${_change_topic})
  fail = True

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Properties tasks-factory STATIC",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "Welcome to the jungle Name(my a task) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)",
         "name" : "my a task",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "Welcome to the jungle Name(my b task) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)",
         "name" : "my b task",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "Welcome to the jungle Name(my c task) Change Number(_change_number) Change Id(_change_id) Change Project(_change_project) Change Branch(_change_branch) Change Status(_change_status) Change Topic(_change_topic)",
         "name" : "my c task",
         "status" : "FAIL"
      }
   ]
}

[root "Root Properties tasks-factory CHANGE"]
  subtasks-factory = tasks-factory CHANGE Properties

[tasks-factory "tasks-factory CHANGE Properties"]
  set-welcome-message = Welcome to the pleasuredome
  names-factory = names-factory a change
  fail-hint = ${welcome-message} Name(${_name}) Change Number(${_change_number}) Change Id(${_change_id}) Change Project(${_change_project}) Change Branch(${_change_branch}) Change Status(${_change_status}) Change Topic(${_change_topic})
  fail = change:_change1_number

[names-factory "names-factory a change"]
  type = change
  changes = change:_change1_number OR change:_change2_number

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Properties tasks-factory CHANGE",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "Welcome to the pleasuredome Name(_change1_number) Change Number(_change1_number) Change Id(_change1_id) Change Project(_change1_project) Change Branch(_change1_branch) Change Status(_change1_status) Change Topic(_change1_topic)",
         "name" : "_change1_number",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "_change2_number",
         "status" : "PASS"
      }
   ]
}

[root "Root tasks-factory _name Property Reference"]
  subtasks-factory = Properties tasks-factory _name Property Reference

[tasks-factory "Properties tasks-factory _name Property Reference"]
  set-name-reference = first-property ${_name}
  fail-hint = ${name-reference}
  fail = true
  names-factory = names-factory static list

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root tasks-factory _name Property Reference",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "first-property my a task",
         "name" : "my a task",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "first-property my b task",
         "name" : "my b task",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "first-property my c task",
         "name" : "my c task",
         "status" : "FAIL"
      }
   ]
}

[root "Root Properties names-factory STATIC"]
  subtasks-factory = tasks-factory Properties names-factory STATIC

[tasks-factory "tasks-factory Properties names-factory STATIC"]
  names-factory = Properties names-factory STATIC
  fail = True

[names-factory "Properties names-factory STATIC"]
  type = static
  name = Change Number(${_change_number})
  name = Change Id(${_change_id})
  name = Change Project(${_change_project})
  name = Change Branch(${_change_branch})
  name = Change Status(${_change_status})
  name = Change Topic(${_change_topic})

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Properties names-factory STATIC",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Change Number(_change_number)",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Change Id(_change_id)",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Change Project(_change_project)",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Change Branch(_change_branch)",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Change Status(_change_status)",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Change Topic(_change_topic)",
         "status" : "FAIL"
      }
   ]
}

[root "Root Properties names-factory CHANGE"]
  subtasks-factory = tasks-factory Properties names-factory CHANGE

[tasks-factory "tasks-factory Properties names-factory CHANGE"]
  names-factory = Properties names-factory CHANGE
  fail = True

[names-factory "Properties names-factory CHANGE"]
  type = change
  changes = change:_change1_number OR change:${_change_number} project:${_change_project} branch:${_change_branch}

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Properties names-factory CHANGE",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "_change_number",
         "status" : "FAIL"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "_change1_number",
         "status" : "FAIL"
      }
   ]
}

[root "Root Preload"]
   preload-task = Subtask FAIL
   subtask = Subtask Preload

[task "Subtask Preload"]
  preload-task = Subtask READY

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root Preload",
   "status" : "FAIL",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Preload",
         "status" : "READY",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask PASS",
               "status" : "PASS"
            }
         ]
      }
   ]
}

[root "Root Preload Preload"]
  subtask = Subtask Preload Preload

[task "Subtask Preload Preload"]
  preload-task = Subtask Preload with Preload

[task "Subtask Preload with Preload"]
  preload-task = Subtask PASS

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Preload Preload",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Preload Preload",
         "status" : "PASS"
      }
   ]
}

[root "Root Preload Hints PASS"]
  subtask = Subtask Preload Hints PASS

[task "Subtask Preload Hints PASS"]
  preload-task = Subtask Hints
  pass = False

[task "Subtask Hints"] # meant to be preloaded, not a test case in itself
  ready-hint = Task is ready
  fail-hint = Task failed

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Preload Hints PASS",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "Task is ready",
         "name" : "Subtask Preload Hints PASS",
         "status" : "READY"
      }
   ]
}

[root "Root Preload Hints FAIL"]
  subtask = Subtask Preload Hints FAIL

[task "Subtask Preload Hints FAIL"]
  preload-task = Subtask Hints
  fail = True

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Preload Hints FAIL",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "Task failed",
         "name" : "Subtask Preload Hints FAIL",
         "status" : "FAIL"
      }
   ]
}

[root "Root Override Preload Pass"]
  subtask = Subtask Override Preload Pass

[task "Subtask Override Preload Pass"]
  preload-task = Subtask PASS
  pass = False

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Override Preload Pass",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Override Preload Pass",
         "status" : "READY"
      }
   ]
}

[root "Root Override Preload Fail"]
  subtask = Subtask Override Preload Fail

[task "Subtask Override Preload Fail"]
  preload-task = Subtask FAIL
  fail = False

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Override Preload Fail",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Override Preload Fail",
         "status" : "PASS"
      }
   ]
}

[root "Root Append Preloaded Subtasks"]
  subtask = Subtask Append Preloaded Subtasks

[task "Subtask Append Preloaded Subtasks"]
  preload-task = Subtask READY
  subtask = Subtask APPLICABLE

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Append Preloaded Subtasks",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Append Preloaded Subtasks",
         "status" : "READY",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask PASS",
               "status" : "PASS"
            },
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Subtask APPLICABLE",
               "status" : "PASS"
            }
         ]
      }
   ]
}

[root "Root Preload Optional"]
  subtask = Subtask Preload Optional
[task "Subtask Preload Optional"]
  preload-task = Missing | Subtask PASS

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Preload Optional",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Preload Optional",
         "status" : "PASS"
      }
   ]
}

[root "Root Preload Properties"]
  subtask = Subtask Preload Properties

[task "Subtask Preload Properties"]
  preload-task = Subtask Preload Properties Hints
  set-fourth-property = fourth-value
  fail-hint = second-property(${second-property}) fourth-property(${fourth-property})

[task "Subtask Preload Properties Hints"]
  set-first-property = first-value
  set-second-property = ${first-property} second-extra ${third-property}
  set-third-property = third-value
  fail = True
  fail-hint = root-property(${root-property}) first-property(${first-property}) second-property(${second-property})

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Preload Properties",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "hint" : "second-property(first-value second-extra third-value) fourth-property(fourth-value)",
         "name" : "Subtask Preload Properties",
         "status" : "FAIL"
      }
   ]
}

[root "Root INVALID Preload"]
  preload-task = missing

{
   "name" : "UNKNOWN",
   "status" : "INVALID"
}

[root "INVALIDS"]
  subtasks-file = invalids.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "INVALIDS",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "No PASS criteria",
         "status" : "INVALID"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "WAITING (subtask INVALID)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : false,
               "name" : "Subtask INVALID",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "WAITING (subtask duplicate)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
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
         "applicable" : true,
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
         "applicable" : true,
         "hasPass" : false,
         "name" : "Grouping WAITING (subtask INVALID)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : false,
               "name" : "Subtask INVALID",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
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
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask INVALID",
         "status" : "INVALID"
      },
      {
         "applicable" : true,
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
         "applicable" : false,
         "hasPass" : true,
         "name" : "NA Bad PASS query",
         "status" : "FAIL"      # Only Test Suite: all
         "status" : "INVALID"   # Only Test Suite: !all
      },
      {
         "applicable" : false,
         "hasPass" : true,
         "name" : "NA Bad FAIL query",
         "status" : "INVALID"
      },
      {
         "applicable" : false,
         "hasPass" : true,
         "name" : "NA Bad INPROGRESS query",
         "status" : "FAIL"      # Only Test Suite: all
         "status" : "INVALID"   # Only Test Suite: !all
      },
      {
         "applicable" : true,
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
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (tasks-factory missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory type missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory type INVALID)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory duplicate)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "duplicate",
               "status" : "FAIL"
            },
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory changes type missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory changes missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory changes invalid)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (tasks-factory changes loop)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "_change1_number",
               "status" : "FAIL",
               "subTasks" : [
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "_change2_number",
               "status" : "FAIL",
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

[root "Root NA Pass"]
  applicable = NOT is:open # Assumes test query is "is:open"
  pass = True

{
   "applicable" : false,
   "hasPass" : true,
   "name" : "Root NA Pass",
   "status" : "PASS"
}

[root "Root NA Fail"]
  applicable = NOT is:open # Assumes test query is "is:open"
  fail = True

{
   "applicable" : false,
   "hasPass" : true,
   "name" : "Root NA Fail",
   "status" : "FAIL"
}

[root "NA INVALIDS"]
  applicable = NOT is:open # Assumes test query is "is:open"
  subtasks-file = invalids.config

{
   "applicable" : false,
   "hasPass" : false,
   "name" : "NA INVALIDS",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "No PASS criteria",
         "status" : "INVALID"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "WAITING (subtask INVALID)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : false,
               "name" : "Subtask INVALID",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "WAITING (subtask duplicate)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
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
         "applicable" : true,
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
         "applicable" : true,
         "hasPass" : false,
         "name" : "Grouping WAITING (subtask INVALID)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : false,
               "name" : "Subtask INVALID",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
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
         "applicable" : true,
         "hasPass" : false,
         "name" : "Subtask INVALID",
         "status" : "INVALID"
      },
      {
         "applicable" : true,
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
         "applicable" : false,
         "hasPass" : true,
         "name" : "NA Bad PASS query",
         "status" : "FAIL"      # Only Test Suite: all
         "status" : "INVALID"   # Only Test Suite: !all
      },
      {
         "applicable" : false,
         "hasPass" : true,
         "name" : "NA Bad FAIL query",
         "status" : "INVALID"
      },
      {
         "applicable" : false,
         "hasPass" : true,
         "name" : "NA Bad INPROGRESS query",
         "status" : "FAIL"      # Only Test Suite: all
         "status" : "INVALID"   # Only Test Suite: !all
      },
      {
         "applicable" : true,
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
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (tasks-factory missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory type missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory type INVALID)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory duplicate)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "duplicate",
               "status" : "FAIL"
            },
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory changes type missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory changes missing)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (names-factory changes invalid)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",
               "status" : "INVALID"
            }
         ]
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "task (tasks-factory changes loop)",
         "status" : "WAITING",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "_change1_number",
               "status" : "FAIL",
               "subTasks" : [
                  {
                     "name" : "UNKNOWN",
                     "status" : "INVALID"
                  }
               ]
            },
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "_change2_number",
               "status" : "FAIL",
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
  subtask = MISSING # security bug: subtask name appears in output

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

[task "task (tasks-factory missing)"]
  subtasks-factory = missing

[task "task (names-factory type missing)"]
  subtasks-factory = tasks-factory (names-factory type missing)

[task "task (names-factory type INVALID)"]
  subtasks-factory = tasks-factory (names-factory type INVALID)

[task "task (names-factory duplicate)"]
  subtasks-factory = tasks-factory (names-factory duplicate)

[task "task (names-factory changes type missing)"]
  subtasks-factory = tasks-factory change (names-factory type missing)

[task "task (names-factory changes missing)"]
  subtasks-factory = tasks-factory change (names-factory changes missing)

[task "task (names-factory changes invalid)"]
  subtasks-factory = tasks-factory change (names-factory changes invalid)

[task "task (tasks-factory changes loop)"]
  subtasks-factory = tasks-factory change loop

[tasks-factory "tasks-factory (names-factory type missing)"]
  names-factory = names-factory (type missing)
  fail = True

[tasks-factory "tasks-factory (names-factory type INVALID)"]
  names-factory = name-factory (type INVALID)

[tasks-factory "tasks-factory (names-factory duplicate)"]
  names-factory = names-factory duplicate
  fail = True

[tasks-factory "tasks-factory change (names-factory type missing)"]
  names-factory = names-factory change list (type missing)
  fail = True

[tasks-factory "tasks-factory change (names-factory changes missing)"]
  names-factory = names-factory change list (changes missing)
  fail = True

[tasks-factory "tasks-factory change (names-factory changes invalid)"]
  names-factory = names-factory change list (changes invalid)
  fail = True

[tasks-factory "tasks-factory change loop"]
  names-factory = names-factory change constant
  subtask = task (tasks-factory changes loop)
  fail = True

[names-factory "names-factory (type missing)"]
  name = no type test

[names-factory "names-factory change list (type missing)"]
  changes = change:_change1_number OR change:_change2_number

[names-factory "names-factory (type INVALID)"]
  name = invalid type test
  type = invalid

[names-factory "names-factory duplicate"]
  name = duplicate
  name = duplicate
  type = static

[names-factory "names-factory change list (changes missing)"]
  type = change

[names-factory "names-factory change list (changes invalid)"]
  change = change:invalidChange
  type = change

[names-factory "names-factory change constant"]
  changes = change:_change1_number OR change:_change2_number
  type = change

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
