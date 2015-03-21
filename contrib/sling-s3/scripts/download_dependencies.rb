#!/usr/bin/env ruby
require 'fileutils'

DEP_PLUGIN = 'org.apache.maven.plugins:maven-dependency-plugin:2.10'
SNAPSHOT_REPO = 'https://repository.apache.org/content/repositories/snapshots'
LOCAL_REPO = '~/.m2/repository'
# If set, copy artifacts to OUTPUT location, default is ~/.m2 only
OUTPUT = ENV["OUTPUT"]

def run cmd
  output = ""
  IO.popen(cmd).each do |line|
    puts line.chomp
    output += line
  end
  output
end

def dep_get groupId, artifactId, version
  result = run "mvn #{DEP_PLUGIN}:get -DremoteRepositories=#{SNAPSHOT_REPO} -Dartifact=#{groupId}:#{artifactId}:#{version} -Dtransitive=false"
  result.include? 'BUILD SUCCESS'
end

def download groupId, artifactId, version
  puts "#{groupId}:#{artifactId}:#{version}"

  repo_dir = "#{groupId.gsub('.', '/')}/#{artifactId}/#{version}"
  jar_name = "#{artifactId}-#{version}.jar"

  repo_file = File.expand_path("#{LOCAL_REPO}/#{repo_dir}/#{jar_name}")
  puts "Retreiving #{repo_file}"
  if File.exists?(repo_file)
    puts "(/) Already downloaded"
  elsif dep_get groupId, artifactId, version
    puts "(/) Downloaded"
  else
    abort "(X) Error"
  end
  if !OUTPUT.nil?
    output_dir = File.expand_path("#{OUTPUT}/#{repo_dir}")
    FileUtils.rm_rf output_dir
    FileUtils.mkdir_p output_dir
    if !File.exists?(repo_file)
      abort "(X) Missing "
    else
      puts "(/) #{repo_file} -> #{output_dir}"
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
