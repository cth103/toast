#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "user_interface.h"
#include "espconn.h"
#include "driver/uart.h"
#include "driver/ds18b20.h"

char const ssid[32] = "TALKTALK227CC2-2G";
char const password[32] = "3N7FEUR9";
char const server_ip[4] = {192, 168, 1, 5};
int const server_port = 9999;

LOCAL os_timer_t check_wifi_timer;
LOCAL os_timer_t conversion_timer;

struct station_config station_conf;
struct espconn connection;
struct _esp_tcp tcp;
uint8_t ds18b20_addr[8];

LOCAL void ICACHE_FLASH_ATTR check_wifi_cb(void* arg);

LOCAL void ICACHE_FLASH_ATTR
conversion_cb(void* arg)
{
	int i;
	uint8_t data[12];
	int tries = 5;

	while (tries) {
		reset();
		select(ds18b20_addr);
		write(DS1820_READ_SCRATCHPAD, 0);
		for (i = 0; i < 9; ++i) {
			data[i] = read();
		}
		if (crc8(data, 8) == data[8]) {
			break;
		}
		--tries;
	}

        int rr = data[1] << 8 | data[0];
        if (rr & 0x8000) {
		/* sign extend */
		rr |= 0xffff0000;
	}
	/* Each bit is 1/16th of a degree C */
	rr = rr * 10000 / 16;
	ets_uart_printf("Read %d\n", rr);
}

LOCAL void ICACHE_FLASH_ATTR
connect_cb(void* arg)
{
	int r;

	ets_uart_printf("Connected to server.\r\n");

	ds_init();
	while ((r = ds_search(ds18b20_addr))) {
		if (crc8(ds18b20_addr, 7) != ds18b20_addr[7]) {
			/* Bad CRC */
			continue;
		}
		ets_uart_printf(
			"Found 1wb device %02x%02x%02x%02x%02x%02x%02x%02x\r\n",
			ds18b20_addr[0], ds18b20_addr[1], ds18b20_addr[2], ds18b20_addr[3], ds18b20_addr[4], ds18b20_addr[5], ds18b20_addr[6], ds18b20_addr[7]
			);
		if (ds18b20_addr[0] == 0x10 || ds18b20_addr[0] == 0x28) {
			ets_uart_printf("Found DS18B20\r\n");
			reset();
			select(ds18b20_addr);
			write(DS1820_CONVERT_T, 1);
			os_timer_setfn(&conversion_timer, (os_timer_func_t *) conversion_cb, (void *) 0);
			os_timer_arm(&conversion_timer, 750, 0);
		}

	}

	espconn_send(&connection, "Cockwomble\r\n", 13);
}

LOCAL void ICACHE_FLASH_ATTR
reconnect_cb(void* arg, sint8 err)
{
	os_timer_setfn(&check_wifi_timer, (os_timer_func_t *) check_wifi_cb, (void *) 0);
	os_timer_arm(&check_wifi_timer, 100, 1);
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
		espconn_regist_reconcb(&connection, reconnect_cb);
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
