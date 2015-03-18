#!/usr/bin/env ruby

DEP_PLUGIN = 'org.apache.maven.plugins:maven-dependency-plugin:2.10:get'
SNAPSHOT_REPO = 'https://repository.apache.org/content/repositories/snapshots'
LOCAL_REPO = '~/.m2/repository'

def download groupId, artifactId, version
  puts "#{groupId}:#{artifactId}:#{version}"
  local = "#{LOCAL_REPO}/#{groupId.gsub('.', '/')}/#{artifactId}/#{version}"
  if File.exists?(File.expand_path(local))
    puts "(/) Already installed"
    return
  end
  result = `mvn #{DEP_PLUGIN}\
            -DremoteRepositories=#{SNAPSHOT_REPO}\
            -Dartifact=#{groupId}:#{artifactId}:#{version}`
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
