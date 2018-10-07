#include <boost/filesystem.hpp>
#include <string>
#include <list>

class config_test;

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
	float humidity_rising_threshold() const {
		return _humidity_rising_threshold;
	}
	float humidity_falling_threshold() const {
		return _humidity_falling_threshold;
	}
	int log_types() const {
		return _log_types;
	}
	std::list<std::string> hidden_zones() const {
		return _hidden_zones;
	}

	static Config* instance();

private:
	friend class ::config_test;

	Config(std::optional<boost::filesystem::path> file = std::optional<boost::filesystem::path>());

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
	float _humidity_rising_threshold;
	float _humidity_falling_threshold;
	int _log_types;
	std::list<std::string> _hidden_zones;

	static Config* _instance;
};
