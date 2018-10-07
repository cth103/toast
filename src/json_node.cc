#include "json_node.h"
#include "toast_socket.h"
#include "config.h"
#include "util.h"

using std::string;
using std::pair;
using std::runtime_error;
using std::shared_ptr;

Datum
JSONNode::get(string) const
{
	Config* config = Config::instance();
	shared_ptr<Socket> socket(new Socket(config->sensor_timeout()));
	socket->connect(boost::asio::ip::tcp::endpoint(_ip, config->sensor_port()));
	char buffer[64];
	snprintf(buffer, sizeof(buffer), "{\"type\": \"get\"}");
	write_with_length(socket, reinterpret_cast<uint8_t*>(buffer), strlen(buffer));
	pair<shared_ptr<uint8_t[]>, uint32_t> reply = read_with_length(socket);
	if (reply.second > 63) {
		throw runtime_error("unexpectedly long reply from JSON node");
	}
	memcpy(buffer, reply.first.get(), reply.second);
	buffer[reply.second] = '\0';
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
