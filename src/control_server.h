#include "server.h"
#include <list>

class State;

class ControlServer : public Server
{
public:
	ControlServer(State* state, int port, int timeout = 30);

private:
	void handle(std::shared_ptr<Socket> socket);
	std::list<std::string> zones_from_message(uint8_t*& p);

	static void write(std::shared_ptr<Socket>, uint8_t const *, int);

	State* _state;
};
