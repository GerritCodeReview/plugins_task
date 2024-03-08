The config below is expected to be in the `task.config` file in project
`{root-cfg-prj}` on ref `{root-cfg-branch}`.

file: `{root-cfg-prj}:{root-cfg-branch}:task.config`
```
[root "Operator"]
    applicable = project:test
    subtask = code-review

[task "code-review"]
    applicable = -topic:skip
    pass = label:Code-Review=+2
    ready-hint = Needs +2 code review
```
