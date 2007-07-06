#
# Copyright (C) 2006 SIPfoundry Inc.
# Licensed by SIPfoundry under the LGPL license.
# 
# Copyright (C) 2006 Pingtel Corp.
# Licensed to SIPfoundry under a Contributor Agreement.
#
##############################################################################

# system requires
require 'logger'
require 'tmpdir'

# application requires
require 'process_config'

=begin
:NOW: In the process manager server test, clean up the sleeper process that it
      starts & figure out how to shut down the server so that the test can be
      run more than once.
:TODO: Start, stop, report status
:TODO: Don't start a process if it's already running
:TODO: Monitor processes and try to restart them if they go down
:TODO: Don't hardwire directories like /var/log/sipxpbx, instead get them from
         sipX directory variables, as defined in
         http://www.sipfoundry.org/sipX/doc/filesystem.html .
        The file config/subdir.am provides some machinery that can be
        applied via a Makefile.

:TODO: Honor process config:
:TODO: "manage" flag -- if false then don't do anything with the process
:TODO: "max_restarts" setting
:TODO: "report", "max_reports" settings
:TODO: "failure_contact" setting
:TODO: "stdout", "stderr", "stdin" settings

:TODO: Remote method to replicate files
:TODO: Remote method to replicate sipdb data
:LATER: Remote method to add/edit/remove crontab entry
:LATER: Create the XML schema referenced by process config files
=end

# ProcessManager features:
# * Start, monitor, stop, report status on sipX processes
#   * Replaces watchdog, processcgi, WatchDog.xml, ProcessDefinitions.xml, ...
# * Offer network services:
#   * Read/write sipdb databases and arbitrary files (replace and extend replicationcgi)
# * Configured via files that are installed into /etc/sipxpbx/process
class ProcessManager
  
  # If set, then this becomes a prefix to the default sipX directories
  SIPX_PREFIX = 'SIPX_PREFIX'

  # Default directory in which to store PID files for the processes we are managing
  PID_DIR_DEFAULT = '/var/run/sipxpbx'
  
  # Default directory in which process config files are located
  PROCESS_CONFIG_DIR_DEFAULT = '/etc/sipxpbx/process'
  
  PID_FILE_EXT = '.pid'
  
  CONFIG_FILE_PATTERN = Regexp.new('.*\.xml')
  
  # Logical sipX directory names
  DATA_DIR = 'data'
  ETC_DIR = 'etc'
  LOG_DIR = 'log'
  TMP_DIR = 'tmp'   # used for testing
  
  # Map directory names to directory locations
  DIR_MAP_DEFAULT = {
    DATA_DIR => '/var/sipxdata/',
    ETC_DIR  => '/etc/sipxpbx/',
    LOG_DIR  => '/var/log/sipxpbx/',
    TMP_DIR  => Dir.tmpdir}
  
public

  def initialize(config = {})
    @pid_map = {}       # map process names to PIDs
    
    init_process_config_dir(config[:ProcessConfigDir])
    init_process_config
    init_logging
    init_pid_dir
  end

  # Manage the named processes.  verb is the operation to perform, e.g., 'start'.
  # processes is an array of process names, which must match the names of
  # configured processes.
  # By convention, the verb 'blah' invokes a method named 'blah_process_by_name'.
  def manageProcesses(verb, process_names)
    method_name = (verb + '_process_by_name').to_sym
    process_names.each do |process_name|
      self.send(method_name, process_name)
    end
  end

  # Start the named process. Log an error if no such process is configured.
  def start_process_by_name(process_name)
    config = @process_config_map[process_name]
    if !config
      log.error("start_process_by_name: cannot start \"#{process_name}\", no such process is configured")
    end
    start_process(config)
  end

  # Start the named process if it is not already running.  Log an error if no
  # such process is configured.  Return the PID of the new or existing process.
  def start_process(process_config)
    # Return the existing PID if the named process is already running
    pid = @pid_map[process_config.name]
    return pid if pid
    
    # Get info on how to run the process. Assume that the config has been validated already.
    run = process_config.run
    command = run.command
    parameters = run.parameters
    defaultdir = run.defaultdir
    
    # Start the process
    pid = fork do
      log.debug("start_process: command = \"#{command}\", parameters = " +
                "\"#{parameters}\", defaultdir = \"#{defaultdir}\"")
      Dir.chdir(defaultdir) if defaultdir
      exec("#{command} #{parameters}")
    end

    # Remember the process
    @pid_map[process_config.name] = pid
    pid_file_path = create_process_pid_file(process_config.name, pid)
    log.debug("start_process: PID file = \"#{pid_file_path}\"")
    
    pid
  end

  # Stop the named process.  Return true if a process was running and we stopped
  # it, false otherwise.
  def stop_process_by_name(process_name)
    did_stop = false
    pid = @pid_map[process_name]
    if pid
      # Looks like there is a process running -- kill it.
      Process.kill('TERM', pid)
      did_stop = true
      @pid_map[process_name] = nil
    end

    did_stop
  end

  # Restart the named process. Log an error if no such process is configured.
  def restart_process_by_name(process_name)
    config = @process_config_map[process_name]
    if !config
      log.error("restart_process_by_name: cannot restart \"#{process_name}\", no such process is configured")
    end
    pid = restart_process(config)
  end

  # Restart the process.  Return the new pid.
  def restart_process(process_config)
    stop_process_by_name(process_config.name)
    pid = start_process(process_config)
    puts pid
    pid
  end
  
  # Return the named sipX directory.  Raise a runtime exception if there is no
  # such directory.
  def get_sipx_directory(name)
    sipx_dir = DIR_MAP_DEFAULT[name]
    if !sipx_dir
      raise(RuntimeError, "Unknown sipX directory name \"#{name}\"", caller)
    end

    # Prepend the prefix dir if $SIPX_PREFIX is defined, except for TMP_DIR, which
    # is a special case for testing.
    if name != TMP_DIR
      sipx_dir = prepend_sipx_prefix(sipx_dir)
    end
    
    sipx_dir
  end

  # These accessors are mainly used for testing
  attr_accessor :pid_dir
  attr_reader :process_config_dir, :process_config_map, :log

private

  def init_process_config_dir(process_config_dir)
    if process_config_dir
      @process_config_dir = process_config_dir
    else
      @process_config_dir = PROCESS_CONFIG_DIR_DEFAULT
      
      # Prepend the prefix dir if $SIPX_PREFIX is defined
      @process_config_dir = prepend_sipx_prefix(@process_config_dir)
    end
  end
  
  # Each config file in the config dir sets up a sipX process.
  # Read config info from those files and build a process map.
  def init_process_config
    @process_config_map = {}
    get_process_config_files.each do |file|
      config = ProcessConfig.new(File.new(file))
      @process_config_map[config.name] = config
    end
    @process_config_map
  end
  
  # Return an array containing the paths of process config files.
  def get_process_config_files
    config_files = []
    Dir.foreach(@process_config_dir) do |file|
      config_files << File.join(@process_config_dir, file) if file =~ CONFIG_FILE_PATTERN
    end
    config_files
  end
  
  def init_logging
    @log = Logger.new(STDOUT)
    @log.level = Logger::DEBUG
  end
  
  # Set @pid_dir to be the directory in which to store PID files for the
  # processes we are managing.
  def init_pid_dir
    @pid_dir = PID_DIR_DEFAULT
      
    # Prepend the prefix dir if $SIPX_PREFIX is defined
    @pid_dir = prepend_sipx_prefix(@pid_dir)
  end

  # Create a PID file for the named process.  Return the path to the file.
  def create_process_pid_file(process_name, pid)
    pid_file_path = File.join(@pid_dir, process_name + PID_FILE_EXT)
    File.open(pid_file_path, 'w') do |file|
      file.puts("#{pid}")
    end
    
    pid_file_path
  end

  # If the environment variable SIPX_PREFIX is defined, then prepend it to the
  # path.  Return the path.
  def prepend_sipx_prefix(path)
    prefix = ENV[SIPX_PREFIX]
    if prefix
      path = File.join(prefix, path)
    end
    path
  end

end