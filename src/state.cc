#include "state.h"
#include "node.h"
#include "sensor.h"
#include <iostream>

using std::shared_ptr;
using std::cout;

void
State::add(shared_ptr<Node> node, shared_ptr<Sensor> sensor, Datum datum)
{
	cout << node->name() << " " << sensor->zone() << " " << sensor->name() << " " << datum.value() << "\n";
}
