#include "state.h"
#include "node.h"
#include "types.h"
#include "sensor.h"
#include "util.h"
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

bool
State::heating_enabled() const
{
	scoped_lock lm(_mutex);
	return _heating_enabled;
}

void
State::set_heating_enabled(bool e)
{
	scoped_lock lm(_mutex);
	_heating_enabled = e;
}

void
State::set_boiler_on(bool s)
{
	scoped_lock lm(_mutex);
	_boiler_on = s;
}

void
State::set_zone_heating_enabled(string z, bool e)
{
	scoped_lock lm(_mutex);
	_zone_heating_enabled[z] = e;
}

bool
State::zone_heating_enabled(string z) const
{
	scoped_lock lm(_mutex);
	auto i = _zone_heating_enabled.find(z);
	assert(i != _zone_heating_enabled.end());
	return i->second;
}

void
State::set_target(string z, float t)
{
	scoped_lock lm(_mutex);
	_target[z] = t;
}

static bool
all_or(uint8_t types, uint8_t t)
{
	return types == OP_ALL || types == t;
}

static
void put_data(uint8_t*& p, bool all_values, list<Datum> const & data)
{
	if (all_values) {
		put_int16(p, data.size());
		for (auto k: data) {
			put(p, k);
		}
	} else if (data.size() == 1) {
		put_int16(p, 1);
		put(p, data.back());
	} else {
		put(p, 0);
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
	if (all_or(types, OP_HEATING_ENABLED)) {
		*p++ = _heating_enabled ? 1 : 0;
	}
	if (all_or(types, OP_BOILER_ON)) {
		*p++ = _boiler_on ? 1 : 0;
	}

	/* Find all our zones */
	set<string> zones;
	for (auto i: _data) {
		zones.insert(i.first->zone());
	}

	for (auto i: zones) {
		put(p, i);
	}

	for (auto i: zones) {
                if (all_or(types, OP_ZONE_HEATING_ENABLED)) {
			auto j = _zone_heating_enabled.find(i);
			if (j != _zone_heating_enabled.end()) {
				*p++ = j->second ? 1 : 0;
			} else {
				*p++ = 0;
			}
		}
                if (all_or(types, OP_TARGET)) {
			auto j = _target.find(i);
			if (j != _target.end()) {
				put_float(p, j->second);
			} else {
				put_float(p, 0.0);
			}
		}
		if (all_or(types, OP_TEMPERATURES)) {
			for (auto j: _data) {
				if (j.first->zone() == i && j.first->name() == "temperature") {
					put_data(p, all_values, j.second);
				}
			}
		}
                if (all_or(types, OP_HUMIDITIES)) {
			for (auto j: _data) {
				if (j.first->zone() == i && j.first->name() == "humidity") {
					put_data(p, all_values, j.second);
				}
			}
		}
		if (all_or(types, OP_ACTUATORS)) {
			for (auto j: Node::all()) {
				for (auto k: j->actuators()) {
					put(p, k->name());
					*p++ = k->state().value_or(false) ? 1 : 0;
				}
			}
		}

		*p++ = _rules.size();
		for (auto i: _rules) {
			i.get(p);
		}

		if (types == OP_ALL) {
			bool any_zone_heating_enabled = false;
			for (auto i: _zone_heating_enabled) {
				if (i.second) {
					any_zone_heating_enabled = true;
				}
			}

			string explanation;

			if (_boiler_on) {
				if (_heating_enabled) {
					explanation = "Heating";
				} else {
					explanation = "Heating to programmed target";
				}
			} else {
				if (_heating_enabled) {
					if (any_zone_heating_enabled) {
						explanation = "Target temperatures reached";
					} else {
						explanation = "All rooms switched off";
					}
				} else {
					explanation = "Heating is switched off";
				}
			}

			put(p, explanation);
		}
	}

	long int length = reinterpret_cast<long int>(p - data.get());
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

optional<float>
State::target(string z) const
{
	auto i = _target.find(z);
	if (i == _target.end()) {
		return optional<float>();
	}
	return i->second;
}
