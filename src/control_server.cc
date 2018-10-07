#include "control_server.h"
#include "state.h"
#include "toast_socket.h"
#include "types.h"
#include "log.h"
#include "util.h"
#include <iostream>

using std::list;
using std::cout;
using std::string;
using std::pair;
using std::shared_ptr;
using std::runtime_error;

ControlServer::ControlServer(State* state, int port, int timeout)
	: Server(port, timeout)
	, _state(state)
{

}

list<string>
ControlServer::zones_from_message(uint8_t*& p)
{
	int N = *p++;
	list<string> z;
	for (int i = 0; i < N; ++i) {
		int const L = *p++;
		string n;
		for (int j = 0; j < L; ++j) {
			n += *p++;
		}
		z.push_back(n);
	}
	return z;
}

void
ControlServer::handle(shared_ptr<Socket> socket)
try
{
	pair<shared_ptr<uint8_t[]>, uint32_t> data = read_with_length(socket);
	if (!data.second) {
		return;
	}
	uint8_t* p = data.first.get();
	LOG_CLIENT("Opcode is %1", static_cast<int>(p[0]));
	if (p[0] == OP_SEND_BASIC) {
		auto s = _state->get(false, OP_ALL);
		write_with_length(socket, s.first.get(), s.second);
	} else if (p[0] == OP_SEND_ALL) {
		auto s = _state->get(true, OP_ALL);
		write_with_length(socket, s.first.get(), s.second);
	} else if (p[0] == OP_HEATING_ENABLED) {
		_state->set_heating_enabled(p[1]);
		auto s = _state->get(false, OP_HEATING_ENABLED);
		write_with_length(socket, s.first.get(), s.second);
	} else if (p[0] == OP_ZONE_HEATING_ENABLED) {
		p++;
		for (auto i: zones_from_message(p)) {
			_state->set_zone_heating_enabled(i, *p++);
		}
		auto s = _state->get(false, OP_ZONE_HEATING_ENABLED);
		write_with_length(socket, s.first.get(), s.second);
	} else if (p[0] == OP_TARGET) {
		p++;
		for (auto i: zones_from_message(p)) {
			_state->set_target(i, static_cast<float>(p[0] | (p[1] << 8)) / 16);
			p += 2;
		}
		auto s = _state->get(false, OP_TARGET);
		write_with_length(socket, s.first.get(), s.second);
	}
}
catch (runtime_error& e)
{
	LOG_CLIENT("ControlServer conversation died: %1", e.what());
}
