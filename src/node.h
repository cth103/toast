#ifndef TOAST_NODE_H
#define TOAST_NODE_H

#include "sensor.h"
#include "actuator.h"
#include <boost/asio.hpp>
#include <list>
#include <memory>
#include <mutex>

class Node
{
public:
	Node(boost::asio::ip::address ip, std::string name, std::string mac)
		: _ip(ip), _name(name), _mac(mac)
	{}

	std::string name() const {
		return _name;
	}

	std::string mac() const {
		return _mac;
	}

	virtual Datum get(std::string id) const = 0;

	void add_sensor(std::shared_ptr<Sensor> s) {
		_sensors.push_back(s);
	}

	std::list<std::shared_ptr<Sensor>> sensors() const {
		return _sensors;
	}

	std::shared_ptr<Sensor> sensor(std::string name) const;

	void add_actuator(std::shared_ptr<Actuator> a) {
		_actuators.push_back(a);
	}

	std::shared_ptr<Actuator> actuator(std::string name) const;

	std::list<std::shared_ptr<Actuator>> actuators() const {
		return _actuators;
	}

	static std::list<std::shared_ptr<Node>> all();
	static void add(std::shared_ptr<Node> node);

protected:
	boost::asio::ip::address _ip;

private:
	std::string _name;
	std::string _mac;
	std::list<std::shared_ptr<Sensor>> _sensors;
	std::list<std::shared_ptr<Actuator>> _actuators;

	static std::mutex _all_mutex;
	static std::list<std::shared_ptr<Node>> _all;
};

#endif
