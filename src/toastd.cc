#include "broadcast_listener.h"
#include "config.h"
#include "node.h"
#include "state.h"
#include "control_server.h"
#include "esp8266_node.h"
#include "json_node.h"
#include <iostream>

using std::cout;
using std::string;
using std::bind;
using std::thread;
using std::shared_ptr;
using std::optional;
using std::list;
using std::dynamic_pointer_cast;

State state;
list<int> auto_off_hours;

void
node_broadcast_received(string mac, boost::asio::ip::address ip)
{
	bool got = false;
	for (auto i: Node::all()) {
		auto e = dynamic_pointer_cast<ESP8266Node>(i);
		if (e && e->mac() == mac) {
			got = true;
		}
	}

	if (!got) {
		/* XXX */
		/* "temperature"/"humidity" here are special values which dictate what the state class will send
		   to clients requesting those types.
		*/
		if (mac == "600194189ed3") {
			shared_ptr<Node> node(new ESP8266Node(ip, "spare-room", mac));
			node->add_sensor(shared_ptr<Sensor>(new Sensor(node, "temp", "temperature", "Bathroom")));
			node->add_actuator(shared_ptr<Actuator>(new Actuator(node, "radiator", "Bathroom")));
			Node::add(node);
		} else if (mac == "68c63ac4a3b3") {
			shared_ptr<Node> node(new ESP8266Node(ip, "loft", mac));
			node->add_sensor(shared_ptr<Sensor>(new Sensor(node, "temp", "temperature-high", "Bathroom")));
			node->add_sensor(shared_ptr<Sensor>(new Sensor(node, "humidity", "humidity", "Bathroom")));
			node->add_sensor(shared_ptr<Sensor>(new Sensor(node, "temp2", "temperature", "Landing")));
			node->add_sensor(shared_ptr<Sensor>(new Sensor(node, "humidity2", "humidity", "Landing")));
			node->add_actuator(shared_ptr<Actuator>(new Actuator(node, "fan", "Bathroom")));
			Node::add(node);
		}
	}
}

void
gather()
{
	while (true) {
		time_t t = time(0);
		struct tm tm = *localtime(&t);
		char log_file[256];
		snprintf(log_file, 256, "%s/%02d-%02d-%d.log", LOG_DIRECTORY, tm.tm_mday, tm.tm_mon, tm.tm_year + 1900);
		FILE* log = fopen(log_file, "a+");

		for (auto i: Node::all()) {
			for (auto j: i->sensors()) {
				auto d = j->get();
				state.add(j, d);
				if (log) {
					time_t const t = d.time();
					struct tm tm = *localtime(&t);
					fprintf(
						log,
						"%02d:%02d:%02d %s %s %s %f\n",
						tm.tm_hour, tm.tm_min, tm.tm_sec, i->name().c_str(), j->name().c_str(), j->zone().c_str(), d.value()
						);
				}
			}
		}

		if (log) {
			fclose(log);
		}
		sleep(GATHER_INTERVAL);
	}
}

void
control()
{
	while (true) {

		/* Auto off */
		time_t const t = time(0);
		struct tm tm = *localtime(&t);
		if (tm.tm_min == 0 && find(auto_off_hours.begin(), auto_off_hours.end(), tm.tm_hour) != auto_off_hours.end() && state.heating_enabled()) {
			state.set_heating_enabled(false);
		}

		/* Copy our current state */
		State active_state = state.thin_clone();

		/* Override interactive settings with active rules */
		for (auto i: state.rules()) {
			if (i.active()) {
				active_state.set_heating_enabled(true);
				active_state.set_zone_heating_enabled(i.zone(), true);
				active_state.set_target(i.zone(), i.target());
			}
		}

		/* Set radiators */
		for (auto i: Node::all()) {
			shared_ptr<Sensor> se = i->sensor("radiator");
			if (!se) {
				continue;
			}
			if (active_state.zone_heating_enabled(se->zone())) {
				string zone = i->sensor("temperature")->zone();
				optional<Datum> const t = active_state.get(zone, "temperature");
				if (t && t->value() > active_state.target(zone).value_or(0) + HYSTERESIS) {
					i->actuator("radiator")->set(false);
				} else if (t && t->value() < active_state.target(zone).value_or(0) - HYSTERESIS) {
					i->actuator("radiator")->set(true);
				}
			}
		}

		/* Decide if we need heat */
		bool heat_required = false;
		for (auto i: Node::all()) {
			if (i->actuator("radiator") && i->actuator("radiator")->state().value_or(false)) {
				heat_required = true;
			}
		}

		state.set_boiler_on(active_state.heating_enabled() && heat_required);

		/* XXX: humidity */

		sleep(CONTROL_INTERVAL);
	}
}

int
main(int argc, char* argv[])
{
	/* XXX */
	auto_off_hours.push_back(0);
	auto_off_hours.push_back(1);
	auto_off_hours.push_back(2);
	auto_off_hours.push_back(3);
	auto_off_hours.push_back(4);
	auto_off_hours.push_back(5);
	auto_off_hours.push_back(6);
	auto_off_hours.push_back(10);
	shared_ptr<Node> hall(new JSONNode(boost::asio::ip::address::from_string("127.0.0.1"), "hall"));
	hall->add_sensor(shared_ptr<Sensor>(new Sensor(hall, "", "temperature", "Sitting room")));
	Node::add(hall);

	BroadcastListener* b = new BroadcastListener();
	b->Received.connect(bind(&node_broadcast_received, _1, _2));
	b->run();

	thread gather_thread(gather);
	thread control_thread(control);

	ControlServer* s = new ControlServer(&state, SERVER_PORT);
	s->run();
}
