#include "node.h"

class ESP8266Node : public Node
{
public:
	ESP8266Node(boost::asio::ip::address ip, std::string name, std::string mac)
		: Node(ip, name)
		, _mac(mac)
	{}

	std::string mac() const {
		return _mac;
	}

	Datum get(std::string id) const;

private:
	std::string _mac;
};
