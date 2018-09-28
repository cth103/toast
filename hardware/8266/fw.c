#include "ets_sys.h"
#include "osapi.h"
#include "gpio.h"
#include "os_type.h"
#include "user_interface.h"
#include "espconn.h"
#include "driver/i2c_master.h"
#include "driver/uart.h"
#include "driver/ds18b20.h"
#include "driver/gpiolib.h"
#include "driver/dhtxx_lib.h"

#include "config"

#define DHTXX_TASK 0
#define DHTXX_TASK_QUEUE_LENGTH 10
#define DHTXX_SIGNAL_START 0
#define DHTXX_SIGNAL_END_READ 1
os_event_t dhtxx_task_queue[DHTXX_TASK_QUEUE_LENGTH];

LOCAL os_timer_t check_wifi_timer;
LOCAL os_timer_t conversion_timer;
LOCAL os_timer_t broadcast_timer;

struct station_config station_conf;
struct espconn listen_connection;
struct _esp_tcp tcp;
struct espconn broadcast_connection;
struct _esp_udp udp;

#ifdef WITH_DS18B20
uint8_t ds18b20_addr[8];
#endif

struct espconn* dhtxx_connection;

LOCAL void ICACHE_FLASH_ATTR check_wifi_cb(void* arg);

#ifdef WITH_DS18B20
LOCAL void ICACHE_FLASH_ATTR
conversion_cb(void* arg)
{
	int i;
	uint8_t data[12];
	int tries = 5;
	char reply[16];
	struct espconn* conn = arg;

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
	os_sprintf(reply, "%d\r\n", rr);
	espconn_sent(conn, reply, os_strlen(reply));
}
#endif

#ifdef WITH_I2C
LOCAL int ICACHE_FLASH_ATTR
read_sht30(int address, float* temp, float* humidity)
{
	char i2c_data[6];
	int i;

	i2c_master_start();
	i2c_master_writeByte(address << 1);
	if (!i2c_master_checkAck()) {
		return 1;
	}
	i2c_master_writeByte(0x2c);
	i2c_master_checkAck();
	i2c_master_writeByte(0x06);
	i2c_master_checkAck();
	i2c_master_stop();

	os_delay_us(500000);

	i2c_master_start();
	i2c_master_writeByte((address << 1) + 1);
	i2c_master_checkAck();
	for (i = 0; i < 6; ++i) {
		i2c_data[i] = i2c_master_readByte();
		if (i == 5) {
			i2c_master_send_nack();
		} else {
			i2c_master_send_ack();
		}
	}
	i2c_master_stop();

	*temp = ((((i2c_data[0] * 256) + i2c_data[1]) * 175) / 65535.0) - 45;
	*humidity = ((((i2c_data[3] * 256) + i2c_data[4]) * 100) / 65535.0);
	return 0;
}

LOCAL int ICACHE_FLASH_ATTR
pcf8574_set(int value)
{
	i2c_master_start();
	i2c_master_writeByte(PCF8574_ADDRESS);
	if (!i2c_master_checkAck()) {
		return 1;
	}
	i2c_master_writeByte(value);
	if (!i2c_master_checkAck()) {
		return 2;
	}
	i2c_master_stop();
	return 0;
}
#endif

LOCAL void ICACHE_FLASH_ATTR
gpio_off(void* arg)
{
#ifdef WITH_I2C
	char reply[32];
	int r;
	r = pcf8574_set(0);
	if (r && arg) {
		os_sprintf(reply, "error 1\r\n");
		espconn_sent((struct espconn *) arg, reply, os_strlen(reply));
		return;
	}
#else
	gpio_output_set(0, 1 << RELAY_GPIO, 1 << RELAY_GPIO, 0);
#endif
}

LOCAL void ICACHE_FLASH_ATTR
receive_cb(void* arg, char* data, unsigned short length)
{
	int r;
#ifdef WITH_I2C
	float temp;
	float humidity;
	char reply[32];
#endif

	if (os_strncmp(data, "temp", 4) == 0) {
		/* Report current temperature in Celsius multiplied by 10000 */
#ifdef WITH_DS18B20
		ds_init();
		while ((r = ds_search(ds18b20_addr))) {
			if (crc8(ds18b20_addr, 7) != ds18b20_addr[7]) {
				/* Bad CRC */
				continue;
			}
			ets_uart_printf(
				"Found device %02x%02x%02x%02x%02x%02x%02x%02x\r\n",
				ds18b20_addr[0], ds18b20_addr[1], ds18b20_addr[2], ds18b20_addr[3], ds18b20_addr[4], ds18b20_addr[5], ds18b20_addr[6], ds18b20_addr[7]
				);
			if (ds18b20_addr[0] == 0x10 || ds18b20_addr[0] == 0x28) {
				ets_uart_printf("Found DS18B20\r\n");
				reset();
				select(ds18b20_addr);
				write(DS1820_CONVERT_T, 1);
				os_timer_setfn(&conversion_timer, (os_timer_func_t *) conversion_c b, arg);
				os_timer_arm(&conversion_timer, 750, false);
			}
		}
#endif
#ifdef WITH_I2C
		r = read_sht30((length > 4 && data[4] == '2') ? SHT30_2_ADDRESS : SHT30_1_ADDRESS, &temp, &humidity);
		if (r) {
			os_sprintf(reply, "error 1\r\n");
			espconn_sent((struct espconn *) arg, reply, os_strlen(reply));
			return;
		}
		os_sprintf(reply, "%d\r\n", (int) temp * 10000);
		espconn_sent((struct espconn *) arg, reply, os_strlen(reply));

#endif
	} else if (os_strncmp(data, "off", 3) == 0) {
		gpio_off(arg);
	} else if (os_strncmp(data, "on", 2) == 0) {
#ifdef WITH_I2C
		r = pcf8574_set(1 << RELAY_CHANNEL);
		if (r) {
			os_sprintf(reply, "error 1\r\n");
			espconn_sent((struct espconn *) arg, reply, os_strlen(reply));
			return;
		}
#else
		gpio_output_set(1 << RELAY_GPIO, 0, 1 << RELAY_GPIO, 0);
#endif
	}
	else if (os_strncmp(data, "humidity", 8) == 0) {
#ifdef WITH_DHTXX
		dhtxx_connection = (struct espconn*) arg;
		system_os_post(DHTXX_TASK, DHTXX_SIGNAL_START, 0);
#endif
#ifdef WITH_I2C
		r = read_sht30((length > 8 && data[8] == '2') ? SHT30_2_ADDRESS : SHT30_1_ADDRESS, &temp, &humidity);
		if (r) {
			os_sprintf(reply, "error 1\r\n");
			espconn_sent((struct espconn *) arg, reply, os_strlen(reply));
			return;
		}
		os_sprintf(reply, "%d\r\n", (int) humidity * 10);
		espconn_sent((struct espconn *) arg, reply, os_strlen(reply));
#endif
	}
}

LOCAL void ICACHE_FLASH_ATTR
reconnect_cb(void* arg, sint8 error)
{

}

LOCAL void ICACHE_FLASH_ATTR
disconnect_cb(void* arg)
{

}

LOCAL void ICACHE_FLASH_ATTR
connect_cb(void* arg)
{
	int r;

	struct espconn* conn = arg;
	espconn_regist_recvcb(conn, receive_cb);
	espconn_regist_reconcb(conn, reconnect_cb);
	espconn_regist_disconcb(conn, disconnect_cb);
}

LOCAL void ICACHE_FLASH_ATTR
broadcast_cb(void* arg)
{
	struct ip_addr address;
	struct ip_info ipi;
	uint8_t mac_address[6];
	char buffer[64];

	wifi_get_ip_info(0, &ipi);
	address.addr = ipi.ip.addr;
	address.addr |= ~ipi.netmask.addr;

	wifi_get_macaddr(0, mac_address);

	broadcast_connection.type = ESPCONN_UDP;
	broadcast_connection.state = ESPCONN_NONE;
	broadcast_connection.proto.udp = &udp;
	udp.remote_port = BROADCAST_PORT;
	udp.local_port = BROADCAST_PORT;
	memcpy(udp.remote_ip, &address,4);
	broadcast_connection.reverse = 0;
	espconn_create(&broadcast_connection);

	os_sprintf(
		buffer,
		"Hello heating %02x%02x%02x%02x%02x%02x",
		mac_address[0],
		mac_address[1],
		mac_address[2],
		mac_address[3],
		mac_address[4],
		mac_address[5]
		);

	espconn_sent(&broadcast_connection, buffer, os_strlen(buffer));

	espconn_delete(&broadcast_connection);
}

LOCAL void ICACHE_FLASH_ATTR
check_wifi_cb(void* arg)
{
	sint8 ret;
	uint8_t mac_address[6];

	os_timer_disarm(&check_wifi_timer);

	struct ip_info ipi;
	wifi_get_ip_info(0, &ipi);
	if (wifi_station_get_connect_status() == STATION_GOT_IP && ipi.ip.addr != 0) {
		/* Start listening for requests */
		wifi_get_macaddr(0, mac_address);
		ets_uart_printf(
			"Hello world: connected with IP %d.%d.%d.%d, MAC %02x:%02x:%02x:%02x:%02x:%02x.\r\n",
			ipi.ip.addr & 0xff,
			(ipi.ip.addr & 0xff00) >> 8,
			(ipi.ip.addr & 0xff0000) >> 16,
			(ipi.ip.addr & 0xff000000) >> 24,
			mac_address[0],
			mac_address[1],
			mac_address[2],
			mac_address[3],
			mac_address[4],
			mac_address[5]
			);
		listen_connection.type = ESPCONN_TCP;
		listen_connection.state = ESPCONN_NONE;
		listen_connection.proto.tcp = &tcp;
		listen_connection.proto.tcp->local_port = LISTEN_PORT;
		espconn_regist_connectcb(&listen_connection, connect_cb);
		ret = espconn_accept(&listen_connection);

		/* Start broadcasting to announce our presence */
		os_timer_setfn(&broadcast_timer, (os_timer_func_t *) broadcast_cb, (void *) 0);
		os_timer_arm(&broadcast_timer, 6000, true);
	} else {
		if (wifi_station_get_connect_status() == STATION_WRONG_PASSWORD ||
		    wifi_station_get_connect_status() == STATION_NO_AP_FOUND ||
		    wifi_station_get_connect_status() == STATION_CONNECT_FAIL)
		{
			ets_uart_printf("Connection to wireless failed.\r\n");
		} else {
			os_timer_setfn(&check_wifi_timer, (os_timer_func_t *) check_wifi_cb, (void *) 0);
			os_timer_arm(&check_wifi_timer, 100, true);
		}
	}
}

#ifdef WITH_DHTXX
LOCAL void ICACHE_FLASH_ATTR
dhtxx_intr_handler()
{
	uint32 gpio_status = GPIO_REG_READ(GPIO_STATUS_ADDRESS);

	// if the interrupt was by GPIO_DHTXX -> Execute dhtxx handler
	if (gpio_status & GPIO_Pin(DHTXX_GPIO)) {
		dhtxx_gpio_intr_handler(gpio_status);
	}
}

LOCAL void ICACHE_FLASH_ATTR
dhtxx_loop(os_event_t *events)
{
	char reply[16];
	int e;
	switch (events->sig) {
        case DHTXX_SIGNAL_START:
		dhtxx_start_read(events->par);
		break;
	case DHTXX_SIGNAL_END_READ:
		e = dhtxx_error();
		if (!e) {
			os_sprintf(reply, "%d\r\n", dht22_get_rh());
		} else {
			os_sprintf(reply, "error %d\r\n", e);
		}
		espconn_sent(dhtxx_connection, reply, strlen(reply));
		break;
	}
}
#endif

void ICACHE_FLASH_ATTR user_init()
{
	uart_init(BIT_RATE_115200, BIT_RATE_115200);
	gpio_init();

#if (RELAY_GPIO==2)
	PIN_FUNC_SELECT(PERIPHS_IO_MUX_GPIO2_U, FUNC_GPIO2);
#endif

#ifdef WITH_DHTXX
	dhtxx_init(DHTXX_GPIO, DHTXX_TASK, DHTXX_SIGNAL_END_READ);
	ETS_GPIO_INTR_DISABLE();
	ETS_GPIO_INTR_ATTACH(dhtxx_intr_handler, DHTXX_GPIO);
	ETS_GPIO_INTR_ENABLE();
	system_os_task(dhtxx_loop, DHTXX_TASK, dhtxx_task_queue, DHTXX_TASK_QUEUE_LENGTH);
#endif

#ifdef WITH_I2C
	i2c_master_gpio_init();
	i2c_master_init();
	os_delay_us(100000);
#endif

	gpio_off(NULL);

	wifi_set_opmode(STATIONAP_MODE);
	os_memcpy(&station_conf.ssid, ssid, 32);
	os_memcpy(&station_conf.password, password, 32);
	wifi_station_set_config(&station_conf);

	os_timer_disarm(&check_wifi_timer);
	os_timer_setfn(&check_wifi_timer, (os_timer_func_t *) check_wifi_cb, (void *) 0);
	os_timer_arm(&check_wifi_timer, 100, true);
}
