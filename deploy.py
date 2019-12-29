#!/usr/bin/python3

import sys, os, shutil
from os import path
from urllib.request import pathname2url
import subprocess
from subprocess import call
import sys
import re
import zipfile

import config

os.chdir(config.root_dir)

SUPPORTED_OPERATING_SYSTEMS = ('windows_x64', 'linux_x64', 'mac')#, 'linux-arm32', 'linux-arm64')


def make_dir(dir_path):
	"""
	make_dir(dir_path)

	creates a directory if it does not already exist, including parent 
	directories.

	dir_path - directory to create
	"""
	if not path.exists(dir_path):
		os.makedirs(dir_path)
def make_parent_dir(file_path):
	"""
	make_parent_dir(file_path)
	
	Creates the parent directory for the specified filepath if it does not 
	already exist.
	
	file_path - path to some file
	"""
	parent_dir = path.dirname(file_path)
	if parent_dir == '': # means parent is working directory
		return
	if not path.isdir(parent_dir):
		os.makedirs(parent_dir)
def _del(filepath):
	"""
	Deletes a file or recursively deletes a directory. Use with caution.
	"""
	if(path.isdir(filepath)):
		for f in os.listdir(filepath):
			_del(path.join(filepath,f))
		os.rmdir(filepath)
	elif(path.exists(filepath)):
		os.remove(filepath)
def del_file(filepath):
	"""
	del_file(filepath):
	
	Deletes a file or recursively deletes a directory. Use with caution.
	
	filepath - path to file or directory to delete
	"""
	if(path.isdir(filepath)):
		for f in os.listdir(filepath):
			_del(path.join(filepath,f))
		os.rmdir(filepath)
	elif(path.exists(filepath)):
		os.remove(filepath)
def del_contents(dirpath):
	""" 
	del_contents(dirpath)
	
	Recursively deletes the contents of a directory, but not the directory itself
	
	dirpath - path to directory to clean-out
	"""
	if(path.isdir(dirpath)):
		for f in os.listdir(dirpath):
			del_file(path.join(dirpath,f))
def list_files(dirpath):
	"""
	list_filetree(dirpath)
	
	Returns a list of all files inside a directory (recursive scan)
	
	dirpath - filepath of directory to scan
	"""
	if(type(dirpath) == str):
		dir_list = [dirpath]
	else:
		dir_list = dirpath
	file_list = []
	for _dir_ in dir_list:
		for base, directories, files in os.walk(_dir_):
			for f in files:
				file_list.append(path.join(base,f))
	return file_list
def safe_quote_string(text):
	"""
	safe_quote_string(text)
	
	returns the text in quotes, with escapes for any quotes in the text itself
	
	text - input text to quote
	
	returns: text in quotes with escapes
	"""
	if os.sep != '\\':
		text2 = text.replace('\\', '\\\\')
		text3 = text2.replace('"', '\\"')
	else:
		text3 = text.replace('\\', '/')
		# windows does not allow " in file names anyway
	return '"'+text3+'"'
def copy_tree(file_list, src_root, dest_root):
	"""
	copy_tree(file_list, src_root, dest_root)
	
	Copies all files to directory dest_root (creating it if necessary), 
	preserving the folder structure relative to src_root
	"""
	for f in file_list:
		rel_path = path.relpath(f, src_root)
		dst_path = path.join(dest_root, rel_path)
		make_parent_dir(dst_path)
		shutil.copy(f, dst_path)
def zip_dir(dir_path, zip_path):
	print('\nzipping %s to %s\n' % (dir_path, zip_path))
	with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
		# zipf is zipfile handle
		for root, dirs, files in os.walk(dir_path):
			for file in files:
				fname = path.basename(dir_path)
				src_file = path.join(root, file)
				dst_file = path.join(fname, path.relpath(src_file, dir_path) )
				zipf.write(src_file, arcname=dst_file)
	# done

# make dirs
make_dir(config.local_cache_dir)
make_dir(config.compile_dir)
make_dir(config.jar_dir)
make_dir(config.deploy_dir)
make_dir(config.deploy_image_dir)
make_dir(config.run_dir)
make_dir(config.src_dir)
make_dir(config.resource_dir)

# clean
del_contents(config.run_dir)
del_contents(config.jar_dir)
del_contents(config.compile_dir)
del_contents(config.deploy_image_dir)
del_contents(config.deploy_dir)

# compile (with jmods)
for release_OS in SUPPORTED_OPERATING_SYSTEMS:
	print('\n',release_OS,'\n')
	module_src_path = path.join(config.src_dir, config.module_name)
	if(release_OS == 'windows_x64'):
		#java_home = 'D:\\CCHall\\Documents\\Programming\\OpenJDK_Distros\\windows-x64\\jdk-13.0.1'
		jmod_dirs = [path.join('jmods','windows')] #[path.join(java_home,'jmods')] + config.jmod_dirs_windows_x64
	elif(release_OS == 'linux_x64'):
		#java_home = 'D:\\CCHall\\Documents\\Programming\\OpenJDK_Distros\\linux-x64\\jdk-13.0.1'
		jmod_dirs = [path.join('jmods','linux')] #[path.join(java_home,'jmods')] + config.jmod_dirs_linux_x64
	elif(release_OS == 'mac'):
		#java_home = 'D:\\CCHall\\Documents\\Programming\\OpenJDK_Distros\\osx-x64\\jdk-13.0.1'
		jmod_dirs = [path.join('jmods','mac')] #[path.join(java_home,'jmods')] + config.jmod_dirs_mac
	else:
		print('UNSUPPORTED OS: %s' % release_OS)
	arg_file = path.join(config.local_cache_dir, 'javac-args.txt')
	command_list = []
	command_list += ['-encoding', 'utf8']
	command_list += ['-d', config.compile_dir]
	command_list += ['--module-source-path', config.src_dir]
	command_list += ['--module', config.module_name]
	module_paths = jmod_dirs + [f for f in list_files(config.dependency_dirs) if str(f).endswith('.jar')] # a .jmod file is auto-discoverable by --module-path
	command_list += ['--module-path', os.pathsep.join(module_paths)]
	with open(arg_file, 'w') as fout:
		file_content = ' '.join(map(safe_quote_string, command_list))
		fout.write(file_content)
		print('@%s: %s' % (arg_file, file_content))
	call([config.javac_exec, '@'+str(arg_file)], cwd=config.root_dir)
	print()
	# need to copy resources separately
	resource_files = list_files(config.resource_dir)
	resource_files += [f for f in list_files(config.src_dir) if str(f).endswith('.java') == False]
	copy_tree(
			list_files(config.resource_dir), 
			config.src_dir,
			config.compile_dir
	)
	copy_tree(
			[f for f in list_files(module_src_path) if str(f).endswith('.java') == False], 
			config.src_dir,
			config.compile_dir
	)

	# jlink
	arg_file = path.join(config.local_cache_dir, 'jlink-args.txt')
	command_list = []
	command_list += ['--module-path', os.pathsep.join(module_paths + [config.compile_dir])]
	command_list += ['--add-modules', config.module_name]
	image_dir = path.join(config.deploy_image_dir, release_OS, config.module_name)
	command_list += ['--launcher', 'launch=%s/%s' % (config.module_name, config.main_class)]
	command_list += ['--output', image_dir]
	with open(arg_file, 'w') as fout:
		file_content = ' '.join(map(safe_quote_string, command_list))
		fout.write(file_content)
		print('@%s: %s' % (arg_file, file_content))
	call([config.jlink_exec, '@'+str(arg_file)], cwd=config.root_dir)
	# launcher
	if release_OS == 'windows_x64':
		with open(path.join(image_dir, 'launch_%s.bat' % config.module_title),'w') as fout:	
			fout.write('"%~dp0\\bin\\launch.bat"\r\n')
	if release_OS == 'linux_x64':
		with open(path.join(image_dir, 'launch_%s.sh' % config.module_title),'w') as fout:	
			fout.write('#!/bin/bash\ncd "`dirname "$0"`"\n./bin/launch\n')
	if release_OS == 'mac':
		with open(path.join(image_dir, 'launch_%s.sh' % config.module_title),'w') as fout:	
			fout.write('#!/bin/sh\ncd "`dirname "$0"`"\n./bin/launch\n')
	
	# package images
	named_dir = path.join(config.deploy_image_dir, release_OS, config.module_title)
	zip_file = path.join(config.deploy_image_dir, '%s_%s.zip' % (config.module_title, release_OS))
	shutil.move(image_dir, named_dir)
	zip_dir(dir_path=named_dir, zip_path=zip_file)
	
