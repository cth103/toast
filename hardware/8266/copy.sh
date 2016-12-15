#!/bin/bash

if [ "$1" == "" ]; then
    echo "Syntax: $0 <.ino>"
    exit 1
fi

avrdude -c usbtiny -p attiny85 -C avrdude.conf -U flash:w:$1.hex
