#include <string>

class Config
{
public:
	int sensor_port() const {
		return _sensor_port;
	}
	int broadcast_port() const {
		return _broadcast_port;
	}
	int gather_interval() const {
		return _gather_interval;
	}
	int control_interval() const {
		return _control_interval;
	}
	std::string log_directory() const {
		return _log_directory;
	}
	int sensor_timeout() const {
		return _sensor_timeout;
	}
	int server_port() const {
		return _server_port;
	}
	float hysteresis() const {
		return _hysteresis;
	}
	float default_target() const {
		return _default_target;
	}
	int boiler_gpio() const {
		return _boiler_gpio;
	}

	static Config* instance();

private:
	Config();

	int _sensor_port;
	int _broadcast_port;
	int _gather_interval;
	int _control_interval;
	std::string _log_directory;
	int _sensor_timeout;
	int _server_port;
	float _hysteresis;
	float _default_target;
	int _boiler_gpio;

	static Config* _instance;
};
