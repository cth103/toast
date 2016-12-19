/* -*- c-basic-offset: 2; tab-width: 2 -*-; indent-tabs-mode: nil; */

#include <Arduino.h>
#include <SoftwareSerial.h>
#include <OneWire.h>
#include <DallasTemperature.h>

#define SSID "PlusnetWireless998FC7"
#define PASS "E5383F2817"

#define ESP8266_TX_PIN 2
#define ESP8266_CH_PD_PIN 1
#define ESP8266_RX_PIN 0
#define RELAY 3
#define ONE_WIRE_BUS 4
#define LISTEN_IP "192.168.1.7"
#define LISTEN_PORT "9142"

SoftwareSerial wifi(ESP8266_RX_PIN, ESP8266_TX_PIN);
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensor(&oneWire);

uint8_t scratchPad[9];
uint8_t const sensorAddress[8] = { 0x28, 0xee, 0xe1, 0xe2, 0x12, 0x16, 0x1, 0x60 };
char* connect[] = { "CWMODE=1", "CWJAP=\"" SSID "\",\"" PASS "\"", "CIPSTA=\"" LISTEN_IP "\"" };

void
resetWifi()
{
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  delay(100);
  digitalWrite(ESP8266_CH_PD_PIN, LOW);
  delay(1000);
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
}

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
      sendWithOk("CIPSEND=0,7");
      wifi.find(">");
      sensor.requestTemperatures();
      wifi.println(sensor.getTempC(sensorAddress), 2);
      wifi.find("OK");
      break;
    } else if (c == 'p') {
      digitalWrite(RELAY, true);
      break;
    } else if (c == 'q') {
      digitalWrite(RELAY, false);
      break;
    }
  }

  sendWithOk("CIPCLOSE=0");
}
