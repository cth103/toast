#include "datum.h"
#include "rule.h"
#include <memory>
#include <mutex>
#include <map>
#include <list>

class Node;
class Sensor;
class Period;

class State
{
public:
	State();

	void add(std::shared_ptr<Sensor> sensor, Datum datum);
	void set_periods(std::list<Period> periods);

	std::list<Rule> rules() const;
	std::list<Period> periods();
	std::map<std::shared_ptr<Sensor>, std::list<Datum>> data() const;

	std::pair<std::shared_ptr<uint8_t[]>, int> get(bool all_values, uint8_t types);
	std::optional<Datum> get(std::string zone, std::string sensor_name);

	State thin_clone();

private:
	State(State &);
	float target_unlocked(std::string z) const;
	std::list<Period> periods_unlocked();

	mutable std::mutex _mutex;
	std::list<Period> _periods;
	std::list<Rule> _rules;
	std::map<std::shared_ptr<Sensor>, std::list<Datum>> _data;
};
