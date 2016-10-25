#!/bin/bash

avrdude -c usbtiny -p attiny85 -C avrdude.conf -U flash:w:fw.ino.hex
