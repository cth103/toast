#include "actuator.h"
#include "node.h"

void
Actuator::set(bool s)
{
	_node->set(s);
	_state = s;
}
