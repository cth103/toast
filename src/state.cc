#include "state.h"
#include "node.h"
#include "types.h"
#include "sensor.h"
#include "util.h"
#include "config.h"
#include "log.h"
#include "period.h"
#include <iostream>
#include <set>

using std::shared_ptr;
using std::cout;
using std::list;
using std::string;
using std::scoped_lock;
using std::set;
using std::pair;
using std::optional;

void
State::add(shared_ptr<Sensor> sensor, Datum datum)
{
	scoped_lock lm(_mutex);
	if (_data.find(sensor) == _data.end()) {
		_data[sensor] = list<Datum>();
	}
	_data[sensor].push_back(datum);
}

static bool
all_or(uint8_t types, uint8_t t)
{
	return types == OP_ALL || types == t;
}

static void
put_data(uint8_t*& p, bool all_values, list<Datum> const & data)
{
	if (all_values) {
		put_int16(p, data.size());
		for (auto k: data) {
			put_datum(p, k);
		}
	} else if (data.size() == 1) {
		put_int16(p, 1);
		put_datum(p, data.back());
	} else {
		put_int16(p, 0);
	}
}

pair<shared_ptr<uint8_t[]>, int>
State::get(bool all_values, uint8_t types) const
{
	scoped_lock lm(_mutex);

	/* XXX: check this is big enough */
	shared_ptr<uint8_t[]> data(new uint8_t[256 * 1024]);
	uint8_t* p = data.get();

	*p++ = types;

	/* Find all our zones */
	set<string> zones;
	for (auto i: _data) {
		zones.insert(i.first->zone());
	}

	for (auto i: Config::instance()->hidden_zones()) {
		zones.erase(i);
	}

	*p++ = zones.size();

	for (auto i: zones) {
		put_string(p, i);
	}

	for (auto i: zones) {
		if (all_or(types, OP_TEMPERATURES)) {
			bool done = false;
			for (auto j: _data) {
				if (j.first->zone() == i && j.first->name() == "temperature") {
					put_data(p, all_values, j.second);
					done = true;
				}
			}
			if (!done) {
				put_int16(p, 0);
			}
		}
                if (all_or(types, OP_HUMIDITIES)) {
			bool done = false;
			for (auto j: _data) {
				if (j.first->zone() == i && j.first->name() == "humidity") {
					put_data(p, all_values, j.second);
					done = true;
				}
			}
			if (!done) {
				put_int16(p, 0);
			}
		}
		if (all_or(types, OP_ACTUATORS)) {
			int N = 0;
			for (auto j: Node::all()) {
				for (auto k: j->actuators()) {
					if (k->zone() == i) {
						++N;
					}
				}
			}
			*p++ = N;
			for (auto j: Node::all()) {
				for (auto k: j->actuators()) {
					if (k->zone() == i) {
						put_string(p, k->name());
						*p++ = k->get().value_or(false) ? 1 : 0;
					}
				}
			}
		}
	}

	if (all_or(types, OP_PERIODS)) {
		*p++ = _periods.size();
		for (auto i: _periods) {
			i.get(p);
		}
	}

	if (all_or(types, OP_RULES)) {
		*p++ = _rules.size();
		for (auto i: _rules) {
			i.get(p);
		}
	}

	ptrdiff_t length = p - data.get();
	shared_ptr<uint8_t[]> out(new uint8_t[length]);
	memcpy(out.get(), data.get(), length);
	return make_pair(out, length);
}

optional<Datum>
State::get(string zone, string sensor_name)
{
	for (auto i: _data) {
		if (i.first->zone() == zone && i.first->name() == sensor_name) {
			if (i.second.empty()) {
				return optional<Datum>();
			}
			return i.second.back();
		}
	}

	return optional<Datum>();
}

list<Rule>
State::rules() const
{
	scoped_lock lm(_mutex);
	return _rules;
}

list<Period>
State::periods() const
{
	scoped_lock lm(_mutex);
	return _periods;
}

/** @return A copy of this object with only the most recent sensor reading for each sensor */
State
State::thin_clone() const
{
	State s;
	s._periods = _periods;
	s._rules = _rules;
	for (auto i: _data) {
		list<Datum> d;
		if (!i.second.empty()) {
			d.push_back(i.second.back());
		}
		s._data[i.first] = d;
	}
	return s;
}

State::State()
{

}

State::State(State const& other)
{
	_periods = other._periods;
	_rules = other._rules;
	_data = other._data;
}


void
State::set_periods(list<Period> periods)
{
	scoped_lock lm(_mutex);
	_periods = periods;
}
