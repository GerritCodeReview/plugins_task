task plugin config
  requirements:
    1. task config devs should be able to collaboratively develop and test config using their own accounts
      a. must support collaboration across teams
      b. devs can preview task config changes before submitting them (using --preview)
      c. admins/devs can find the config where a specific task is defined (using --include-paths)  
    2. gerrit users should be able to view applicable tasks on their changes even when those tasks come from various team specific configs
    3. teams should be able to configure tasks whose definition and applicability are visible only to a desired set of users
      a. users outside that set should not see invalid tasks if the tasks are not applicable on their changes (needs testing and a fix if needed)
    4. teams should be able to create automated jobs to
      a. verify the correctness of their task config changes
      b. merge task config updates after their desired gating criteria is met
      c. create/update task config changes
    5. task config devs can define gating criteria for task configs they own
      a. (nice to have) gating criteria for task configs can be defined using task.config
      b. can block submit until dependent change (group query/destinations update for example) is submitted
    6. CI systems can filter tasks to only roots they are interested in 


  task config root within All-Projects:refs/meta/config
    cons:
      refs/meta/config has restricted permissions usually, so admins have to get involved to get config updates merged. Hard to satisfy requirement [4.b]
      permissions on refs/meta/config have to be relaxed so that task.config devs can read/push on that ref which may expose other configs on that ref
      having config in All-Projects leads to customized labels, submit requirements and code-owners config being inherited by other projects we didn't intend to apply to
      requires special setup for fetching/pushing to refs/meta/config
      relies on group refs for requirement [3]
        see section [solutions for requirement [3]]
    pros:
      consistent with other plugin configs
      common location for teams to share/find task config
      admins control task roots
    requirement gaps: [4.b]


  task.config root on a custom project:branch
    cons:
      not consistent with other plugin configs
      location for teams to find task config might be hard
      relies on group refs for requirement [3]
        see section [solutions for requirement [3]]
      admins have less oversight on task roots
      cannot have project configuration per team. Independent project:branch per team needs to be used if this is needed.
    pros:
      admins do only one time setup for project permissions
      permissions on the ref containing task configs can be setup as desired by the custom project:branch owner (might not include all task config devs)
      can customize labels, create new submit requirements and enable other plugins(code-owners for example) without any impact on other projects
      reduces dependency on moving team config to indepdendent projects/branches
    implementation gap:
      plugin currently does not support a custom project:branch for config with roots

  solutions for requirement [3]
    team task config on group refs
      cons:
        group refs are only visible to group owners and upstream does not want to change this
        requires internal Gerrit group
        requires special setup for fetching/pushing to refs/groups/..
      pros:
        admins do a one-time setup to allow everyone to push/review/submit to group refs
        Gerrit groups already have a concept of visibility based on membership
      requirement gaps: [4.b]

    team task config on custom group refs (refs/heads/group-owned/..) in All-Users
      cons:
        not consistent with other plugin configs
        location for teams to find task config might be hard
        admins have to setup permissions on the new refs/heads/group-owned/.. ref. See implementation gaps.
        there is a risk that any project customization can interfere with core's use of All-Users
        these refs are not an existing gerrit concept and might be hard to get acceptance from upstream
      pros:
        permissions on the ref containing task configs can be setup as desired by the task config devs
        can customize labels, create new submit requirements and enable other plugins(code-owners for example) without any impact on other projects
        Gerrit groups already have a concept of visibility based on membership
      implementation gap:
        plugin currently does not support referring to a refs/heads/group-owned/..
        no support for automatically configuring permissions on refs/heads/group-owned/..

    team task config on custom project branch
      cons:
        not consistent with other plugin configs
        location for teams to find task config might be hard
        admins have to setup new projects for each team if existing projects are not used
      pros:
        admins do only one time setup for project permissions. Might already be done if it is an existing project.
        permissions on the ref containing task configs can be setup as desired by the task config devs
        can customize labels, create new submit requirements and enable other plugins(code-owners for example) without any impact on other projects
      implementation gap:
        plugin currently does not support referring to a custom project:branch

