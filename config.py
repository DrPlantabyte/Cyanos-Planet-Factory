#!/usr/bin/python3

from os import path


this_file = path.realpath(__file__)
this_dir = path.dirname(this_file)

root_dir = path.dirname(path.abspath(__file__)) # change if this file is not in project root directory

#project settings
module_name = 'cchall.cyanosplanetfactory' # module name
compile_dir = path.join('out','production') # intermediary compiled files (ie .class files) are sotered here
jar_dir = path.join('out','artifacts') # compiled binaries (aka artifects) go here
deploy_dir = path.join('out','deploy') # deployed packages go here
deploy_image_dir = path.join(deploy_dir,'images') # deployed packages go here
run_dir = path.join('out','run') # working directory when using the run script
local_cache_dir = 'cache' # temporary generated files
src_dir = 'src' # code files
resource_dir = 'resources' # non-code files that should be included with the generated binaries
dependency_dirs = ['lib'] # libraries (.jar files/modules)jmods_dirs = []
main_class = 'hall.collin.christopher.cpf.App' # classpath for the main(...) method



# binaries
java_exec = 'java'
javac_exec = 'javac'
jlink_exec = 'jlink'
jar_exec = 'jar'
python_exec = 'python'

# hooks for custom modifications
def pre_clean():
	pass
def post_clean():
	pass
def pre_build():
	pass
def post_build():
	pass
def pre_jar():
	pass
def post_jar():
	pass
def pre_run():
	pass
def post_run():
	pass
def pre_junit():
	pass
def post_junit():
	pass

# OS-specific mods
import sys
if 'win' in sys.platform:
	# windows	#dependency_dirs += ['D:\\CCHall\\Documents\\Programming\\JFX-SDK\\openjfx-11.0.2_windows-x64_bin-sdk\\javafx-sdk-11.0.2\\lib']	jmods_dirs += ['D:\CCHall\Documents\Programming\JFX-SDK\openjfx-11.0.2_windows-x64_bin-jmods\javafx-jmods-11.0.2']
	python_exec = 'python.exe'
	pass
elif 'linux' in sys.platform:
	# linux
	pass
elif 'darwin' in sys.platform:
	# mac
	pass
else:
	# unknown
	pass

