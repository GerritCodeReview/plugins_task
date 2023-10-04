# --task-preview non-root file with subtask pointing root task

file: `All-Projects.git:refs/meta/config:task.config`
```
[root "Points to subFile task with rootFile task preview"]
    applicable = is:open
    pass = is:true_task
    subtask = foo/bar/baz.config^Preview pointing to rootFile task

[task "Task in rootFile"]
    applicable = is:open
    pass = is:true_task
```

file: `All-Projects.git:refs/meta/config:task/foo/bar/baz.config`
```
 [task "Preview pointing to rootFile task"]
     applicable = is:open
     pass = Fail
+    subtask = ^Task in rootFile
```

json:
```
{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Points to subFile task with rootFile task preview",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "Preview pointing to rootFile task",
         "status" : "READY",
         "subTasks" : [
            {
               "applicable" : true,
               "hasPass" : true,
               "name" : "Task in rootFile",
               "status" : "PASS"
            }
         ]
      }
   ]
}
```