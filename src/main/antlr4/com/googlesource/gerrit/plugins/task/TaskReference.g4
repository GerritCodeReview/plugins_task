// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 *
 * This file defines the grammar used for Task Reference
 *
 * TASK_REFERENCE = [
 *                    [ // TASK_FILE_PATH ] |
 *                    [ @USERNAME [ TASK_FILE_PATH ] ] |
 *                    [ %GROUP_NAME [ TASK_FILE_PATH ] ] |
 *                    [ %%GROUP_UUID [ TASK_FILE_PATH ] ] |
 *                    [ TASK_FILE_PATH ]
 *                  ] '^' TASK_NAME
 *
 * Examples:
 *
 * file: All-Projects:refs/meta/config:task.config
 * reference: foo.config^sample
 * Implied task:
 *     file: All-Projects:refs/meta/config:task/foo.config task: sample
 *
 * file: All-Projects:refs/meta/config:task/dir/bar.config
 * reference: /foo.config^sample
 * Implied task:
 *     file: All-Projects:refs/meta/config:task/foo.config task: sample
 *
 * file: All-Projects:refs/meta/config:task/dir/bar.config
 * reference: sub-dir/foo.config^sample
 * Implied task:
 *     file: All-Projects:refs/meta/config:task/dir/sub-dir/foo.config task: sample
 *
 * file: All-Projects:refs/meta/config:task/dir/bar.config
 * reference: ^sample
 * Implied task:
 *     file: All-Projects:refs/meta/config:task.config task: sample
 *
 * file: Any projects, ref, file
 * reference: @jim^sample
 * Implied task:
 *     file: All-Users:refs/users/<jim>:task.config task: sample
 *
 * file: Any projects, ref, file
 * reference: @jim/foo^simple
 * Implied task:
 *     file: All-Users:refs/users/<jim>:task/foo^simple task: sample
 *
 * file: Any projects, ref, file
 * reference: //foo.config^sample
 * Implied task:
 *     file: All-Projects:refs/meta/config:task/foo task: sample
 *
 * file: Any projects, ref, file
 * reference: //^simple
 * Implied task:
 *     file: All-Projects:refs/meta/config:task.config task: sample
 *
 * Suppose a8341ade45d83e867c24a2d37f47b410cfdbea6d is the UUID of 'CI System Owners' group.
 * file: Any projects, ref, file
 * reference: %CI System Owners^sample
 * Implied task:
 *     file: All-Users:refs/groups/a8/a8341ade45d83e867c24a2d37f47b410cfdbea6d:task.config
 *     task: sample
 *
 * file: Any projects, ref, file
 * reference: %CI System Owners/foo^simple
 * Implied task:
 *     file: All-Users:refs/groups/a8/a8341ade45d83e867c24a2d37f47b410cfdbea6d:task/foo^simple
 *     task: sample
 *
 * file: Any projects, ref, file
 * reference: %%a8341ade45d83e867c24a2d37f47b410cfdbea6d^sample
 * Implied task:
 *     file: All-Users:refs/groups/a8/a8341ade45d83e867c24a2d37f47b410cfdbea6d:task.config
 *     task: sample
 *
 */

grammar TaskReference;

options {
  language = Java;
}

reference
  : file_path? TASK
  ;

file_path
 : ALL_PROJECTS_ROOT
 | FWD_SLASH absolute TASK_DELIMETER
 | user absolute? TASK_DELIMETER
 | group_name absolute? TASK_DELIMETER
 | group_uuid absolute? TASK_DELIMETER
 | (absolute| relative)? TASK_DELIMETER
 ;

user
 : '@' NAME
 ;

group_name
 : '%' (NAME | NAME_WITH_SPACES)
 ;

group_uuid
 : '%%' INTERNAL_GROUP_UUID
 ;

absolute
 : FWD_SLASH relative
 ;

relative
 : dir* NAME
 ;

dir
 : (NAME FWD_SLASH)
 ;

TASK
 : (~'^')+ EOF
 ;

INTERNAL_GROUP_UUID
 : HEX_10 HEX_10 HEX_10 HEX_10
 ;

fragment HEX_10
 : HEX HEX HEX HEX HEX HEX HEX HEX HEX HEX
 ;

fragment HEX
 : [0-9a-f]
 ;

NAME
 : URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH_AND_AT_AND_PERCENTILE URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH*
 ;

NAME_WITH_SPACES
 : URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH_AND_AT_AND_PERCENTILE (URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH | SPACE)*
 ;

fragment URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH
 : URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH_AND_AT_AND_PERCENTILE
 | '@' | '%'
 ;

fragment URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH_AND_AT_AND_PERCENTILE
 : ':' | '?' | '#' | '[' | ']'
 |'!' | '$' | '&' | '\'' | '(' | ')'
 | '*' | '+' | ',' | ';' | '='
 | 'A'..'Z' | 'a'..'z' | '0'..'9'
 | '_' | '.' | '\\' | '-' | '~'
 ;

fragment SPACE
 : ' '
 ;

TASK_DELIMETER
 : '^'
 ;

ALL_PROJECTS_ROOT
 : FWD_SLASH FWD_SLASH TASK_DELIMETER
 ;

FWD_SLASH
 : '/'
 ;
