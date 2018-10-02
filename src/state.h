#include "datum.h"
#include <memory>
#include <mutex>
#include <map>
#include <list>

class Node;
class Sensor;

class State
{
public:
	void add(std::shared_ptr<Sensor> sensor, Datum datum);
	void set_boiler_on(bool o);
	void set_heating_enabled(bool e);
	void set_zone_heating_enabled(std::string z, bool e);
	void set_target(std::string z, float t);

	std::optional<float> target(std::string z) const;

	bool heating_enabled() const;
	bool zone_heating_enabled(std::string z) const;

	std::pair<std::shared_ptr<uint8_t[]>, int> get(bool all_values, uint8_t types) const;
	std::optional<Datum> get(std::string zone, std::string sensor_name);

private:
	mutable std::mutex _mutex;
	bool _heating_enabled;
	bool _boiler_on;
	std::map<std::string, bool> _zone_heating_enabled;
	std::map<std::string, float> _target;
	std::map<std::shared_ptr<Sensor>, std::list<Datum>> _data;
};
