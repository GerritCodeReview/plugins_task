Task Plugin Config
==================

***

**Requirements**
----------------
1. Task config devs should be able to collaboratively develop and test config using their own accounts
    1. Must support collaboration across task config devs in various CI teams
    2. Devs can preview task config changes before submitting them (using --preview)
    3. Admins/devs can find the config where a specific task is defined (using --include-paths)
2. Gerrit users should be able to view applicable tasks on their changes even when those tasks come from various team specific configs
3. Task config devs should be able to configure tasks whose definition and applicability are visible only to a desired set of users
    1. Users outside that set should not see invalid tasks if the tasks are not applicable on their changes
4. Task config devs should be able to create automated jobs to
    1. Verify the correctness of their task config changes
    2. Merge task config updates after their desired gating criteria is met
    3. Create/update task config changes
    4. Only validate the roots they are interested in
5. Task config devs can define gating criteria for task configs they own
    1.  Gating criteria for task configs can be defined using task.config (nice to have)
    2.  Can block submit until dependent change (group query/destinations update for example) is submitted

***

The next two sections highlight how meeting some above-listed requirements is difficult with the default configuration and pits itself against the custom root project and branch configuration.

**Task config root in All-Projects:refs/meta/config (default)**
---------------------------------------------------------------
#### Cons:
  1. refs/meta/config has restricted permissions usually, so admins have to get involved to get config updates merged. Hard to satisfy requirement [4.2](#requirements)
  2. Permissions on refs/meta/config have to be relaxed so that task.config devs can read/push on that ref which may expose other configs on that ref
  3. Having config in All-Projects leads to customized labels, submit requirements and code-owners config being inherited by other projects we didn't intend to apply to
  4. Requires special setup for fetching/pushing to refs/meta/config
  5. Relies on group refs for requirements [3, 4 ,5](#requirements). See [Solutions for requirement [3]](#solutions-for-requirement-3)
#### Pros:
  1. Consistent with other plugin configs
  2. Common location for Task config devs to share/find task config
  3. Admins control task roots


**Task.config root in a custom project:branch**
-----------------------------------------------
#### Cons:
  1. Not consistent with other plugin configs
  2. Location of task config might be hard to find by task config devs
  3. Relies on group refs for requirement [3](#requirements). See [Solutions for requirement [3]](#solutions-for-requirement-3)
  4. Admins potentially have less oversight on task roots
  5. Cannot have project configuration such as labels, submit requirements, code-owners per team. Independent project:branch per team needs to be used if this is needed.
#### Pros:
  1. Admins do only one time setup for project permissions
  2. Permissions on the ref containing task configs can be setup as desired by the custom project:branch owner (might not include all task config devs)
  3. Can customize labels, create new submit requirements and enable other plugins(code-owners for example) without any impact on other projects
  4. Reduces dependency on moving team config to independent projects/branches


***

This section details the known solutions to requirement [3] whilst highlighting advantages and limitations of each solution.

**Solutions for requirement [3]**
---------------------------------
### **Team task config on group refs**
#### Cons:
  1. Group refs are only visible to group owners and upstream does not want to change this. Service accounts and collaborators would need to become group owners in order to do anything useful with the team's group-based task config.
  2. Requires internal Gerrit group
  3. Requires special setup for fetching/pushing to refs/groups/..
#### Pros:
  1. Admins do a one-time setup to allow everyone to push/review/submit to group refs
  2. Gerrit groups already have a concept of visibility based on membership

### **Team task config on custom group refs (refs/heads/group-owned/..) in All-Users**
#### Cons:
  1. Not consistent with other plugin configs
  2. Location of task config might be hard to find by task config devs
  3. Admins have to set up permissions on the new refs/heads/group-owned/.. ref. See implementation gaps.
  4. There is a risk that any project customization can interfere with core's use of All-Users
  5. These refs are not an existing gerrit concept and might be hard to get acceptance from upstream
#### Pros:
  1. Permissions on the ref containing task configs can be setup as desired by the task config devs
  2. Can customize labels, create new submit requirements and enable other plugins(code-owners for example) without any impact on other projects
  3. Gerrit groups already have a concept of visibility based on membership
#### Implementation gap:
  1. Plugin currently does not support task expressions referring to a refs/heads/group-owned/..
  2. No support for automatically configuring permissions on refs/heads/group-owned/.. or otherwise tying the ref to the group

### **Team task config on custom project branch**
#### Cons:
  1. Not consistent with other plugin configs (but might be similar if refs/meta/config is used as the branch)
  2. Location of task config might be hard to find by task config devs
  3. Admins have to setup new projects for each team if existing projects are not used
#### Pros:
  1. Admins do only one time setup for project permissions. Might already be done if it is an existing project.
  2. Permissions on the ref containing task configs can be setup as desired by the task config devs
  3. Can customize labels, create new submit requirements and enable other plugins(code-owners for example) without any impact on other projects
#### Implementation gap:
  1. Plugin currently does not support task expressions referring to a custom project:branch

