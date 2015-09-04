#!/usr/bin/env ruby

require 'open-uri'

def parse_bundle_list name
  url = "https://raw.githubusercontent.com/apache/sling/trunk/launchpad/builder/src/main/provisioning/#{name}"
  sections = []
  section = nil
  open(url).each_line do |line|
    next if line.strip.empty?
    next if line =~ /^\s*#/
    if line =~ /^\[([^ ]+) ?(.*)\]$/
      name = $1
      attributes = {}
      $2.split(' ').map { |a| a.split('=') }.each { |a| attributes[a[0]] = a[1] }
      section = { :name => name, :attr => attributes, :lines => [] }
      sections << section
      next
    end
    next unless section
    section[:lines] << line
  end
  sections
end

def write_variables file, section
  section[:lines].each do |l|
    a = l.strip.split('=')
    file.puts "defaults #{a[0]} #{a[1]}"
  end
  file.puts
end

def write_artifacts file, section
  level = section[:attr]['startLevel'] || 1
  file.puts "defaults crankstart.bundle.start.level #{level}"
  section[:lines].each do |l|
    file.puts "bundle mvn:#{l.strip}"
  end
  file.puts
end

def write_configs file, section
  section[:lines].each do |l|
    l = l[2..-1]
    if l =~ /^  (.+)=I?\[?"([^"]+)"\]?/
      file.puts "  #{$1}=#{$2}"
    elsif l =~ /(.+)-(.+)/
      file.puts "config.factory #{$1}"
    else
      file.puts "config #{l}"
    end
  end
end

def write_config sections, filename
  File.open(filename, 'w') do |f|
    sections.each do |s|
      runModes = s[:attr]['runModes']
      next if ['oak_tar', 'oak_mongo'].include? runModes
      case s[:name]
      when 'variables' then write_variables f, s
      when 'artifacts' then write_artifacts f, s
      when 'configurations' then write_configs f, s
      end
    end
  end
end

index = 40
['standalone.txt', 'boot.txt', 'sling.txt', 'oak.txt'].each do |n|
  sections = parse_bundle_list n
  write_config sections, "crank.d/#{index}-provisioning-#{n}"
  index += 10
end
