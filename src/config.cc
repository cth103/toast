#include "config.h"
#include "compose.hpp"
#include <string>
#include <stdexcept>
#include <iostream>

using std::cout;
using std::string;
using std::runtime_error;

Config* Config::_instance = 0;

Config::Config()
	: _sensor_port(9142)
	, _broadcast_port(9143)
	, _gather_interval(5)
	, _control_interval(1)
	, _log_directory("/var/log/toastd")
	, _sensor_timeout(30)
	, _server_port(9999)
	, _hysteresis(0.2)
{
	char config_file[256];
	snprintf(config_file, sizeof(config_file), "%s/.config/toastd", getenv("HOME"));
	FILE* f = fopen(config_file, "r");
	while (!feof(f)) {
		char* buf = 0;
		size_t n = 0;
		getline(&buf, &n, f);
		string line(buf);
		free(buf);
		size_t const space = line.find(" ");
		if (space != string::npos && line.length() > 1) {
			string const key = line.substr(0, space);
			string const value = line.substr(space + 1, line.length() - space - 1);
			if (key == "sensor_port") {
				_sensor_port = atoi(value.c_str());
			} else if (key == "broadcast_port") {
				_broadcast_port = atoi(value.c_str());
			} else if (key == "gather_interval") {
				_gather_interval = atoi(value.c_str());
			} else if (key == "control_interval") {
				_control_interval = atoi(value.c_str());
			} else if (key == "log_directory") {
				_log_directory = value;
			} else if (key == "sensor_timeout") {
				_sensor_timeout = atoi(value.c_str());
			} else if (key == "server_port") {
				_server_port = atoi(value.c_str());
			} else if (key == "hysteresis") {
				_hysteresis = atof(value.c_str());
			} else {
				throw runtime_error(String::compose("Unknown key %1 in configuration", key));
			}
		}
	}

	fclose(f);
}

Config*
Config::instance()
{
	if (!_instance) {
		_instance = new Config();
	}
	return _instance;
}