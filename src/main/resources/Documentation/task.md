@PLUGIN@
========

The @PLUGIN@ plugin provides a mechanism to manage tasks which need to be
performed on changes. The task plugin creates a common place where tasks can
be defined, along with a common way to expose and query this information.
Task definition includes defining which changes each task applies to, and how
to determine the status for each task. Tasks are organized hierarchically.
This hierarchy is considered for task applicability and status.

An important use case of the task plugin is to have a common place for CI
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
subtasks. The following keys may be defined in any task section:

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

`pass`

: This key defines a query that is used to determine whether a task has
already executed and passed for each change. This key is mandatory for
leaf tasks.

Example:
```
    pass = label:verified+1
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

Root Tasks
----------
Root tasks typically define the "final verification" tasks for changes. Each
root task likely defines a single CI system which is responsible for verifying
and possibly submitting the changes which are managed by that CI system.
Applicable queries for all root tasks should generally be defined in a non
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

Change Query Output
-------------------
Changes which have tasks applicable to them will have a "task" section
which will include applicable tasks for the change added to their output.

```
  $ ssh -x -p 29418 example.com gerrit query change:123
  change I9fdfb1315610a8e3d5c48e4321193b7c265f30ae
  ...
  plugins:
    name: task
    roots:
      name: Jenkins Build and Test
      status: READY
      subTasks:
        name: code review
        status: PASS
```

Examples
--------
See [task_states](task_states.html) for a comprehensive list of examples
of task configs and their states.
