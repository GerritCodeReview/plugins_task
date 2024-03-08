Search Operators
================

Root Operator
--------------------------

This @PLUGIN@ plugin provides a `root_@PLUGIN@` change search operator
which can search changes that are applicable to a root task.

**root_@PLUGIN@:ROOT[,status=STATUS]**

: Matches all changes that are applicable to the provided `ROOT`. When the `status`
option is provided, the change is filtered based on the root's status. Note that,
`STATUS` should be one of `INVALID`, `UNKNOWN`, `DUPLICATE`, `WAITING`, `READY`,
`PASS` or `FAIL`.