#include "../src/rule.h"
#include <boost/test/unit_test.hpp>

BOOST_AUTO_TEST_CASE(rule_test)
{
	Rule rule(Rule::MONDAY | Rule::THURSDAY, 10, 30, 12, 45, 21, "Bathroom");

	struct tm at;

	at.tm_hour = 10;
	at.tm_min = 25;
	at.tm_wday = 0; // Sunday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 10;
	at.tm_min = 50;
	at.tm_wday = 0; // Sunday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 12;
	at.tm_min = 50;
	at.tm_wday = 0; // Sunday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 18;
	at.tm_min = 59;
	at.tm_wday = 0; // Sunday
	BOOST_CHECK(!rule.active(at));

	at.tm_hour = 10;
	at.tm_min = 25;
	at.tm_wday = 1; // Monday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 10;
	at.tm_min = 50;
	at.tm_wday = 1; // Monday
	BOOST_CHECK(rule.active(at));
	at.tm_hour = 12;
	at.tm_min = 50;
	at.tm_wday = 1; // Monday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 18;
	at.tm_min = 59;
	at.tm_wday = 1; // Monday
	BOOST_CHECK(!rule.active(at));

	at.tm_hour = 10;
	at.tm_min = 25;
	at.tm_wday = 4; // Thursday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 10;
	at.tm_min = 50;
	at.tm_wday = 4; // Thursday
	BOOST_CHECK(rule.active(at));
	at.tm_hour = 12;
	at.tm_min = 50;
	at.tm_wday = 4; // Thursday
	BOOST_CHECK(!rule.active(at));
	at.tm_hour = 18;
	at.tm_min = 59;
	at.tm_wday = 4; // Thursday
	BOOST_CHECK(!rule.active(at));
}
