#!/usr/bin/env bash

options_prefix() { # array_name option_name
    local array_name=$1
    local array_elements_name=$array_name[@]
    local option=$2
    local items=("${!array_elements_name}")
    local item
    eval "$array_name=()"
    for item in "${items[@]}"; do
        eval "$array_name"'+=("$option" "$item")'
    done
}
