@PLUGIN@
========

The @PLUGIN@ plugin provides a mechanism to manage tasks which need to be
performed on changes. The @PLUGIN@ plugin creates a common place where tasks
can be defined, along with a common way to expose and query this information.
Task definition includes defining which changes each task applies to, and how
to determine the status for each task. Tasks are organized hierarchically.
This hierarchy is considered for task applicability and status.

An important use case of the @PLUGIN@ plugin is to have a common place for CI
systems to define which changes they will operate on, and when they will do
so. This makes it possible for independent and unrelated teams to setup
entirely independent CI systems which operate on different sets of changes,
all while exposing these applicability relations to Gerrit and other teams and
users. This also makes it possible for work for a single change to be split
across multiple cooperating CI systems so that assessments can be staged and
gated upon various other tasks or assessments first completing (or passing).

Exposing task applicability information helps users determine via Gerrit which,
if any, system will "take care" of their changes. Users can thus figure this
out without having the knowledge of "how to", or even "the ability to" query
the configuration of external CI systems. This also makes it possible for
conflicting systems to more easily be detected in cases when more than one
system is mistakenly configured to be responsible for the same changes.

Exposing task hierarchy information via Gerrit helps users understand the
workflow that is expected of their changes. It helps them visualize task
requirements and which tasks are expected to be completed before another task
will even be attempted. It helps them understand how their changes are, or
are not progressing through the outlined stages.

Exposing task status information helps users and CI systems determine via
Gerrit when it is appropriate for them to take action (to perform their task).
It helps them identify blocking tasks. It helps them figure out what they
can do, or perhaps who they need to talk to to ensure that their changes do
make progress through all their hoops.

Task definitions can be split up across multiple files/refs, and even
across multiple projects. This splitting of task definitions allows the
control of task definitions to be delegated to different entities. By
aligning ref boundaries with controlling entities, the standard gerrit ref
ACL mechanisms may be used to control who can define tasks on which changes.

Task Status
-----------
Task status is used to indicate either the readiness of a task for execution
if it has not yet completed execution, or the outcome of the task if it has
completed execution. Tasks generally progress from `WAITING` to `READY` as
their subtasks complete, and then from `READY` to `PASS` once the task itself
completes.

A task with a `WAITING` status is not yet ready to execute. A task in this
state is blocked by its subtasks which are not yet in the `PASS` state.

A task with a `READY` status is ready to be executed. All of its subtasks are
in the `PASS` state.

A task with a `PASS` status meets all the criteria for `READY`, and has
executed and was successful.

A task with a `FAIL` status has executed and was unsuccessful.

A task with a `INVALID` status has an invalid definition.

Tasks
-----
Tasks can either be root tasks, or subtasks. Tasks are defined in the
`All-Projects` project, on the `refs/meta/config` branch, in a file named
`task.config`. This file uses the gitconfig format to define root tasks and
subtasks. The special "True" keyword may be used as any query definition to
indicate an always matching query. The following keys may be defined in any
task section:

`applicable`

: This key defines a query that is used to determine whether a task is
applicable to each change. Since tasks are defined hierarchically, the
applicability of subtasks is inherently limited by the applicability of
all tasks above them in the hierarchy.

Example:
```
    applicable = status:open
```

`fail`

: This key defines a query that is used to determine whether a task has
already executed and failed for each change.

Example:
```
    fail = label:verified-1
```

`in-progress`

: This key defines a query that is used to determine whether a task is
currently in-progress or not. A CI system may use this to ensure that it
only runs one verification instance for a specific change. Either a pass
or fail key is mandatory for leaf tasks. A task with a fail criteria,
but no pass criteria, will pass if it otherwise would be ready. Setting
this to "True" is useful for defining blocking criteria that do not
actually have a task to execute.

Example:
```
    in-progress = label:patchset-lock,user=jenkins
```

`pass`

: This key defines a query that is used to determine whether a task has
already executed and passed for each change. Either a pass or fail key is
mandatory for leaf tasks. Tasks with no defined pass criteria and with
defined subtasks are valid, but they are only applicable when at least
one subtask is applicable. Setting this to "True" is useful for defining
informational tasks that are not really expected to execute.

Example:
```
    pass = label:verified+1
```

Example:
```
    in-progress = label:patchset-lock,user=jenkins
```

`ready-hint`

: This key defines a hint when a task is `READY` describing what
accomplishing the tasks entails. This is meant to be a hint for humans
and may be used as a tool-tip.

Example:
```
    ready-hint = Needs to be verified by Jenkins
```

`fail-hint`

: This key defines a hint when a task is in the `FAIL` state describing why
the task is failing. This is meant to be a hint for humans and may be used
as a tool-tip.

Example:
```
    fail-hint = Blocked by a negative review score
```

`subtask`

: This key lists the name of a subtask of the current task. This key may be
used several times in a task section to define more than one subtask for a
particular task.

Example:

```
    subtask = "Code Review"
    subtask = "License Approval"
```

`subtasks-external`

: This key defines a file containing subtasks of the current task. This
key may be used several times in a task section to define more than one file
containing subtasks for a particular task. The subtasks-external key points
to an external file defined by external section. Note: all of the tasks in
the referenced file will be included as subtasks of the current task!

Example:

```
    subtasks-external = my-external
```

`subtasks-file`

: This key defines a file containing subtasks of the current task. This
key may be used several times in a task section to define more than one file
containing subtasks for a particular task. The subtasks-file key points to
a file under the top level task directory in the same project and ref as the
current task file. Note: all of the tasks in the referenced file will be
included as subtasks of the current task!

Example:

```
    subtasks-file = common.config  # references the file named task/common.config
```

Root Tasks
----------
Root tasks typically define the "final verification" tasks for changes. Each
root task likely defines a single CI system which is responsible for verifying
and possibly submitting the changes which are managed by that CI system.
`applicable` queries for all root tasks should generally be defined in a non
overlapping fashion.

Root tasks are defined using "root" sections. A sample task.config which
defines 3 non overlapping CI systems might look like this:

```
[root "Jenkins Build"]
   applicable = status:open AND (project:a OR project:b) AND -branch:master
   ...

[root "Jenkins Build and Test"]
   applicable = status:open AND (project:a OR project:b) AND branch:master
   ...

[root "Buildbot"]
   applicable = status:open AND project:c
   ...
```

Subtasks
--------
Subtasks define tasks that must pass before their parent task state is
considered `READY` or `PASS`. Subtasks make it possible to define task
execution dependencies and ordering. Subtasks typically define all the
things that are required for change submission except for the final criteria
that will be assessed by the final verification defined by the change's root
task. This may include tasks that need to be executed by humans, such as
approvals like `code-review`, along with automated tasks such as tests, or
static analysis tool executions.

Subtasks are defined using a "task" section. An example subtask definition:

```
[task "Code Review"]
    pass = label:code-review+2
    fail = label:code-review-2
```

External Entries
----------------
A name for external task files on other projects and branches may be given
by defining an `external` section in a task file. This later allows this
external name to then be referenced by other definitions. The following
keys may be defined in an external section. External references are limited
to files under the top level task directory.

`file`

: This key defines the name of the external task file under the
task directory referenced.

Example:

```
    file = common.config  # references the file named task/common.config
```

`user`

: This key defines the username of the user's ref in the `All-Users` project
of the external file referenced.

Example:

```
    user = first-user # references the sharded user ref refs/users/01/1000001
```

Change Query Output
-------------------
It is possible to add a task section to the query output of changes using
the task plugin switches. The following switches are available:

**\-\-@PLUGIN@\-\-applicable**

This switch is meant to be used to determine the state of applicable
tasks for each change, it outputs applicable tasks for a change.

**\-\-@PLUGIN@\-\-all**

This switch is meant as a debug switch, it outputs all tasks visible to the
calling user, whether they apply to a change or not. When this flag is used,
an additional 'applicable' property is included in each task output to
indicate whether the task actually met its applicability criteria or not.

**\-\-@PLUGIN@\-\-preview**

This switch is meant as a debug switch for previewing changes to task configs.
This switch outputs tasks as if the supplied change were already merged. This
makes it possible to preview the effect of proposed changes before going live
with them. Multiple changes may be previewed together.

**\-\-@PLUGIN@\-\-invalid**

This switch is meant as a debug switch to help find mis-configured tasks,
it causes only invalid tasks and the tasks in the tree hierarchy above them
to be output. To verify task validity, this change runs all queries defined
for a task, no matter what the tasks state is; this makes it possible to
detect some additional configuration problems which may not be detected when
running normally. If all tasks are properly configured, this switch should
not output anything. This switch is particularly useful in combination
with the **\-\-@PLUGIN@\-\-preview** switch.

When tasks are appended to changes, they will have a "task" section under
the plugins section like below:

```
  $ ssh -x -p 29418 example.com gerrit query change:123
  change I9fdfb1315610a8e3d5c48e4321193b7c265f30ae
  ...
  plugins:
    name: task
    roots:
      name: Jenkins Build and Test
      inProgress: false
      status: READY
      subTasks:
        name: code review
        status: PASS
```

Examples
--------
See [task_states](task_states.html) for a comprehensive list of examples
of task configs and their states.
