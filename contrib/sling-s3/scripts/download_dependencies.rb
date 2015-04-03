#!/usr/bin/env ruby
#
# Copy all rependencies from crank files on command line
# Optionally copy also to output folder
# output folder can e specified but OUTPUT in the enviroment, or --output as a command line arg
# the local maven repo can also be overridden by LOCAL_REPO or --local
# the remote maven repo can also be overridden by REMOTE_REPO or --remote
#
require 'fileutils'
require 'optparse'

DEP_PLUGIN = 'org.apache.maven.plugins:maven-dependency-plugin:2.10'

# Env defaults
$remote_repo = ENV.fetch("REMOTE_REPO",'https://repository.apache.org/content/repositories/snapshots')
$local_repo = ENV.fetch("LOCAL_REPO", File.expand_path("~/.m2/repository"))
# If set, copy artifacts to OUTPUT location, default is ~/.m2 only
$output = ENV["OUTPUT"]

# Override from command line
OptionParser.new do |opt|
  opt.on('-o', '--output OUTPUT, copy to OUTPUT location') { |o| $output = o }
  opt.on('-l', '--local repo location') { |o| $local_repo = o }
  opt.on('-r', '--remote repo location') { |o| $remote_repo = o }
  opt.on('-v', '--verbose') { $verbose = true }
end.parse!

puts "local_repo=#{$local_repo}" if $verbose
puts "output=#{$output}" if $verbose
FileUtils.mkdir_p $output if !$output.nil?

def run cmd
  output = ""
  IO.popen(cmd).each do |line|
    puts line.chomp
    output += line
  end
  output
end

def dep_get groupId, artifactId, version
  cmd = "mvn #{DEP_PLUGIN}:get -Dmaven.repo.local=#{$local_repo} -DremoteRepositories=#{$remote_repo} -Dartifact=#{groupId}:#{artifactId}:#{version} -Dtransitive=false"
  puts cmd if $verbose
  result = run cmd
  result.include? 'BUILD SUCCESS'
end

def download groupId, artifactId, version
  puts "#{groupId}:#{artifactId}:#{version}"

  repo_dir = "#{groupId.gsub('.', '/')}/#{artifactId}/#{version}"
  jar_name = "#{artifactId}-#{version}.jar"

  repo_file = File.expand_path("#{$local_repo}/#{repo_dir}/#{jar_name}")
  if !$output.nil?
    output_dir = File.expand_path("#{$output}/#{repo_dir}")
    output_file = File.expand_path("#{output_dir}/#{jar_name}")
  end
  puts "Retreiving #{repo_file}" if $verbose
  if File.exists?(repo_file) or ( !$output.nil? and File.exists?(output_file) )
    puts "(/) Already downloaded" if $verbose
  else
    dep_get groupId, artifactId, version
    if !File.exists?(repo_file)
      abort "(X) Error downloading #{repo_file}"
    else
      puts "(/) Downloaded" if $verbose
    end
  end
  if !$output.nil? and !File.exists?(output_file)
    FileUtils.rm_rf output_dir
    FileUtils.mkdir_p output_dir
    if !File.exists?(repo_file)
      abort "(X) Missing #{repo_file}"
    else
      puts "(/) #{repo_file} -> #{output_dir}" if $verbose
      FileUtils.cp repo_file, output_dir
    end
  end
end

defaults = Hash.new

ARGV.each do |f|
  File.open(f).each_line do |l|
    defaults.each { |k, v| l[k] &&= v }
    if l =~ /^(?:bundle|classpath) mvn:([^\/]+)\/([^\/]+)\/(.+)$/
      download($1, $2, $3)
    elsif l =~ /^defaults ([^ ]+) ([^ ]+)$/
      defaults["${#{$1}}"] = $2
    end
  end
end
