# Admin User Guide - Configuration

## File `etc/@PLUGIN@.config`

The file `'$site_path'/etc/@PLUGIN@.config` is a Git-style config file
that controls settings for @PLUGIN@ plugin.

### Section "cacheablePredicates"

The cacheablePredicates section configures Change Predicate
optimizations which the @PLUGIN@ plugin may use when evaluating tasks.

#### cacheablePredicates.byBranch.className

The value set with this key specifies a fully qualified class name
of a Predicate which can be assumed to always return the same match
result to all Changes destined for the same project/branch
combinations. This key may be specified more than once.

Example:

```
[cacheablePredicates "byBranch"]
        className = com.google.gerrit.server.query.change.BranchSetPredicate
```

### Section "rootConfig"

The rootConfig section can be used to configure the project and branch containing the root task.config.

#### rootConfig.project

The plugin will fetch the root task.config from the project set for this key. Defaults to `All-Projects`.

#### rootConfig.branch

The plugin will fetch the root task.config from the branch set for this key. Defaults to `refs/meta/config`.

Example:

```
[rootConfig]
        project = task/configuration
        branch = refs/heads/master
```
