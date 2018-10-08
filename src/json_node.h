#include "node.h"

struct json_node_test;

class JSONNode : public Node
{
public:
	JSONNode(boost::asio::ip::address ip, std::string name)
		: Node(ip, name)
	{}

	Datum get(std::string id) const;
	void set(bool) {
		/* JSON nodes don't have actuators at the moment */
	}

private:
	friend struct json_node_test;

	static float parse_reply(std::string reply);
};
