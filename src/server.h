#include <boost/asio.hpp>
#include <string>
#include <mutex>

class Socket;

class Server : public boost::noncopyable
{
public:
	explicit Server(int port, int timeout = 30);
	virtual ~Server();

	virtual void run();
	void stop();

protected:
	std::mutex _mutex;
	bool _terminate;

private:
	virtual void handle(std::shared_ptr<Socket> socket) = 0;

	void start_accept();
	void handle_accept(std::shared_ptr<Socket>, boost::system::error_code const &);

	boost::asio::io_service _io_service;
	boost::asio::ip::tcp::acceptor _acceptor;
	int _timeout;
};
