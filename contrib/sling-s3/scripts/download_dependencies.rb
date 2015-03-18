#!/usr/bin/env ruby

DEP_PLUGIN = 'org.apache.maven.plugins:maven-dependency-plugin:2.10:get'
SNAPSHOT_REPO = 'https://repository.apache.org/content/repositories/snapshots'
LOCAL_REPO = '~/.m2/repository'

def run cmd
  output = ""
  IO.popen(cmd).each do |line|
    puts line.chomp
    output += line
  end
  output
end

def download groupId, artifactId, version
  puts "#{groupId}:#{artifactId}:#{version}"
  local = "#{LOCAL_REPO}/#{groupId.gsub('.', '/')}/#{artifactId}/#{version}/#{artifactId}-#{version}.jar"
  if File.exists?(File.expand_path(local))
    puts "(/) Already installed"
    return
  end
  result = run "mvn #{DEP_PLUGIN} -DremoteRepositories=#{SNAPSHOT_REPO} -Dartifact=#{groupId}:#{artifactId}:#{version} -Dtransitive=false"
  if result.include? 'BUILD SUCCESS'
    puts "(/) Downloaded"
  else
    puts "(X) Error\n#{result}"
  end
end

defaults = Hash.new

ARGV.each do |f|
  File.open(f).each_line do |l|
    defaults.each { |k, v| l[k] &&= v }
    if l =~ /^bundle mvn:([^\/]+)\/([^\/]+)\/(.+)$/
      download($1, $2, $3)
    elsif l =~ /^defaults ([^ ]+) ([^ ]+)$/
      defaults["${#{$1}}"] = $2
    end
  end
end
