# Admin User Guide - Configuration

## File `etc/gerrit.config`

The file `'$site_path'/etc/gerrit.config` is a Git-style config file
that controls many host specific settings for Gerrit.

### Section @PLUGIN@ "cacheable-predicates"

The @PLUGIN@.cacheable-predicates section configures Change Predicate
optimizations which the @PLUGIN@ plugin may use when evaluating tasks.

#### @PLUGIN@.cacheable-predicates.byBranch-className

The value set with this key specifies a fully qualified class name
of a Predicate which can be assumed to always return the same match
result to all Changes destined for the same project/branch
combinations. This key may be specified more than once.

Example:

```
[@PLUGIN@ "cacheable-predicates"]
        byBranch-className = com.google.gerrit.server.query.change.BranchSetPredicate
```