# --task-preview a non-secret user ref with subtasks-external pointing to secret user ref.

file: `All-Projects.git:refs/meta/config:task.config`
```
[root "Root for NON-SECRET external Preview with SECRET external"]
    applicable = "is:open"
    pass = True
    subtasks-external = NON-SECRET

[external "NON-SECRET"]
    user = {non_secret_user}
    file = sample.config
```

file: `All-Users:{non_secret_user_ref}:task/sample.config`
```
 [task "NON-SECRET task"]
     applicable = is:open
     pass = Fail
+    subtasks-external = SECRET

+[external "SECRET"]
+    user = {secret_user}
+    file = secret.config
```

file: `All-Users.git:{secret_user_ref}:task/secret.config`
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
   "name" : "Root for NON-SECRET external Preview with SECRET external",
   "status" : "WAITING",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "NON-SECRET task",
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