#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "user_interface.h"
#include "espconn.h"
#include "driver/uart.h"

char const ssid[32] = "TALKTALK227CC2-2G";
char const password[32] = "3N7FEUR9";
char const server_ip[4] = {192, 168, 1, 5};
int const server_port = 9999;

LOCAL os_timer_t check_wifi_timer;

struct station_config station_conf;
struct espconn connection;
struct _esp_tcp tcp;

LOCAL void ICACHE_FLASH_ATTR
connect_cb(void* arg)
{
	ets_uart_printf("Connected to server.\r\n");
	espconn_send(&connection, "Cockwomble\r\n", 13);
}

LOCAL void ICACHE_FLASH_ATTR
reconnect_cb(void* arg, sint8 err)
{
	/* Lost tcp connection */
}

LOCAL void ICACHE_FLASH_ATTR
check_wifi_cb(void* arg)
{
	os_timer_disarm(&check_wifi_timer);

	struct ip_info ipi;
	wifi_get_ip_info(0, &ipi);
	if (wifi_station_get_connect_status() == STATION_GOT_IP && ipi.ip.addr != 0) {
		ets_uart_printf(
			"Hello world: connected with IP %d.%d.%d.%d.\r\n",
			ipi.ip.addr & 0xff,
			(ipi.ip.addr & 0xff00) >> 8,
			(ipi.ip.addr & 0xff0000) >> 16,
			(ipi.ip.addr & 0xff000000) >> 24
			);
		connection.proto.tcp = &tcp;
		connection.type = ESPCONN_TCP;
		connection.state = ESPCONN_NONE;

		os_memcpy(connection.proto.tcp->remote_ip, server_ip, 4);
		connection.proto.tcp->remote_port = server_port;
		connection.proto.tcp->local_port = espconn_port();
		espconn_regist_connectcb(&connection, connect_cb);
		espconn_regist_reconcb(&connection, reconnect_cb); // register reconnect callback as error handler
		espconn_connect(&connection);
	} else {
		if (wifi_station_get_connect_status() == STATION_WRONG_PASSWORD ||
		    wifi_station_get_connect_status() == STATION_NO_AP_FOUND ||
		    wifi_station_get_connect_status() == STATION_CONNECT_FAIL)
		{
			ets_uart_printf("Connection to wireless failed.\r\n");
		} else {
			os_timer_setfn(&check_wifi_timer, (os_timer_func_t *) check_wifi_cb, (void *) 0);
			os_timer_arm(&check_wifi_timer, 100, 1);
		}
	}
}

void ICACHE_FLASH_ATTR user_init()
{
	uart_init(BIT_RATE_115200, BIT_RATE_115200);

	wifi_set_opmode(STATION_MODE);
	os_memcpy(&station_conf.ssid, ssid, 32);
	os_memcpy(&station_conf.password, password, 32);
	wifi_station_set_config(&station_conf);

	os_timer_disarm(&check_wifi_timer);
	os_timer_setfn(&check_wifi_timer, (os_timer_func_t *) check_wifi_cb, (void *) 0);
	os_timer_arm(&check_wifi_timer, 100, 1);
}
