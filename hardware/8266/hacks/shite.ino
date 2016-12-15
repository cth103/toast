/* -*- c-basic-offset: 2; tab-width: 2 -*-; indent-tabs-mode: nil; */

#include <SoftwareSerial.h>

#define DEBUG_TX_PIN 3
#define DEBUG_RX_PIN 4

SoftwareSerial debug(DEBUG_RX_PIN, DEBUG_TX_PIN);

void setup()
{
	debug.begin(9600);
}

void loop()
{
	debug.println("Hello");
  debug.println(42, HEX);
}
