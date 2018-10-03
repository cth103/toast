#include "compose.hpp"
#include <string>
#include <mutex>

class Log
{
public:
	static void log(std::string);

private:
	static std::mutex _mutex;
};

#define LOG(...) Log::log(String::compose(__VA_ARGS__));
#define LOG_NC(...) Log::log(__VA_ARGS__);
