Manual Tests
------------

1. Test that task validation for more than one change provides different
   results. This will ensure that the per-change match cache introduced
   to avoid duplicate queries is being re-newed for each change.

   Pick two changes which have different results for atleast one of the
   task-applicable queries. For example, we can use a task with status:open
   as applicability, one of the changes can be open and the other one merged.

   For example, 12345 is open and 12346 is merged and atleast one task has
   status:open as applicability. Below query should return different results
   for both changes:

    ssh -x -p 29418 review.example.com gerrit query
      --format JSON 'change:12345 OR change:12346'
      --task--all --task--preview 12347,1

