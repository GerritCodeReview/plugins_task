<a id="task_expression"/>
Task Expression
--------------

The tasks in subtask and preload-task can be defined using a Task Expression.
Each task expression can contain multiple tasks (all can be optional). Tasks
from other files and refs can be referenced using [Task Reference](#task_reference).

```
TASK_EXPR = TASK_REFERENCE [ WHITE_SPACE * '|' [ WHITE_SPACE * TASK_EXPR ] ]
```

To define a task that may not exist and that will not cause the task referencing
it to be INVALID, follow the task name with pipe (`|`) character. This feature
is particularly useful when a property is used in the task name.

```
    preload-task = Optional task {$_name} |
```

To define an alternate task to load when an optional task does not exist,
list the alternate task name after the pipe (`|`) character. This feature
may be chained together as many times as needed.

```
    subtask = Optional Subtask {$_name} |
              Backup Optional Subtask {$_name} Backup |
              Default Subtask # Must exist if the above two don't!
```

<a id="task_reference"/>
Task Reference
---------

Tasks reference can be a simple task name when the defined task is intended to be in
the same file, tasks from other files and refs can also be referenced by syntax explained
below.

```
 TASK_REFERENCE = [
                    [ // TASK_FILE_PATH ]
                    [ @USERNAME [ TASK_FILE_PATH ] ] |
                    [ TASK_FILE_PATH ]
                  ] '^' TASK_NAME
```

To reference a task from root task.config (top level task.config file of a repository)
on the current ref, prefix the task name with `^`.

Example:

task/.../<any>.config
```
    ...
    preload-task = ^Task in root task config
    ...
```

task.config
```
    ...
    [task "Task in root task config"]
    ...
```

To provide an absolute reference to a task under the `task` folder, provide the subpath starting
from `task` directory with a leading `/` followed by a `^` and then task name.

Example:

task.config
```
    ...
    subtask =  /foo/bar/baz.config^Absolute Example Task
    ...
```

task/foo/bar/baz.config
```
    ...
    [task "Absolute Example Task"]
    ...
```

Similarly, to provide reference to tasks which are in a subdirectory of the file containing the
current task avoid the leading `/`.

Example:

task/foo/file.config
```
    ...
    subtask = bar/baz.config^Relative Example Task
    ...
```

task/foo/bar/baz.config
```
    ...
    [task "Relative Example Task"]
    ...
```

Relative tasks specified in a root task.config would look for a file path under the task directory.

Example:

task.config
```
    ...
    subtask = foo/bar.config^Relative from Root Example Task
    ...
```

task/foo/bar.config
```
    ...
    [task "Relative from Root Example Task"]
    ...
```

To reference a task from a specific user ref (All-Users.git:refs/users/<user>), specify the
username with `@`.

when referencing from user refs, to get task from top level task.config on a user ref use
`@<username>^<task_name>` and to get any task under the task directory use the relative
path, like: `@<username>/<relative path from task dir>^<task_name>`. It doesn't matter which
project, ref and file one is referencing from while using this syntax.

Example:
Assumption: Account id of user_a is 1000000

All-Users:refs/users/00/1000000:task.config
```
    ...
    [task "top level task"]
    ...
```

All-Users:refs/users/00/1000000:/task/dir/common.config
```
    ...
    [task "common task"]
    ...
```

All-Projects:refs/meta/config:/task.config
```
    ...
    preload-task = @user_a_username^top level task
    preload-task = @user_a_username/dir/common.config^common task
    ...
```

To reference a task from root task.config on the All-Projects.git, prefix the task name with `//^`
and to reference a task from task dir on the All-Projects.git, use
`//<relative path from task dir>^<task_name>`. It doesn't matter which project, ref and file one
is referencing from while using this syntax.

Example:

All-Projects:refs/meta/config:task.config
```
    ...
    [task "root task"]
    ...
```

All-Projects:refs/meta/config:/task/dir/sample.config

```
    ...
    [task "sample task"]
    ...
```

All-Users:refs/users/00/1000000:task.config
```
    ...
    preload-task = //dir/sample.config^sample task
    preload-task = //^root task
    ...
```
