#include "config.h"
#include "compose.hpp"
#include "log.h"
#include <boost/algorithm/string.hpp>
#include <string>
#include <stdexcept>
#include <iostream>

using std::cout;
using std::string;
using std::optional;
using std::vector;
using std::runtime_error;

Config* Config::_instance = 0;

Config::Config(optional<boost::filesystem::path> file)
	: _sensor_port(9142)
	, _broadcast_port(9143)
	, _gather_interval(5)
	, _control_interval(1)
	, _log_directory("/var/log/toast")
	, _sensor_timeout(30)
	, _server_port(9999)
	, _hysteresis(0.2)
	, _default_target(19)
	, _boiler_gpio(2)
	, _humidity_rising_threshold(8)
	, _humidity_falling_threshold(5)
	, _log_types(Log::STARTUP)
	, _fan_off_delay(300)
	, _max_datum_age(60*60*24*7)
{
	FILE* f = 0;

	if (file) {
		f = fopen(file->string().c_str(), "r");
	} else {
		char config_file[256];
		snprintf(config_file, sizeof(config_file), "%s/.config/toastd", getenv("HOME"));
		f = fopen(config_file, "r");
	}

	if (!f) {
		return;
	}

	while (!feof(f)) {
		char* buf = 0;
		size_t n = 0;
		ssize_t const r = getline(&buf, &n, f);
		if (r == -1) {
			break;
		}
		string line(buf);
		free(buf);
		size_t const space = line.find(" ");
		if (space != string::npos && line.length() > 1) {
			string const key = line.substr(0, space);
			string const value = line.substr(space + 1, line.length() - space - 2);
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
			} else if (key == "default_target") {
				_default_target = atof(value.c_str());
			} else if (key == "boiler_gpio") {
				_boiler_gpio = atoi(value.c_str());
			} else if (key == "humidity_rising_threshold") {
				_humidity_rising_threshold = atof(value.c_str());
			} else if (key == "humidity_falling_threshold") {
				_humidity_falling_threshold = atof(value.c_str());
			} else if (key == "log_types") {
				_log_types = atoi(value.c_str());
			} else if (key == "hidden_zone") {
				_hidden_zones.push_back(value);
			} else if (key == "fan_off_delay") {
				_fan_off_delay = atoi(value.c_str());
			} else if (key == "max_datum_age") {
				_max_datum_age = atoi(value.c_str());
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
