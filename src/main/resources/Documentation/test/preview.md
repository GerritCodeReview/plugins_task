[root "INVALIDS Preview"]
  subtasks-file = invalids.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "INVALIDS Preview",
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
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Blank",
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
         "status" : "FAIL"      # Only Test Suite: !invalid
         "status" : "INVALID"   # Only Test Suite: invalid
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
         "status" : "FAIL"      # Only Test Suite: !invalid
         "status" : "INVALID"   # Only Test Suite: invalid
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
         "name" : "task (tasks-factory static INVALID)",
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
         "name" : "task (tasks-factory change INVALID)",
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
         "name" : "task (names-factory name Blank)",
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
      }
   ]
}

[root "Root PASS Preview"]
  pass = True

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root PASS Preview",
   "status" : "PASS"
}

[root "Root READY (subtask PASS) Preview"]
  applicable = is:open
  pass = NOT is:open
  subtask = Subtask PASS Preview
  ready-hint = You must now run the ready task

[task "Subtask PASS Preview"]
  applicable = is:open
  pass = is:open

{
   "applicable" : true,
   "hasPass" : true,
   "hint" : "You must now run the ready task",
   "name" : "Root READY (subtask PASS) Preview",
   "status" : "READY",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask PASS Preview",
         "status" : "PASS"
      }
   ]
}

[root "Subtasks External Preview"]
  subtasks-external = user special Preview

[external "user special Preview"]
  user = testuser
  file = special.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Subtasks External Preview",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,                              # Only Test Suite: !untrusted
         "hasPass" : true,                                 # Only Test Suite: !untrusted
         "name" : "userfile task/special.config PASS",     # Only Test Suite: !untrusted
         "status" : "PASS"                                 # Only Test Suite: !untrusted
         "name" : "UNKNOWN",                               # Only Test Suite: untrusted
         "status" : "UNKNOWN"                              # Only Test Suite: untrusted
      },
      {
         "applicable" : true,                              # Only Test Suite: !untrusted
         "hasPass" : true,                                 # Only Test Suite: !untrusted
         "name" : "userfile task/special.config FAIL",     # Only Test Suite: !untrusted
         "status" : "FAIL"                                 # Only Test Suite: !untrusted
         "name" : "UNKNOWN",                               # Only Test Suite: untrusted
         "status" : "UNKNOWN"                              # Only Test Suite: untrusted
      },
      {
         "applicable" : true,                              # Only Test Suite: !untrusted
         "hasPass" : true,                                 # Only Test Suite: !untrusted
         "name" : "file task/common.config Preload PASS",  # Only Test Suite: !untrusted
         "status" : "PASS"                                 # Only Test Suite: !untrusted
         "name" : "UNKNOWN",                               # Only Test Suite: untrusted
         "status" : "UNKNOWN"                              # Only Test Suite: untrusted
      }
   ]
}

[root "Root NA Pass Preview"]
  applicable = NOT is:open
  pass = True

{
   "applicable" : false,
   "hasPass" : true,
   "name" : "Root NA Pass Preview",
   "status" : "PASS"
}

[root "NA INVALIDS Preview"]
  applicable = NOT is:open
  subtasks-file = invalids.config

{
   "applicable" : false,
   "hasPass" : false,
   "name" : "NA INVALIDS Preview",
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
         "applicable" : true,
         "hasPass" : true,
         "name" : "Subtask Blank",
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
         "status" : "FAIL"      # Only Test Suite: !invalid
         "status" : "INVALID"   # Only Test Suite: invalid
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
         "status" : "FAIL"      # Only Test Suite: !invalid
         "status" : "INVALID"   # Only Test Suite: invalid
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
         "name" : "task (tasks-factory static INVALID)",
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
         "name" : "task (tasks-factory change INVALID)",
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
         "name" : "task (names-factory name Blank)",
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
      }
   ]
}
