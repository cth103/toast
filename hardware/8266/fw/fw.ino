/* -*- c-basic-offset: 2; tab-width: 2 -*-; indent-tabs-mode: nil; */

#include <SoftwareSerial.h>
#include <OneWire.h>
#include <DallasTemperature.h>

#define SSID "PlusnetWireless998FC7"
#define PASS "E5383F2817"

#define ESP8266_TX_PIN 0
#define ESP8266_CH_PD_PIN 1
#define ESP8266_RX_PIN 2
#define DEBUG_TX_PIN 3
#define DEBUG_RX_PIN 4
#define ONE_WIRE_BUS 4

SoftwareSerial wifi(ESP8266_RX_PIN, ESP8266_TX_PIN);
SoftwareSerial debug(DEBUG_RX_PIN, DEBUG_TX_PIN);
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensor(&oneWire);

uint8_t scratchPad[9];
uint8_t const sensorAddress[8]= { 0x28, 0xee, 0xe1, 0xe2, 0x12, 0x16, 0x1, 0x60 };

void
resetWifi()
{
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  delay(100);
  digitalWrite(ESP8266_CH_PD_PIN, LOW);
  delay(1000);
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  wifi.print("AT+RST\r\n");
}

void
setup()
{
  pinMode(ESP8266_TX_PIN, OUTPUT);
  pinMode(ESP8266_CH_PD_PIN, OUTPUT);
  pinMode(ESP8266_RX_PIN, INPUT);
  pinMode(DEBUG_TX_PIN, OUTPUT);
  pinMode(ONE_WIRE_BUS, OUTPUT);

  OSCCAL = 82;

  debug.begin(9600);
  debug.println("Hello world!");
  debug.flush();

  return;

  wifi.begin(9600);
  wifi.listen();

  //debug.println("Resetting ESP8266.");
  resetWifi();
  delay(2000);

  wifi.setTimeout(5000);
  if (wifi.find("WIFI GOT IP")) {
    //debug.println("ESP8266 already connected.");
    //debug.flush();
    return;
  }

  while (true) {

    wifi.setTimeout(5000);
    //debug.println("Talking to ESP8266.");
    //debug.flush();
    wifi.print("AT+CWMODE=1\r\n");
    wifi.flush();
    if (!wifi.find("OK")) {
      //debug.println("failed.");
      //debug.flush();
      continue;
    }

    wifi.setTimeout(25000);
    //debug.println("ok");
    //debug.println("Connecting to network.");
    //debug.flush();
    wifi.print("AT+CWJAP=\"" SSID "\",\"" PASS "\"\r\n");
    wifi.flush();
    if (!wifi.find("OK")) {
      //debug.println("failed.");
      continue;
    }

    wifi.setTimeout(1000);
    //debug.println("ok.");
    //debug.println("Enabling DHCP.");
    //debug.flush();
    wifi.print("AT+CWDHCP=1,1\r\n");
    wifi.flush();
    if (!wifi.find("OK")) {
      //debug.println("failed.");
      continue;
    }

    //debug.println("ok.");
    break;
  }
}

void
loop()
{
  debug.println(sensor.getTempC(sensorAddress), 2);

  wifi.print("AT+CIPSTART=\"TCP\",\"192.168.1.1\",4024\r\n");
  wifi.find("OK");

  wifi.print("AT+CIPSEND=14\r\n");
  wifi.find("OK");
  wifi.find(">");
  wifi.print("Hello Dolly.\r\n");
  if (!wifi.find("OK")) {
    debug.println("UDP send failed.");
  }

  wifi.print("AT+CIPCLOSE\r\n");
  if (!wifi.find("OK")) {
    debug.println("UDP close failed.");
    return;
  }

  delay(1000);
}
