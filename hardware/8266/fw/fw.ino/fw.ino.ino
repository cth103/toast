
#include <SoftwareSerial.h>

#define SSID "PlusnetWireless998FC7"
#define PASS "E5383F2817"

#define ESP8266_TX_PIN 0
#define ESP8266_CH_PD_PIN 1
#define ESP8266_RX_PIN 2
#define DEBUG_TX_PIN 3
#define DEBUG_RX_PIN 4

SoftwareSerial wifi(ESP8266_RX_PIN, ESP8266_TX_PIN);
SoftwareSerial debug(DEBUG_RX_PIN, DEBUG_TX_PIN);

char buffer[128];

void
readLine(unsigned int timeout)
{
  unsigned long const end_time = millis() + timeout;
  char* p = buffer;
  while (millis() < end_time && ((p - buffer) < (sizeof(buffer) - 2))) {
    int b = wifi.read();
    if (b != -1) {
      *p++ = char(b);
    }
    if (b == '\n') {
      break;
    }
  }
  *p = '\0';
}

/** @param timeout timeout in milliseconds */
bool
waitForString(char const * input, unsigned int timeout)
{
  unsigned long const end_time = millis() + timeout; 
  while (millis() < end_time) {
    readLine(timeout);
    if (strcmp(buffer, input) == 0) {
      return true;
    } else {
      debug.println(buffer);
    }
  }

  return false;
}

void
resetWifi()
{
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
  delay(100);
  digitalWrite(ESP8266_CH_PD_PIN, LOW);
  delay(1000);
  digitalWrite(ESP8266_CH_PD_PIN, HIGH);
}

void
setup()
{
  debug.begin(9600);
  debug.print("Hello world!\r\n");

  pinMode(ESP8266_CH_PD_PIN, OUTPUT);

  wifi.begin(9600);
  wifi.listen();

  return;

  debug.print("Resetting ESP8266... ");
  resetWifi();
  debug.print("ok.\r\n");

  if (waitForString("WIFI CONNECTED\r\n", 1000)) {
    debug.print("ESP8266 already connected.\r\n");
    return;
  }

  while (true) {

    debug.print("Talking to ESP8266... ");
    wifi.print("AT+CWMODE=1\r\n");
    if (!waitForString("OK\r\n", 1000)) {
      debug.print("failed.\r\n");
      continue;
    }

    debug.print("ok.\r\nConnecting to network... ");
    wifi.print("AT+CWJAP=\"" SSID "\",\"" PASS "\"\r\n");
    if (!waitForString("OK\r\n", 25000)) {
      debug.print("failed.\r\n");
      continue;
    }

    debug.print("ok.\r\nEnabling DHCP... ");
    wifi.print("AT+CWDHCP=1,1\r\n");
    if (!waitForString("OK\r\n", 1000)) {
      debug.print("failed.\r\n");
      continue;
    }

    debug.print("ok.\r\n");
    break;
  }
}

void
loop()
{ 
  return;
  
  wifi.print(F("AT+CIPSTART=\"UDP\",\"192.168.1.1\",4024\r\n"));

  wifi.print("AT+CIPSEND=1,14\r\n");
  wifi.print("Hello Dolly.\r\n");
  if (!waitForString("OK", 1000)) {
    debug.print("UDP send failed.\r\n");
    return;
  }

  wifi.print("AT+CIPCLOSE=1\r\n");
  if (!waitForString("OK", 1000)) {
    debug.print("UDP close failed.\r\n");
    return;
  }

  delay(1000);
}
