The config below is expected to be in the `task.config` file in project
`{root-cfg-prj}` on ref `{root-cfg-branch}`.

file: `{root-cfg-prj}:{root-cfg-branch}:task.config`
```
[root "Root Task PATHS"]
  subtask = subtask pass

[task "subtask pass"]
  applicable = is:open
  pass = is:open

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Task PATHS",
   "path" : {
      "ref" : "{root-cfg-branch}",
      "file" : "task.config",
      "name" : "Root Task PATHS",
      "project" : "{root-cfg-prj}",
      "type" : "root"
   },
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "subtask pass",
         "path" : {
            "ref" : "{root-cfg-branch}",
            "file" : "task.config",
            "name" : "subtask pass",
            "project" : "{root-cfg-prj}",
            "type" : "task"
         },
         "status" : "PASS"
      }
   ]
}

[root "Root other FILE"]
  applicable = is:open
  subtasks-file = common.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root other FILE",
   "path" : {
      "ref" : "{root-cfg-branch}",
      "file" : "task.config",
      "name" : "Root other FILE",
      "project" : "{root-cfg-prj}",
      "type" : "root"
   },
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config PASS",
         "path" : {
            "ref" : "{root-cfg-branch}",
            "file" : "task/common.config",
            "name" : "file task/common.config PASS",
            "project" : "{root-cfg-prj}",
            "type" : "task"
         },
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config FAIL",
         "path" : {
            "ref" : "{root-cfg-branch}",
            "file" : "task/common.config",
            "name" : "file task/common.config FAIL",
            "project" : "{root-cfg-prj}",
            "type" : "task"
         },
         "status" : "FAIL"
      }
   ]
}

[root "Root tasks-factory"]
  subtasks-factory = tasks-factory example

[tasks-factory "tasks-factory example"]
  names-factory = names-factory example list

[names-factory "names-factory example list"]
  type = static
  name = my a task
  name = my b task

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root tasks-factory",
   "path" : {
      "ref" : "{root-cfg-branch}",
      "file" : "task.config",
      "name" : "Root tasks-factory",
      "project" : "{root-cfg-prj}",
      "type" : "root"
   },
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "my a task",
         "path" : {
            "ref" : "{root-cfg-branch}",
            "file" : "task.config",
            "name" : "my a task",
            "project" : "{root-cfg-prj}",
            "tasksFactory" : "tasks-factory example",
            "type" : "tasks-factory"
         },
         "status" : "INVALID"
      },
      {
         "applicable" : true,
         "hasPass" : false,
         "name" : "my b task",
         "path" : {
            "ref" : "{root-cfg-branch}",
            "file" : "task.config",
            "name" : "my b task",
            "project" : "{root-cfg-prj}",
            "tasksFactory" : "tasks-factory example",
            "type" : "tasks-factory"
         },
         "status" : "INVALID"
      }
   ]
}

[root "Root other PROJECT"]
  subtasks-external = user ref

[external "user ref"]
  user = testuser
  file = common.config

{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root other PROJECT",
   "path" : {
      "ref" : "{root-cfg-branch}",
      "file" : "task.config",
      "name" : "Root other PROJECT",
      "project" : "{root-cfg-prj}",
      "type" : "root"
   },
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config PASS",
         "path" : {
            "ref" : "{testuser_user_ref}",
            "file" : "task/common.config",
            "name" : "file task/common.config PASS",
            "project" : "All-Users",
            "user" : "testuser",
            "type" : "task"
         },
         "status" : "PASS"
      },
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "file task/common.config FAIL",
         "path" : {
            "ref" : "{testuser_user_ref}",
            "file" : "task/common.config",
            "name" : "file task/common.config FAIL",
            "project" : "All-Users",
            "user" : "testuser",
            "type" : "task"
         },
         "status" : "FAIL"
      }
   ]
}
```

The config below is expected to be in the `task.config` file in project
`{root-cfg-prj}` on ref `{root-cfg-branch}`.

file: `{root-cfg-prj}:{root-cfg-branch}:task.config`
```
[root "Root Capability Error"]
    applicable = is:open
    pass = true

{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root Capability Error",
   "path" : {
      "error" : "Can't perform operation, need viewTaskPaths capability"
   },
   "status" : "PASS"
}
```
