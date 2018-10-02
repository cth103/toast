#include "compose.hpp"
#include <string>

class Log
{
public:
	static void log(std::string);
};

#define LOG(...) Log::log(String::compose(__VA_ARGS__));
#define LOG_NC(...) Log::log(__VA_ARGS__);
