
#include <SoftwareSerial.h>

#define SSID "PlusnetWireless998FC7"
#define PASS "E5383F2817"

#define ESP8266_TX_PIN 0
#define ESP8266_RX_PIN 2
#define DEBUG_TX_PIN 3
#define DEBUG_RX_PIN 4

SoftwareSerial wifi(ESP8266_RX_PIN, ESP8266_TX_PIN);
SoftwareSerial debug(DEBUG_RX_PIN, DEBUG_TX_PIN);

/** @param timeout timeout in milliseconds */
bool
waitForString(char* input, unsigned int timeout)
{
	unsigned long const end_time = millis() + timeout;
	int const length = strlen(input);
  wifi.listen();

	int i = 0;
	while (millis() < end_time) {

		int b = wifi.read();
		if (b != -1 && b != '\r' && b != '\n') {
			if (b == input[i]) {
				++i;
				if (i == length) {
					return true;
				}
			} else {
				i = 0;
			}
		}
	}

	return false;
}

void
setup()
{
	debug.begin(9600);
	debug.print("Hello world!\r\n");

	wifi.begin(9600);

  debug.print("Waiting for ESP8266... ");
  while (true) {
    if (waitForString("ready", 1000)) {
      break;
    }
  }
  debug.print("ok.\r\n");
  
  if (waitForString("WIFI CONNECTED", 1000)) {
    debug.print("ESP8266 already connected.\r\n");
    return;
  }

  while (true) {

    debug.print("Talking to ESP8266... ");
    wifi.print("AT+CWMODE=1\r\n");
    if (!waitForString("OK", 1000)) {
      debug.print("failed.\r\n");
      continue;
    }
    
	  debug.print("ok.\r\nConnecting to network... ");
	  wifi.print("AT+CWJAP=\"" SSID "\",\"" PASS "\"\r\n");
	  if (!waitForString("OK", 25000)) {
	  	debug.print("failed.\r\n");
		  continue;
		}

    debug.print("ok.\r\nEnabling DHCP... ");
    wifi.print("AT+CWDHCP=1,1");
    //if (!waitForString("OK", 1000)) {
    //  debug.print("failed.\r\n");
    //  continue;
    //}
   
		debug.print("ok.\r\n");
    break;
  }
}

void
loop()
{  
  wifi.print(F("AT+CIPSTART=\"UDP\",\"192.168.1.1\",4024\r\n"));

  wifi.print("AT+CIPSEND\r\n");
  if (!waitForString(">", 1000)) {
    debug.print("Failed to start UDP send.");
    return;
  }

  wifi.print("Hello Dolly.\r\n");

  delay(1000);
}
