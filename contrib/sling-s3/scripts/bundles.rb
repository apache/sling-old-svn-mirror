#!/usr/bin/env ruby

require 'open-uri'
require 'rexml/document'
include REXML

url = 'https://svn.apache.org/repos/asf/sling/trunk/launchpad/builder/src/main/bundles/list.xml'
doc = Document.new(open(url).read)

index = 50

doc.root.each_element('startLevel') do |startLevel|
  level = startLevel.attributes['level']
  f = File.open("crank.d/#{index}-sling-startlevel-#{level}.txt", 'w')
  level = 1 if level == 'boot'
  f.puts "defaults crankstart.bundle.start.level #{level}"
  f.puts
  startLevel.each_element('bundle') do |bundle|
    elements = bundle.elements
    artifactId = elements['artifactId'].text
    groupId = elements['groupId'].text
    version = elements['version'].text
    runModes = elements['runModes']
    runModes = runModes.text if runModes
    next if runModes == 'jackrabbit' or runModes == 'oak_mongo'
    f.puts "bundle mvn:#{groupId}/#{artifactId}/#{version}"
  end
  f.close
  index += 5
end
