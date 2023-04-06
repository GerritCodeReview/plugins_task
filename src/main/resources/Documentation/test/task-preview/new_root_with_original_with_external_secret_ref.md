# --task-preview a new root, original root with subtasks-external pointing to secret user ref.

file: `All-Projects.git:refs/meta/config:task.config`
```
 [root "Root with SECRET external"]
     applicable = is:open
     subtasks-external = SECRET

 [external "SECRET"]
     user = {secret_user}
     file = secret.config
+
+[root "Root Preview Simple"]
+    subtask = simple task

+[task "simple task"]
+    applicable = is:open
+    pass = True
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
   "hasPass" : false,
   "name" : "Root with SECRET external",
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
{
   "applicable" : true,
   "hasPass" : false,
   "name" : "Root Preview Simple",
   "status" : "PASS",
   "subTasks" : [
      {
         "applicable" : true,
         "hasPass" : true,
         "name" : "simple task",
         "status" : "PASS"
      }
   ]
}
```