#include "node.h"

class ESP8266Node : public Node
{
public:
	ESP8266Node(boost::asio::ip::address ip, std::string name, std::string mac)
		: Node(ip, name, mac)
	{}

	Datum get(std::string id) const;
};
