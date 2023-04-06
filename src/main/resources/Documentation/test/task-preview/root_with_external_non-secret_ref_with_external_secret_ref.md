# --task-preview root file with subtasks-external pointing to a non-secret user ref with subtasks-external pointing to a secret user ref.

file: `All-Projects.git:refs/meta/config:task.config`
```
 [root "Root Preview NON-SECRET external with SECRET external"]
     applicable = "is:open"
     pass = True
+    subtasks-external = NON-SECRET with SECRET External

+[external "NON-SECRET with SECRET External"]
+    user = {non_secret_user}
+    file = secret_external.config
```

file: `All-Users.git:{non_secret_user_ref}:task/secret_external.config`
```
[task "NON-SECRET with SECRET external"]
    applicable = is:open
    pass = True
    subtasks-external = SECRET external

[external "SECRET external"]
    user = {secret_user}
    file = secret.config
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
   "name" : "Root Preview NON-SECRET external with SECRET external",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "NON-SECRET with SECRET external",
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