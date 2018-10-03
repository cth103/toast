#include "esp8266_node.h"
#include "config.h"
#include "toast_socket.h"
#include <iostream>

using std::string;
using std::cout;

Datum
ESP8266Node::get(string id) const
{
	Config* config = Config::instance();
	Socket socket(config->sensor_timeout());
	socket.connect(boost::asio::ip::tcp::endpoint(_ip, config->sensor_port()));
	char buffer[64];
	snprintf(buffer, sizeof(buffer), "%s\r\n", id.c_str());
	socket.write(reinterpret_cast<uint8_t*>(buffer), strlen(buffer));
	socket.read(reinterpret_cast<uint8_t*>(buffer), sizeof(buffer));
	if (id == "temp" || id == "temp2") {
		return Datum(atof(buffer) / 10000);
	} else {
		return Datum(atof(buffer) / 10);
	}
}
