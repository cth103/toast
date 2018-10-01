#include <mutex>

/** @class ExceptionStore
 *  @brief A parent class for classes which have a need to catch and
 *  re-throw exceptions.
 *
 *  This is intended for classes which run their own thread; they should do
 *  something like
 *
 *  void my_thread()
 *  try {
 *    // do things which might throw exceptions
 *  } catch (...) {
 *    store_current();
 *  }
 *
 *  and then in another thread call rethrow().  If any
 *  exception was thrown by my_thread it will be stored by
 *  store_current() and then rethrow() will re-throw it where
 *  it can be handled.
 */
class ExceptionStore
{
public:
	void rethrow() {
		std::scoped_lock lm(_exception_mutex);
		if (_exception) {
			std::exception_ptr tmp = _exception;
			_exception = std::exception_ptr();
			std::rethrow_exception(tmp);
		}
	}

protected:

	void store_current() {
		std::scoped_lock lm(_exception_mutex);
		_exception = std::current_exception();
	}

private:
	std::exception_ptr _exception;
	mutable std::mutex _exception_mutex;
};
