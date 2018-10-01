#include "sensor.h"
#include "node.h"

Datum
Sensor::get() const
{
	return _node->get(_id);
}
