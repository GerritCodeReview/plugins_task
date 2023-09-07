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
 * TASK_REF = [ [ TASK_FILE_PATH ] '^' ] TASK_NAME
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
 */

grammar TaskReference;

options {
  language = Java;
}

reference
  : file_path? TASK
  ;

file_path
 : (absolute| relative)? TASK_DELIMETER
 ;

absolute
 : '/' relative
 ;

relative
 : dir* NAME
 ;

dir
 : (NAME '/')
 ;

TASK
 : (~'^')+ EOF
 ;

NAME
 : URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH+
 ;

fragment URL_ALLOWED_CHARS_EXCEPT_FWD_SLASH
 : ':' | '?' | '#' | '[' | ']' | '@'
 |'!' | '$' | '&' | '\'' | '(' | ')'
 | '*' | '+' | ',' | ';' | '=' | '%'
 | 'A'..'Z' | 'a'..'z' | '0'..'9'
 | '_' | '.' | '\\' | '-' | '~'
 ;

TASK_DELIMETER
 : '^'
 ;