# --task-preview root file with subtask pointing to a non-secret user ref with subtask pointing to a secret user ref.

file: `All-Projects.git:refs/meta/config:task.config`
```
 [root "Root Preview NON-SECRET subtask with SECRET subtask"]
     applicable = "is:open"
     pass = True
+    subtask = @{non_secret_user}/secret_external.config^NON-SECRET with SECRET subtask
```

file: `All-Users.git:{non_secret_user_ref}:task/secret_external.config`
```
[task "NON-SECRET with SECRET subtask"]
    applicable = is:open
    pass = True
    subtask = @{secret_user}/secret.config^SECRET task
```

file: `All-Users:{secret_user_ref}:task/secret.config`
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
   "name" : "Root Preview NON-SECRET subtask with SECRET subtask",
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