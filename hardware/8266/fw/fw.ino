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
//SoftwareSerial debug(DEBUG_RX_PIN, DEBUG_TX_PIN);
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensor(&oneWire);

uint8_t scratchPad[9];
uint8_t const sensorAddress[8] = { 0x28, 0xee, 0xe1, 0xe2, 0x12, 0x16, 0x1, 0x60 };
char* connect[] = { "AT+CWMODE=1", "AT+CWJAP=\"" SSID "\",\"" PASS "\"", "AT+CWDHCP=1,1" };

void
resetWifi()
{
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  delay(100);
  digitalWrite(ESP8266_CH_PD_PIN, LOW);
  delay(1000);
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  //wifi.print("AT+RST\r\n");
}

bool
sendWithOk(char const * message)
{
  wifi.print(message);
  wifi.print("\r\n");
  return wifi.find("OK");
}

void
setup()
{
  pinMode(ESP8266_TX_PIN, OUTPUT);
  pinMode(ESP8266_CH_PD_PIN, OUTPUT);
  pinMode(ESP8266_RX_PIN, INPUT);
  //pinMode(DEBUG_TX_PIN, OUTPUT);
  pinMode(ONE_WIRE_BUS, OUTPUT);

  OSCCAL = 82;

  //debug.begin(9600);
  //debug.println("Hello world!");
  //debug.flush();

  wifi.begin(9600);
  wifi.listen();

  resetWifi();
  delay(2000);

  wifi.setTimeout(5000);
  if (wifi.find("GOT IP")) {
    return;
  }

  while (true) {
 
    wifi.setTimeout(5000);
    int i;
    for (i = 0; i < 3; ++i) {
      if (!sendWithOk(connect[i])) {
        break;
      }
    }

    if (i == 3) {
      return;
    }
  }
}

void
loop()
{
  sendWithOk("AT+CIPMUX=1");
  sendWithOk("AT+CIPSERVER=1");
  while (!wifi.find("S")) {}
  sendWithOk("AT+CIPSEND=0,7");
  wifi.find(">");
  sensor.requestTemperatures();
  wifi.print(sensor.getTempC(sensorAddress), 2);
  sendWithOk("");
  sendWithOk("AT+CIPCLOSE=0");
}
