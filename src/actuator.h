#ifndef TOAST_ACTUATOR_H
#define TOAST_ACTUATOR_H

#include "transducer.h"

class Actuator : public Transducer
{
public:
	Actuator(std::shared_ptr<Node> node, std::string id, std::string name, std::string zone)
		: Transducer(node, id, name, zone)
	{}

};

#endif
