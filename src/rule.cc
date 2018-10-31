#include "util.h"
#include "rule.h"

using std::pair;
using std::optional;
using std::string;
using std::make_pair;
using std::shared_ptr;

uint8_t Rule::_next_id = 0;

Rule::Rule(int days, int on_hour, int on_minute, int off_hour, int off_minute, float target, string zone)
	: _id(_next_id++)
	, _days(days)
	, _on_hour(on_hour)
	, _on_minute(on_minute)
	, _off_hour(off_hour)
	, _off_minute(off_minute)
	, _target(target)
	, _zone(zone)
{

}

void
Rule::get(uint8_t*& p, uint8_t* e) const
{
	*p++ = _id;
	*p++ = _days;
	*p++ = _on_hour;
	*p++ = _on_minute;
	*p++ = _off_hour;
	*p++ = _off_minute;
	put_float(p, e, _target);
	put_string(p, e, _zone);
}

bool
Rule::active(optional<struct tm> at) const
{
	if (!at) {
		time_t t = time(0);
		at = *localtime(&t);
	}

	int since_monday = at->tm_wday - 1;
	if (since_monday == -1) {
		since_monday = 6;
	}

	if ((_days & (1 << since_monday)) == 0)  {
		return false;
	}

	if (_on_hour > at->tm_hour || _off_hour < at->tm_hour) {
		return false;
	}

	return (
		(_on_hour < at->tm_hour || _on_minute <= at->tm_min) &&
		(_off_hour > at->tm_hour || _off_minute > at->tm_min)
		);
}
