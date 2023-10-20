# --task-preview root file with subtask pointing to secret group ref

file: `All-Projects.git:refs/meta/config:task.config`
```
 [root "Root Preview SECRET external group"]
     applicable = is:open
     pass = True
+    subtask = %{secret_group_name}/secret.config^SECRET Task
```

file: `All-Users.git:refs/groups/{sharded_secret_group_uuid}:task/secret.config`
```
[task "SECRET Task"]
    applicable = is:open
    pass = Fail
```

json:
```
{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root Preview SECRET external group",
   "status" : "WAITING",
   "subTasks" : [
      {
         "name" : "UNKNOWN",                  # Only Test Suite: non-secret
         "status" : "UNKNOWN"                 # Only Test Suite: non-secret
         "applicable" : true,                 # Only Test Suite: secret
         "hasPass" : true,                    # Only Test Suite: secret
         "name" : "SECRET Task",              # Only Test Suite: secret
         "status" : "READY"                   # Only Test Suite: secret
      }
   ]
}
```
