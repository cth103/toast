#include "server.h"
#include "toast_socket.h"
#include "log.h"

using std::shared_ptr;
using std::scoped_lock;
using std::bind;

Server::Server(int port, int timeout)
	: _terminate(false)
	, _acceptor(_io_service, boost::asio::ip::tcp::endpoint(boost::asio::ip::tcp::v4(), port))
	, _timeout(timeout)
{

}

Server::~Server()
{
	{
		scoped_lock lm(_mutex);
		_terminate = true;
	}

	_acceptor.close();
	stop();
}

void
Server::run()
{
	start_accept();
	_io_service.run();
}

void
Server::start_accept()
{
	{
		scoped_lock lm(_mutex);
		if (_terminate) {
			return;
		}
	}

	shared_ptr<Socket> socket(new Socket(_timeout));
	_acceptor.async_accept(socket->socket(), std::bind(&Server::handle_accept, this, socket, std::placeholders::_1));
}

void
Server::handle_accept(shared_ptr<Socket> socket, boost::system::error_code const & error)
{
	if (error) {
		return;
	}

	LOG_CLIENT_NC("Client connection started.");

	handle(socket);
	start_accept();
}

void
Server::stop()
{
	_io_service.stop();
}
