#!/usr/bin/env ruby

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

def dep_copy groupId, artifactId, version, dest
  `mkdir -p #{dest}`
  result = run "mvn #{DEP_PLUGIN}:copy -Dartifact=#{groupId}:#{artifactId}:#{version} -DoutputDirectory=#{dest}"
  result.include? 'BUILD SUCCESS'
end

def download groupId, artifactId, version
  puts "#{groupId}:#{artifactId}:#{version}"

  repo_dir = "#{groupId.gsub('.', '/')}/#{artifactId}/#{version}"
  jar_name = "#{artifactId}-#{version}.jar"

  if !OUTPUT.nil?
    if File.exists?(File.expand_path("#{OUTPUT}/#{repo_dir}/#{jar_name}"))
      puts "(/) Already downloaded"
    elsif dep_copy groupId, artifactId, version, "#{OUTPUT}/#{repo_dir}"
      puts "(/) Downloaded"
    else
      puts "(X) Error"
    end
  else
    if File.exists?(File.expand_path("#{LOCAL_REPO}/#{repo_dir}/#{jar_name}"))
      puts "(/) Already installed"
    elsif dep_get groupId, artifactId, version
      puts "(/) Installed to local repo"
    else
      puts "(X) Error"
    end
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
