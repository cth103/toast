#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "driver/uart.h"
#include "user_interface.h"

char const ssid[32] = "TALKTALK227CC2-2G";
char const password[32] = "3N7FEUR9";

LOCAL os_timer_t hello_timer;

struct station_config station_conf;

void
hello_cb(void* arg)
{
	struct ip_info ipi;
	wifi_get_ip_info(0, &ipi);
	ets_uart_printf(
		"Hello world: IP is %d.%d.%d.%d.\r\n",
		ipi.ip.addr & 0xff,
		(ipi.ip.addr & 0xff00) >> 8,
		(ipi.ip.addr & 0xff0000) >> 16,
		(ipi.ip.addr & 0xff000000) >> 24
		);
}

void ICACHE_FLASH_ATTR user_init()
{
	uart_init(BIT_RATE_115200, BIT_RATE_115200);

	os_timer_disarm(&hello_timer);
	os_timer_setfn(&hello_timer, (os_timer_func_t *) hello_cb, (void *) 0);
	os_timer_arm(&hello_timer, 1000, 1);

	wifi_set_opmode(STATION_MODE);
	os_memcpy(&station_conf.ssid, ssid, 32);
	os_memcpy(&station_conf.password, password, 32);
	wifi_station_set_config(&station_conf);
	wifi_station_connect();
}
