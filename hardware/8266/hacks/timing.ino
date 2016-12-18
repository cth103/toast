/* -*- c-basic-offset: 2; tab-width: 2 -*-; indent-tabs-mode: nil; */

#include <SoftwareSerial.h>

#define ESP8266_TX_PIN 0
#define ESP8266_CH_PD_PIN 1
#define ESP8266_RX_PIN 2
#define DEBUG_TX_PIN 3
#define DEBUG_RX_PIN 4

SoftwareSerial fuck(ESP8266_RX_PIN, ESP8266_TX_PIN);
SoftwareSerial debug(DEBUG_RX_PIN, DEBUG_TX_PIN);

void setup()
{
	debug.begin(9600);
	OSCCAL = 82;
}

void loop()
{
	debug.print("U");
}
