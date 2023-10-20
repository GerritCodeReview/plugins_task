# --task-preview root file with subtask pointing to a non-secret group ref with subtask pointing to a secret group ref.

file: `All-Projects.git:refs/meta/config:task.config`
```
 [root "Root Preview NON-SECRET group subtask with SECRET group subtask"]
     applicable = "is:open"
     pass = True
+    subtask = %{non_secret_group_name}/secret_external.config^NON-SECRET with SECRET subtask
```

file: `All-Users.git:refs/groups/{sharded_non_secret_group_uuid}:task/secret_external.config`
```
[task "NON-SECRET with SECRET subtask"]
    applicable = is:open
    pass = True
    subtask = %{secret_group_name}/secret.config^SECRET task
```

file: `All-Users:refs/groups/{sharded_secret_group_uuid}:task/secret.config`
```
[task "SECRET task"]
    applicable = is:open
    pass = Fail
```

json:
```
{
   "applicable" : true,
   "hasPass" : true,
   "name" : "Root Preview NON-SECRET group subtask with SECRET group subtask",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "NON-SECRET with SECRET subtask",
         "status" : "WAITING",
         "subTasks" : [
            {
               "name" : "UNKNOWN",            # Only Test Suite: non-secret
               "status" : "UNKNOWN"           # Only Test Suite: non-secret
               "applicable" : true,           # Only Test Suite: secret
               "hasPass" : true,              # Only Test Suite: secret
               "name" : "SECRET task",        # Only Test Suite: secret
               "status" : "READY"             # Only Test Suite: secret
            }
         ]
      }
   ]
}
```
