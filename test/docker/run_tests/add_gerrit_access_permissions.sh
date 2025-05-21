#!/usr/bin/env bash

GROUP_ID_ADMINISTRATORS=$(curl --no-progress-meter --netrc -X GET \
    "http://$GERRIT_HOST:8080/a/groups/Administrators" | \
    sed -e '1!b' -e "/^)]}'$/d" | jq -r '.id')
curl --no-progress-meter --netrc -X POST \
    "http://$GERRIT_HOST:8080/a/projects/All-Projects/access" \
    --header 'Content-Type: application/json' --output /dev/null \
    --data @<(cat <<EOF
{
    "add": {
        "refs/*": {
            "permissions": {
                "push": {
                    "rules": {
                        "$GROUP_ID_ADMINISTRATORS": {
                            "action": "ALLOW"
                        }
                    }
                }
            }
        }
    }
}
EOF
)
