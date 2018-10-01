#include "datum.h"
#include <memory>

class Node;
class Sensor;

class State
{
public:
	void add(std::shared_ptr<Node> node, std::shared_ptr<Sensor> sensor, Datum datum);
};
