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
#include <avr/wdt.h>

/* Pins on the trinket that things are connected to */
#define ESP8266_RX_PIN 0     /* pin connected to ESP8266 TX */
#define ESP8266_CH_PD_PIN 1  /*                  ESP8266 CH_PD */
#define ESP8266_TX_PIN 2     /*                  ESP8266 RX */
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

char* setup_messages[] = { "WMODE=1", "WJAP=\"" SSID "\",\"" PASS "\"", "IPSTA=\"" LISTEN_IP "\"", "IPMUX=1", "IPSERVER=1," LISTEN_PORT };

unsigned long int lastActivity = millis();

/** Send an AT command and wait for "OK" to come back */
bool
sendWithOk(char const * message)
{
	wifi.print("AT+C");
	wifi.print(message);
	wifi.print("\r\n");
	wdt_reset();
	bool const r = wifi.find("OK");
	wdt_reset();
	return r;
}

bool safeFind(char* message)
{
	wdt_reset();
	bool r = wifi.find(message);
	wdt_reset();
	return r;
}

void initWifi()
{
	while (true) {
		wdt_reset();

		/* Reset the Wifi board by pulling its CH_PD pin low */
		digitalWrite(ESP8266_CH_PD_PIN, HIGH);
		delay(100);
		digitalWrite(ESP8266_CH_PD_PIN, LOW);
		delay(1000);
		digitalWrite(ESP8266_CH_PD_PIN, HIGH);

		delay(2000);
		wdt_reset();

		int i;
		for (i = 0; i < 5; ++i) {
			if (!sendWithOk(setup_messages[i])) {
				break;
			}
		}

		if (i == 5) {
			/* All ok */
			return;
		}

		/* Something failed, go back round to reset the board and try again */
	}
}

void
setup()
{
	pinMode(ESP8266_TX_PIN, OUTPUT);
	pinMode(ESP8266_CH_PD_PIN, OUTPUT);
	pinMode(ESP8266_RX_PIN, INPUT);
	pinMode(ONE_WIRE_BUS, OUTPUT);
	pinMode(RELAY, OUTPUT);

	/* Give ourselves 2s of sanity in case the watchdog goes crazy */
	wdt_disable();
	delay(2000);
	wdt_enable(WDTO_8S);

	/* Empirically derived to give accurate 9600 baud with SoftwareSerial;
	 * I'm not sure if this is necessary.
	 */
	OSCCAL = 82;

	wifi.begin(9600);
	wifi.listen();
	wifi.setTimeout(5000);

	initWifi();
}

void
loop()
{
	int lastTemperature = 0;

	while (true) {

		wdt_reset();

		if (millis() > (lastActivity + 10000)) {
			/* See if the Wifi module is still with us */
			wifi.print("AT\r\n");
			if (!safeFind("OK")) {
				initWifi();
			}
			lastActivity = millis ();
		}

		wdt_reset();
		char c = wifi.read();
		if (c != -1) {
			lastActivity = millis();
		}
		wdt_reset();

		if (c == 's') {
			/* Send temperature */
			sendWithOk("IPSEND=0,6");
			safeFind(">");
			sensor.requestTemperatures();
			/* Send temperature as a raw value to avoid pulling in the FP libraries
			   (I think); program size is about 2k larger if you do getTempC here.
			*/
			int16_t val = sensor.getTemp(sensorAddress);
			if (val == DEVICE_DISCONNECTED_RAW) {
				val = lastTemperature;
			}

			if (val < 0x1000) {
				wifi.print("0");
			}
			if (val < 0x100) {
				wifi.print("0");
			}
			if (val < 0x10) {
				wifi.print("0");
			}
			wifi.println(val, HEX);
			lastTemperature = val;
			safeFind("OK");
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

	sendWithOk("IPCLOSE=0");
}
