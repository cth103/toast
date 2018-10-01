#include <boost/asio.hpp>

/** @class Socket
 *  @brief A class to wrap a boost::asio::ip::tcp::socket with some useful things.
 *
 *  This class wraps some things that I could not work out how to do easily with boost;
 *  most notably, sync read/write calls with timeouts.
 */
class Socket : public boost::noncopyable
{
public:
	explicit Socket(int timeout = 30);

	/** @return Our underlying socket */
	boost::asio::ip::tcp::socket& socket() {
		return _socket;
	}

	void connect(boost::asio::ip::tcp::endpoint);

	void write(uint8_t const * data, int size);
	int read(uint8_t* data, int size);

private:
	void check();

	Socket(Socket const &);

	boost::asio::io_service _io_service;
	boost::asio::deadline_timer _deadline;
	boost::asio::ip::tcp::socket _socket;
	int _timeout;
};
