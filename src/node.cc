#include "node.h"
#include "log.h"
#include <iostream>

using std::list;
using std::shared_ptr;
using std::scoped_lock;
using std::mutex;
using std::string;
using std::cout;

list<shared_ptr<Node>> Node::_all;
mutex Node::_all_mutex;

list<shared_ptr<Node>>
Node::all()
{
	scoped_lock lm(_all_mutex);
	return _all;
}

void
Node::add(shared_ptr<Node> node)
{
	scoped_lock lm(_all_mutex);
	_all.push_back(node);
	LOG_NODE("Added node %1", node->description());
}

shared_ptr<Sensor>
Node::sensor(string name) const
{
	for (auto i: _sensors) {
		if (i->name() == name) {
			return i;
		}
	}

	return shared_ptr<Sensor>();
}

shared_ptr<Actuator>
Node::actuator(string name) const
{
	for (auto i: _actuators) {
		if (i->name() == name) {
			return i;
		}
	}

	return shared_ptr<Actuator>();
}

string
Node::description() const
{
	return String::compose("%1 %2", ip(), name());
}
