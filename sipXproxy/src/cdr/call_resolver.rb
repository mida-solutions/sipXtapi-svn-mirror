#
# Copyright (C) 2006 SIPfoundry Inc.
# Licensed by SIPfoundry under the LGPL license.
# 
# Copyright (C) 2006 Pingtel Corp.
# Licensed to SIPfoundry under a Contributor Agreement.
#
##############################################################################

# system requires
require 'rubygems'            # Ruby packaging and installation framework
require_gem 'activerecord'    # object-relational mapping layer for Rails
require 'logger'              # writes log messages to a file or stream
require 'observer'            # for Observer pattern a.k.a Publish/Subscribe

# set up the load path
thisdir = File.dirname(__FILE__)
$:.unshift(thisdir)
$:.unshift(File.join(thisdir, "app", "models"))

# application requires
require 'call_resolver_configure'
require 'call_state_event'
require 'cdr'
require 'configure'
require 'database_url'
require 'database_utils'
require 'exceptions'


# :TODO: log the number of calls analyzed and how many succeeded vs. dups or
#        failures, also termination status

# The CallResolver analyzes call state events (CSEs) and computes call detail 
# records (CDRs).  It loads CSEs from a database and writes CDRs back into the
# same database.
class CallResolver
  include Observable    # so we can notify Call Resolver plugins of events

  # Constants

  LOCALHOST = 'localhost'

  # How many seconds are there in a day
  SECONDS_IN_A_DAY = 86400

  # Names of events that we send to plugins.
  EVENT_NEW_CDR = 'a new CDR has been created in the database'
  
  # SQL command to garbage-collect and analyze a PostgreSQL database
  POSTGRESQL_TUNE_UP_DATABASE = "VACUUM ANALYZE"

public

  # Methods

  def initialize(config_file = nil)
    @config = CallResolverConfigure.new(config_file)    
  end

  # Run daily processing, including purging and/or call resolution
  def daily_run(purge_flag = false, purge_time = 0)
    run_resolver = true
    do_purge = config.purge?
    
    connect_to_cdr_database
        
    if config.daily_run?
      # Get start time and end time
      start_time = get_daily_start_time      
      end_time = Time.now()
      log.debug{"Daily run - resolve start: #{start_time}, end: #{end_time}"}
    else
      log.error("resolve: the --daily_flag is set, but the daily run is disabled in the configuration");
      run_resolver = false
    end
    
    # Resolve CDRs if enabled
    if run_resolver
      resolve(start_time, end_time)
    end
        
    # Purge if enabled
    if do_purge || purge_flag
      # Was a purge age explicitly set?
      if purge_time != 0
        purge_start_cdr = Time.now() - (SECONDS_IN_A_DAY * purge_time)
        purge_start_cse = purge_start_cdr         
        log.info("Purge override CSEs: #{purge_start_cse}, CDRs: #{purge_start_cdr}")
      else
        purge_start_cdr = config.purge_start_time_cdr
        purge_start_cse = config.purge_start_time_cse
        log.info("Normal purge CSEs: #{purge_start_cse}, CDRs: #{purge_start_cdr}")
      end  
      purge(purge_start_cse, purge_start_cdr)        
    end
  end

  # Resolve CSEs to CDRs
  def resolve(start_time, end_time)
    connect_to_cdr_database
    
    begin
      Cdr.transaction do
        # Default the start time to 1 day before now
        start_time ||= Time.now() - SECONDS_IN_A_DAY
        
        # Default the end_time to 1 day after the start time
        end_time ||= start_time + SECONDS_IN_A_DAY
    
        start_run = Time.now
        log.info("resolve: Resolving calls from #{start_time.to_s} to " +
                 "#{end_time.to_s}.  Running at #{start_run}.")
  
        # Load all CSEs in the time window.  The call_map is a hash where each key
        # is a call ID and the value is an array of events for that call ID,
        # sorted by time.
        # :TODO: For performance/scalability (XPR-144) we can't just load all the
        # data at once.  Split the time window into subwindows and do one
        # subwindow at a time, noting incomplete CDRs and carrying those calls
        # forward into the next subwindow.
        call_map = load_distrib_events_in_time_window(start_time, end_time)
        
        # Resolve each call to yield 0-1 CDRs.  Save the CDRs.
        call_map.each_value do |call|
          resolve_call(call)
        end
  
        end_run = Time.now
        log.info("resolve: Done at #{end_run}.  Analysis took #{end_run - start_run} seconds.")
      end
    rescue
      # Backstop exception handler: don't let any exceptions propagate back
      # to the caller.  Log the error and the stack trace.  The log message has to
      # go on a single line, unfortunately.  Embed "\n" for syslogviewer.
      start_line = "\n        from "    # start each backtrace line with this
      log.error("Exiting because of error: \"#{$!}\"" + start_line +
                $!.backtrace.inject{|trace, line| trace + start_line + line})
    end
  end

  # Give public access to the config
  attr_reader :config

private

  # For each config param, define an accessor that forwards to the config.
  CallResolverConfigure::ALL_PARAMS.each do |sym|
    self.class_eval(%{
      def #{sym}
        config.#{sym}
      end
    })
  end
  
  # Allow other components to use the Call Resolver log, since all logging
  # should go to a single shared file. We have defined the readers above, make
  # them public. 
  public(:log, :log_device)

  # Connect to the CDR database, if not already connected.  The CDR database
  # is the default database for all models.  Because there is only one CDR
  # database, the caller doesn't need to provide an URL.
  def connect_to_cdr_database
    unless ActiveRecord::Base.connected?
      ActiveRecord::Base.establish_connection(cdr_database_url.to_hash)
    end
  end

  # Connect the CallStateEvent class to the CSE database at the specified URL.
  # With HA there are multiple CSE databases.
  def connect_to_cse_database(db_url)    
    log.debug{"connect_to_cse_database: #{db_url}"}
    CallStateEvent.establish_connection(db_url.to_hash)
  end

  # Resolve the call, given an events array for that call, to yield 0-1 CDRs.
  # Persist the CDRs.  Do this as a single transaction.
  def resolve_call(events)
    call_id = events[0].call_id
    log.debug {
      # Build a string listing the event types
      event_types_str =
        events.inject('') {|msg, event| msg + event.event_type + ' '}
      event_types_str[-1] = ')'
      "Resolving a call: call ID = #{call_id} with #{events.length} events (" + event_types_str}
    
    begin
      call_req = find_call_request(events)
      if call_req
        # Read info from the call request and start constructing the CDR.
        cdr = start_cdr(call_req)
        
        # The forking proxy might ring multiple phones.  The dialog with each
        # phone is a separate call leg.
        # Pick the call leg with the best outcome and longest duration to be the
        # basis for the CDR.
        to_tag = best_call_leg(events)

        if to_tag                 # if there are any call legs worth examining
          # Fill the CDR from the call leg events.  The returned status is true
          # if that succeeded, false otherwise.
          status = finish_cdr(cdr, events, to_tag)
        
          if status
            log.debug do
              "resolve_call: Resolved a call from #{cdr.caller_aor} to " +
              "#{cdr.callee_aor} at #{cdr.start_time}, call status = " +
              "#{cdr.termination_text}"
            end
            
            save_cdr(cdr)
          end
        else
          log.debug {"resolve_call: no good call legs found, discarding this call"}
        end
      end
    # Don't let a single bad call blow up the whole run, if the exception was
    # raised by our code.  If it's an exception that we don't know about, then
    # bail out.
    rescue CallResolverException
      log.error("resolve_call: error with call ID = \"#{call_id}\", no CDR created: #{$!}") 
    end
  end

  # Load all events in the time window from HA distributed servers.
  # Return a hash where the key is a call ID and values
  # are event arrays for that call ID, sorted by time.
  def load_distrib_events_in_time_window(start_time, end_time)
    call_map = {}
    
    # all_calls holds arrays, one array of event subarrays for each database.
    # Each subarray holds events for one call, sorted by time.
    all_calls = []
    
    cse_database_urls.each do |db_url|
      connect_to_cse_database(db_url)
      
      # Load events from this database
      events = []
      begin
        events = load_events_in_time_window(start_time, end_time)
        log.debug{"load_distrib_events_in_time_window: loaded #{events.length} " +
                  "events from #{db_url}"}
      rescue
        log.error("load_distrib_events_in_time_window: Unable to load events " +
                  "from the database #{db_url}. The error was: \"#{$!}\".")
      end
      
      # Divide the events into subarrays, one for each call.  Save the result.
      if events.length > 0
        calls = split_events_by_call(events)
        all_calls << calls
      end
    end
    
    # Put the event arrays in the hash table, merging on collisions
    merge_events_for_call(all_calls, call_map)

    call_map
  end
    
  # Put call event arrays in the hash table, merging on collisions. The
  # all_calls arg is an array of arrays, one for each CSE database. Each array
  # contains event arrays, where each event array is for a single call.
  def merge_events_for_call(all_calls, call_map)
    all_calls.each do |calls|
      calls.each do |call|
        call_id = call[0].call_id
        # If there is a hash entry already, then merge this partial call into
        # it, keeping the time sort. Otherwise create a new hash entry.
        # Typically each call comes completely from one server, but that is not
        # always true.
        entry = call_map[call_id]
        if entry
          # merge the events together and re-sort by time
          entry += call
          entry.sort!{|x, y| x.event_time <=> y.event_time}
          
          # not sure why I have to do this -- entry has been modified in place,
          # yes? -- but otherwise the hash entry doesn't get updated correctly
          call_map[call_id] = entry   
        else
          # create a new hash entry
          call_map[call_id] = call
        end
      end
    end
  end

  # Given an events array where the events for a given call are contiguous,
  # split the array into subarrays, one for each call.  Return the array of
  # subarrays.
  def split_events_by_call(events)
    calls = []
    call_start = 0                          # index of first event in the call
    while call_start < events.length
      call_id = events[call_start].call_id  # call ID of current call
      
      # Look for the next event with a different call ID.  If we don't find
      # one, then the current call goes to the end of the events array.
      call_end = events.length - 1
      events[call_start + 1..-1].each_with_index do |event, index|
        if event.call_id != call_id
          call_end = index + call_start
          break
        end
      end
  
      # Add the subarray to the calls array
      calls << events[call_start..call_end]
  
      # On to the next call
      call_start = call_end + 1
    end
    calls
  end

  # Load all events in the time window, sorted by call ID then by time.
  # Return the events in an array.
  def load_events_in_time_window(start_time, end_time)
    events =
      CallStateEvent.find(
        :all,
        :conditions => "event_time >= '#{start_time}' and event_time < '#{end_time}'",
        :order => "call_id, event_time")
    log.debug{"load_events: loaded #{events.length} events between #{start_time} and #{end_time}"}
    events
  end

  # Finds the last generated CDR (if there is one) and returns the 
  # start time. If there is no CDR the return now - 24 hrs.
  def get_daily_start_time
    event = Cdr.find(
                     :first, 
                     :order => "start_time DESC")
    if event == nil
      start_time = Time.now() - SECONDS_IN_A_DAY
    else
      start_time = event.start_time 
    end
    start_time
  end
   
  # Load all events with the given call_id, in ascending chronological order.
  def load_events_with_call_id(call_id)
    events =
      CallStateEvent.find(
        :all,
        :conditions => "call_id = '#{call_id}'",
        :order => "event_time")
    log.debug {"load_events: loaded #{events.length} events with call_id = #{call_id}"}
    events
  end
  
  # Load the call IDs for the specified time window.
  def load_call_ids(start_time, end_time)
    # This query returns an array of objects of class CallStateEvent, but they
    # only have the call_id attribute defined.  Convert to an array of strings
    # and return that.
    results = CallStateEvent.find_by_sql(
      "select distinct call_id from call_state_events " +
      "where event_time >= '#{start_time}' and event_time <= '#{end_time}'")
    results.collect do |obj|
      obj.call_id
    end
  end  
  
  # Given an events array sorted by time, find the call request event, which
  # represents a SIP INVITE message. Return nil if there are no call requests.
  # If there are multiple requests, then pick original INVITEs (no to_tag).
  # If there's more than one of those, then pick the earliest.
  # Note that in the HA case events may be logged on multiple machines, with
  # imperfectly synchronized clocks. 
  def find_call_request(events)
    call_request = nil        # return value
    m = 'find_call_request'   # method name, for debug logging
    
    # Find call requests
    requests = events.find_all {|event| event.call_request?}
    if requests.length == 1
      call_request = requests[0]
      log.debug {"#{m}: found one call request: #{call_request.to_s}"}
    elsif requests.length == 0
      log.debug {"#{m}: found no call requests"}
    else                  # requests.length > 1
      log.debug {
        "#{m}: found #{requests.length} call requests: " +
        Utils.events_to_s(requests)}
      
      # Among the call requests, find original INVITEs
      originals = requests.find_all {|event| !event.to_tag}
      if originals.length == 1
        call_request = originals[0]
        log.debug{"#{m}: found one original call request: #{call_request.to_s}"}
      elsif originals.length == 0
        call_request = requests[0]
        log.debug{"#{m}: found no original call requests, " +
                  "use the earliest non-original request: #{call_request.to_s}"}
      else                # originals.length > 1
        call_request = originals[0]
        log.debug {
          "#{m}: found #{originals.length} original call requests: " +
          Utils.events_to_s(originals) +
          "\nuse the earliest: #{call_request.to_s}"}
      end
    end
    
    call_request
  end

  # Read info from the call request event and start constructing the CDR.
  # Return the new CDR.
  def start_cdr(call_req)
    cdr = Cdr.new(:call_id => call_req.call_id,
                  :from_tag => call_req.from_tag,
                  :caller_aor => call_req.caller_aor,
                  :callee_aor => call_req.callee_aor,
                  :start_time => call_req.event_time,
                  :termination => Cdr::CALL_REQUESTED_TERM)
    cdr.caller_contact = Utils.contact_without_params(call_req.contact)

    cdr
  end

  # Pick the call leg with the best outcome and longest duration to be the
  # basis for the CDR.  Return the to_tag for that call leg.  A call leg is a
  # set of events with a common to_tag.
  # Return nil if there is no such call leg.
  def best_call_leg(events)     # array of events with a given call ID
    to_tag = nil                # result: the to_tag for the best call leg
    
    # Is there at least one call_end event?
    has_call_end = !events.any? {|event| event.call_end?}
    
    # Find the call leg with the best outcome and longest duration. If there is
    # a call_end event, then look for the call end event with the biggest
    # timestamp. Otherwise look for the call failure event with the biggest
    # timestamp. Events have already been sorted for us in timestamp order.
    final_event_type = has_call_end ?
                       CallStateEvent::CALL_FAILURE_TYPE :
                       CallStateEvent::CALL_END_TYPE
    events.reverse_each do |event|
      if event.event_type == final_event_type
        to_tag = event.to_tag
        log.debug {"best_call_leg: to_tag is #{to_tag} for final event #{event.to_s}"}
        break
      end
    end
    
    if !to_tag
      # If there is no final event, then try to at least find a call_setup event
      events.reverse_each do |event|
        if event.call_setup?
          to_tag = event.to_tag
          log.debug {"best_call_leg: to_tag is #{to_tag} for setup event #{event.to_s}"}
          break
        end
      end
    end

    if !to_tag
      log.debug {"best_call_leg: could not find a final event or a setup event"}
    end

    to_tag
  end
  
  # Fill in the CDR from a call leg consisting of the events with the given
  # to_tag.  Return true if successful, false otherwise.
  def finish_cdr(cdr, events, to_tag)
    status = false                # return value: did we fill in the CDR?

    # Get the events for the call leg
    call_leg = events.find_all {|event| event.to_tag == to_tag or
                                        event.from_tag == to_tag}

    # Find the call_setup event
    call_setup = call_leg.find {|event| event.call_setup?}

    if call_setup
      # The call was set up, so mark it provisionally as in progress.
      cdr.termination = Cdr::CALL_IN_PROGRESS_TERM
 
      # We have enough data now to build the CDR.
      status = true
      
      # Get data from the call_setup event
      cdr.callee_contact = Utils.contact_without_params(call_setup.contact)
      cdr.to_tag = call_setup.to_tag
      cdr.connect_time = call_setup.event_time
      
      # Get data from the call_end or call_failure event
      call_end = call_leg.find {|event| event.call_end?}
      if call_end
        cdr.termination = Cdr::CALL_COMPLETED_TERM    # successful completion
        cdr.end_time = call_end.event_time
      else
        # Couldn't find a call_end event, try for call_failure
        status = handle_call_failure(call_leg, cdr)
      end
    else
      # No call_setup event, so look for a call_failure event
      status = handle_call_failure(call_leg, cdr)
    end
    
    status
  end

  # Look for a call_failure event in the call_leg events array.  If we find one,
  # then fill in CDR info and return success.  Otherwise return failure.
  def handle_call_failure(call_leg, cdr)
    status = false      
    call_failure = call_leg.find {|event| event.call_failure?}
    if call_failure
      # found a call_failure event, use it
      # :TODO: consider optimizing space usage by not setting the
      # failure_reason if it is the default value for the failure_status
      # code. For example, the 486 error has the default reason "Busy Here".      
      cdr.to_tag = call_failure.to_tag  # may already be filled in from call_setup
      cdr.termination = Cdr::CALL_FAILED_TERM
      cdr.end_time = call_failure.event_time
      cdr.failure_status = call_failure.failure_status
      cdr.failure_reason = call_failure.failure_reason
      status = true
    end
    status
  end

  # Save the CDR.  Be sure not to create a duplicate CDR for the same dialog.
  # (:TODO: is there a way to avoid querying for the existence of a prior CDR
  # here?  Or maybe we could query just by call_id first and index that column?)
  # Raise a CallResolverException if the save fails for some reason.
  #
  # Assume that this is the only process/thread creating CDRs for these calls.
  # Therefore we don't have to worry about the race condition where after we
  # check for the existence of the CDR and before we save it, another thread
  # saves a CDR with the same call ID.
  def save_cdr(cdr)
    # Continue only if a complete CDR doesn't already exist
    db_cdr = find_cdr(cdr)
    
    # If we found an existing CDR, then log that for debugging
    if db_cdr
      log.debug do
        "save_cdr: found an existing CDR.  Call ID = #{db_cdr.call_id}, " +
        "termination = #{db_cdr.termination}, ID = #{db_cdr.id}."
      end
    end
    
    # Save the CDR as long as there is no existing, complete CDR for that call ID.
    if (!db_cdr or !db_cdr.complete?)
      # Call the Observable method indicating a state change.
      changed
  
      # Notify plugins of the new CDR.
      # "notify_observers" is a method of the Observable module, which is mixed
      # in to the CallResolver.
      notify_observers(EVENT_NEW_CDR,       # event type
                       cdr)                 # the new CDR
      
      # Save the CDR. If there is an incomplete CDR already, then replace it.
      # Call "save!" rather than "save" so that we'll get an exception if the
      # save fails.
      # :TODO: Perhaps just copy values into the CDR in the DB and update it,
      # rather than doing a delete/insert.
      if db_cdr
        db_cdr.destroy
      end
      if cdr.save!
        db_cdr = cdr
      else
        Utils.raise_exception('save_helper_cdr: save failed')
      end
    end
    
    db_cdr
  end
  
  # Given an in-memory CDR, find that CDR in the database and return it.
  # If the CDR is not in the database, then return nil.
  def find_cdr(cdr)
    if !cdr.call_id
      Utils.raise_exception('find_cdr: call_id is nil', ArgumentError)
    end
    
    Cdr.find(
      :first,
      :conditions => "call_id = '#{cdr.call_id}'")
  end

  # Purge records from call state event and cdr tables, making sure
  # to delete unreferenced entries in the parties table.
  def purge(start_time_cse, start_time_cdr)
    connect_to_cdr_database
    
    # Purge CSEs
    CallStateEvent.transaction do
      log.debug{"purge: purging CSEs where event_time <= #{start_time_cse}"}
      CallStateEvent.delete_all(["event_time <= '#{start_time_cse}'"])
    end
    
    # Purge CDRs
    Cdr.transaction do
      log.debug{"purge: purging CDRs where event_time <= #{start_time_cdr}"}
      Cdr.delete_all(["end_time <= '#{start_time_cdr}'"])
    end    
  
    # Garbage-collect deleted records and tune performance.
    # See http://www.postgresql.org/docs/8.0/static/sql-vacuum.html .
    # :TODO: For HA, purge multiple databases, both CSE and CDR
    # Must be done outside of a transaction or we'll get an error.
    ActiveRecord::Base.connection.execute(POSTGRESQL_TUNE_UP_DATABASE)
  end

end    # class CallResolver