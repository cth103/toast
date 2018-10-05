#include "broadcast_listener.h"
#include "config.h"
#include "node.h"
#include "state.h"
#include "control_server.h"
#include "esp8266_node.h"
#include "json_node.h"
#include "log.h"
#ifdef TOAST_HAVE_WIRINGPI
#include <wiringPi.h>
#endif
#include <iostream>

using std::cout;
using std::string;
using std::bind;
using std::thread;
using std::shared_ptr;
using std::optional;
using std::list;
using std::dynamic_pointer_cast;
using std::runtime_error;

State state;
list<int> auto_off_hours;

void
node_broadcast_received(string mac, boost::asio::ip::address ip)
{
	LOG_NODE("Received broadcast from %1 %2", mac, ip.to_string());

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
		snprintf(log_file, 256, "%s/%02d-%02d-%d.log", Config::instance()->log_directory().c_str(), tm.tm_mday, tm.tm_mon, tm.tm_year + 1900);
		FILE* log = fopen(log_file, "a+");

		for (auto i: Node::all()) {
			for (auto j: i->sensors()) {
				try {
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
				} catch (runtime_error& e) {
					LOG_ERROR("Could not get value from %1: %2", j->name(), e.what());
				}
			}
		}

		if (log) {
			fclose(log);
		}
		sleep(Config::instance()->gather_interval());
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
			LOG_DECISION_NC("Doing auto-off");
			state.set_heating_enabled(false);
		}

		/* Copy our current state */
		State active_state = state.thin_clone();

		/* Override interactive settings with active rules */
		for (auto i: state.rules()) {
			if (i.active()) {
				LOG_DECISION_NC("Have an active rule");
				active_state.set_heating_enabled(true);
				active_state.set_zone_heating_enabled(i.zone(), true);
				active_state.set_target(i.zone(), i.target());
			}
		}

		/* Set radiators */
		for (auto i: Node::all()) {
			shared_ptr<Actuator> rad = i->actuator("radiator");
			if (!rad) {
				continue;
			}
			if (active_state.zone_heating_enabled(rad->zone())) {
				string zone = i->sensor("temperature")->zone();
				optional<Datum> const t = active_state.get(zone, "temperature");
				float const hysteresis = Config::instance()->hysteresis();
				if (t && t->value() > active_state.target(zone) + hysteresis) {
					rad->set(false);
				} else if (t && t->value() < active_state.target(zone) - hysteresis) {
					rad->set(true);
				}
			} else {
				rad->set(false);
			}
		}

		/* Decide if we need heat */
		bool heat_required = false;
		for (auto i: Node::all()) {
			if (i->actuator("radiator") && i->actuator("radiator")->get().value_or(false)) {
				heat_required = true;
			}
		}

		LOG_DECISION("heating_enabled=%1 heat_required=%2", active_state.heating_enabled(), heat_required);
		state.set_boiler_on(active_state.heating_enabled() && heat_required);

		for (auto i: Node::all()) {
			shared_ptr<Actuator> fan = i->actuator("fan");
			if (!fan) {
				continue;
			}
			optional<Datum> const current = active_state.get(fan->zone(), "humidity");
			/* XXX */
			optional<Datum> const ref = active_state.get("Landing", "humidity");
			if (current && ref) {
				Config* config = Config::instance();
				float diff = current->value() - ref->value();
				if (fan->get().value_or(false) && diff > config->humidity_rising_threshold()) {
					fan->set(true);
				} else if (fan->get().value_or(false) && diff < config->humidity_falling_threshold()) {
					fan->set(false);
				}
			}
		}

		sleep(Config::instance()->control_interval());
	}
}

int
main()
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

#ifdef TOAST_HAVE_WIRINGPI
	wiringPiSetup();
	wiringPiSetupGpio();
	pinMode(Config::instance()->boiler_gpio(), OUTPUT);
#endif

	LOG_STARTUP_NC("Starting broadcast listener");
	BroadcastListener* b = new BroadcastListener();
	b->Received.connect(bind(&node_broadcast_received, _1, _2));
	b->run();

	LOG_STARTUP_NC("Starting gather thread");
	thread gather_thread(gather);
	LOG_STARTUP_NC("Starting control thread");
	thread control_thread(control);

	LOG_STARTUP_NC("Starting control server");
	ControlServer* s = new ControlServer(&state, Config::instance()->server_port());
	s->run();
}
