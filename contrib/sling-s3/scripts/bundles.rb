#!/usr/bin/env ruby

require 'open-uri'
require 'nokogiri'

url = 'https://svn.apache.org/repos/asf/sling/trunk/launchpad/builder/src/main/bundles/list.xml'
result = Nokogiri.XML(open(url).read)

index = 50

result.child.element_children.each do |startLevel|
  level = startLevel['level']
  f = File.open("crank.d/#{index}-sling-startlevel-#{level}.txt", 'w')
  level = 1 if level == 'boot'
  f.puts "defaults crankstart.bundle.start.level #{level}"
  f.puts
  startLevel.element_children.each do |bundle|
    artifactId = bundle.xpath('./artifactId').text
    groupId = bundle.xpath('./groupId').text
    version = bundle.xpath('./version').text
    runModes = bundle.xpath('./runModes').text
    next if runModes == 'jackrabbit'
    f.puts "bundle mvn:#{groupId}/#{artifactId}/#{version}"
  end
  f.close
  index += 5
end
