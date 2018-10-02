#include <memory>
#include <optional>

class Rule
{
public:
	static int const MONDAY =    1 << 0;
	static int const TUESDAY =   1 << 1;
	static int const WEDNESDAY = 1 << 2;
	static int const THURSDAY =  1 << 3;
	static int const FRIDAY =    1 << 4;
	static int const SATURDAY =  1 << 5;
	static int const SUNDAY =    1 << 6;

	Rule(int days, int on_hour, int on_minute, int off_hour, int off_minute, float target, std::string zone);

	bool active(std::optional<struct tm> at = std::optional<struct tm>()) const;
	std::pair<std::shared_ptr<uint8_t[]>, int> get() const;

private:
	uint8_t _id;
	int _days;
	int _on_hour;
	int _on_minute;
	int _off_hour;
	int _off_minute;
	float _target;
	std::string _zone;

	static uint8_t _next_id;
};
