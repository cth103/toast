#ifndef TOAST_SENSOR_H
#define TOAST_SENSOR_H

#include "transducer.h"
#include "datum.h"

class Sensor : public Transducer
{
public:
	Sensor(std::shared_ptr<Node> node, std::string id, std::string name, std::string zone)
		: Transducer(node, name, zone)
		, _id(id)
	{}

	Datum get() const;

private:
	std::string _id;
};

#endif
