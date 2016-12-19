/* -*- c-basic-offset: 2; tab-width: 2 -*-; indent-tabs-mode: nil; */
/*
    Copyright (C) 2016 Carl Hetherington <cth@carlh.net>

    This file is part of toast.

    toast is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    toast is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with toast.  If not, see <http://www.gnu.org/licenses/>.

*/

#include <Arduino.h>
#include <SoftwareSerial.h>
#include <OneWire.h>
#include <DallasTemperature.h>

/* Pins on the trinket that things are connected to */
#define ESP8266_RX_PIN 0
#define ESP8266_CH_PD_PIN 1
#define ESP8266_TX_PIN 2
#define RELAY 3
#define ONE_WIRE_BUS 4

/* Where to listen */
#define LISTEN_IP "192.168.1.7"
#define LISTEN_PORT "9142"

/* One-wire bus address of our DS18B20 */
uint8_t const sensorAddress[8] = { 0x28, 0xee, 0xe1, 0xe2, 0x12, 0x16, 0x1, 0x60 };

SoftwareSerial wifi(ESP8266_RX_PIN, ESP8266_TX_PIN);
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensor(&oneWire);

uint8_t scratchPad[9];
char* connect[] = { "CWMODE=1", "CWJAP=\"" SSID "\",\"" PASS "\"", "CIPSTA=\"" LISTEN_IP "\"" };

/** Reset the Wifi board by pulling its CH_PD pin low */
void
resetWifi()
{
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  delay(100);
  digitalWrite(ESP8266_CH_PD_PIN, LOW);
  delay(1000);
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
}

/** Send an AT command and wait for "OK" to come back */
bool
sendWithOk(char const * message)
{
  wifi.print("AT+");
  wifi.print(message);
  wifi.print("\r\n");
  return wifi.find("OK");
}

void
startServer()
{
  sendWithOk("CIPMUX=1");
  sendWithOk("CIPSERVER=1," LISTEN_PORT);
}

void
setup()
{
  pinMode(ESP8266_TX_PIN, OUTPUT);
  pinMode(ESP8266_CH_PD_PIN, OUTPUT);
  pinMode(ESP8266_RX_PIN, INPUT);
  pinMode(ONE_WIRE_BUS, OUTPUT);
  pinMode(RELAY, OUTPUT);

	/* Empirically derived to give accurate 9600 baud with SoftwareSerial;
	 * I'm not sure if this is necessary.
	 */
  OSCCAL = 82;

  wifi.begin(9600);
  wifi.listen();

  resetWifi();
  delay(2000);

  wifi.setTimeout(5000);

  while (true) {
    int i;
    for (i = 0; i < 3; ++i) {
      if (!sendWithOk(connect[i])) {
        break;
      }
    }

    if (i == 3) {
      startServer();
      return;
    }
  }
}

void
loop()
{
  while (true) {
    char c = wifi.read();
    if (c == 's') {
			/* Send temperature */
      sendWithOk("CIPSEND=0,7");
      wifi.find(">");
      sensor.requestTemperatures();
      wifi.println(sensor.getTempC(sensorAddress), 2);
      wifi.find("OK");
      break;
    } else if (c == 'p') {
			/* Radiator on */
      digitalWrite(RELAY, true);
      break;
    } else if (c == 'q') {
			/* Radiator off */
      digitalWrite(RELAY, false);
      break;
    }
  }

  sendWithOk("CIPCLOSE=0");
}
