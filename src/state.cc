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

	time_t const early = time(0) - Config::instance()->max_datum_age();
	list<Datum>& dl = _data[sensor];
	while (!dl.empty() && dl.front().time() < early) {
		dl.pop_front();
	}
}

static bool
all_or(uint8_t types, uint8_t t)
{
	return types == OP_ALL || types == t;
}

static void
put_data(uint8_t*& p, uint8_t* e, bool all_values, list<Datum> const & data)
{
	if (all_values) {
		put_int16(p, e, data.size());
		for (auto k: data) {
			put_datum(p, e, k);
		}
	} else if (data.size() == 1) {
		put_int16(p, e, 1);
		put_datum(p, e, data.back());
	} else {
		put_int16(p, e, 0);
	}
}

pair<shared_ptr<uint8_t[]>, int>
State::get(bool all_values, uint8_t types)
{
	scoped_lock lm(_mutex);

	/* Find all our zones */
	set<string> zones;
	for (auto i: _data) {
		zones.insert(i.first->zone());
	}

	for (auto i: Config::instance()->hidden_zones()) {
		zones.erase(i);
	}

	Config* config = Config::instance();

	/* Space required for readings assuming 2 sensors per zone */
	/* XXX: check this is big enough */
	size_t required = (config->max_datum_age() * zones.size() * 2 * Datum::size) / config->gather_interval();
	/* Add some for everything else */
	required += 16384;
	shared_ptr<uint8_t[]> data(new uint8_t[required]);
	uint8_t* p = data.get();
	uint8_t* e = p + required;

	*p++ = types;
	*p++ = zones.size();

	for (auto i: zones) {
		put_string(p, e, i);
	}

	for (auto i: zones) {
		if (all_or(types, OP_TEMPERATURES)) {
			bool done = false;
			for (auto j: _data) {
				if (j.first->zone() == i && j.first->name() == "temperature") {
					put_data(p, e, all_values, j.second);
					done = true;
				}
			}
			if (!done) {
				put_int16(p, e, 0);
			}
		}
                if (all_or(types, OP_HUMIDITIES)) {
			bool done = false;
			for (auto j: _data) {
				if (j.first->zone() == i && j.first->name() == "humidity") {
					put_data(p, e, all_values, j.second);
					done = true;
				}
			}
			if (!done) {
				put_int16(p, e, 0);
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
						put_string(p, e, k->name());
						*p++ = k->get().value_or(false) ? 1 : 0;
					}
				}
			}
		}
	}

	if (all_or(types, OP_PERIODS)) {
		list<Period> per = periods_unlocked();
		*p++ = per.size();
		for (auto i: per) {
			i.get(p, e);
		}
	}

	if (all_or(types, OP_RULES)) {
		*p++ = _rules.size();
		for (auto i: _rules) {
			i.get(p, e);
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
State::periods()
{
	scoped_lock lm(_mutex);
	return periods_unlocked();
}

list<Period>
State::periods_unlocked()
{
	time_t const now = time(0);
	list<Period> old = _periods;
	_periods.clear();
	for (auto i: old) {
		if (i.to() >= now) {
			_periods.push_back(i);
		}
	}
	return _periods;
}

/** @return A copy of this object with only the most recent sensor reading for each sensor */
State
State::thin_clone()
{
	State s;
	s._periods = periods();
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

State::State(State& other)
{
	_periods = other.periods();
	_rules = other._rules;
	_data = other._data;
}


void
State::set_periods(list<Period> periods)
{
	scoped_lock lm(_mutex);
	_periods = periods;
}
