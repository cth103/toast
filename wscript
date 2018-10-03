def options(opt):
    opt.load('compiler_cxx')
    opt.add_option('--enable-debug', action='store_true', default=False, help='build with debugging information and without optimisation')

def configure(conf):
    conf.load('compiler_cxx')
    conf.env.append_value('CXXFLAGS', ['-Wall', '-Wextra', '-std=c++17'])
    if conf.options.enable_debug:
        conf.env.append_value('CXXFLAGS', ['-g', '-fno-omit-frame-pointer'])
    else:
        conf.env.append_value('CXXFLAGS', '-O2')
    conf.check(lib=['boost_system', 'pthread'], uselib_store='BOOST_SYSTEM', msg="Checking for library boost-system")
    conf.recurse('test')

def build(bld):
    bld.recurse('src')
    bld.recurse('test')
