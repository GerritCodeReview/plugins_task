# --task-preview root file with subtask pointing to secret user ref

file: `All-Projects.git:refs/meta/config:task.config`
```
 [root "Root Preview SECRET external"]
     applicable = is:open
     pass = True
+    subtask = @{secret_user}/secret.config^SECRET Task
```

file: `All-Users.git:{secret_user_ref}:task/secret.config`
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
   "name" : "Root Preview SECRET external",
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