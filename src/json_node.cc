#include "json_node.h"
#include "toast_socket.h"
#include "config.h"

using std::string;
using std::runtime_error;

Datum
JSONNode::get(string) const
{
	Socket socket(SENSOR_TIMEOUT);
	socket.connect(boost::asio::ip::tcp::endpoint(_ip, SENSOR_PORT));
	char buffer[64];
	snprintf(buffer, sizeof(buffer), "{\"type\": \"get\"}");
	socket.write(reinterpret_cast<uint8_t*>(buffer), strlen(buffer));
	int const N = socket.read(reinterpret_cast<uint8_t*>(buffer), sizeof(buffer) - 1);
	buffer[N] = '\0';
	return Datum(parse_reply(buffer));
}

float
JSONNode::parse_reply(string reply)
{
	size_t const colon = reply.find(":");
	if (colon == string::npos) {
		throw runtime_error("failed to parse JSON reply");
	}
	size_t const brace = reply.find("}");
	if (brace == string::npos) {
		throw runtime_error("failed to parse JSON reply");
	}
	if ((colon + 1) >= reply.length() || brace == 0) {
		throw runtime_error("failed to parse JSON reply");
	}
	return atof(reply.substr(colon + 1, brace - 1).c_str());
}
