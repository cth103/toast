#ifndef TOAST_TRANSDUCER_H
#define TOAST_TRANSDUCER_H

#include <string>
#include <memory>

class Node;

class Transducer
{
public:
	Transducer(std::shared_ptr<Node> node, std::string name, std::string zone)
		: _node(node), _name(name), _zone(zone)
	{}

	std::string name() const {
		return _name;
	}

	std::string zone() const {
		return _zone;
	}

protected:
	std::shared_ptr<Node> _node;

private:
	std::string _name;
	std::string _zone;
};

#endif
