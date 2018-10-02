#ifndef TOAST_ACTUATOR_H
#define TOAST_ACTUATOR_H

#include "transducer.h"
#include <optional>

class Actuator : public Transducer
{
public:
	Actuator(std::shared_ptr<Node> node, std::string name, std::string zone)
		: Transducer(node, name, zone)
	{}

	std::optional<bool> state() const {
		return _state;
	}

	void set(bool s) {
		_state = s;
	}

private:
	std::optional<bool> _state;

};

#endif
