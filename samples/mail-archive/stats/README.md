Apache Sling mail archive server stats
======================================

This is a work in progress stats module for the Sling mail archive
server.

To demonstrate it, load some data and look at 
http://localhost:8080/content/mailarchiveserver/stats.html

The stats are based on who writes to which destinations, with the
destinations being defined by an OrgMapper service. The current
OrgMapper just generates a destination for mailing list addresses
that it recognizes, and the domain names of other addresses.

Here's how you can load some data from the Sling dev archives,
for example:

export BASE="http://mail-archives.apache.org/mod_mbox"
export SLING="http://localhost:8080"

for year in 2012 2013
do
  for month in 01 02 03 04 05 06 07 08 09 10 11 12
  do
    F=${year}${month}.mbox
    SRC=$BASE/sling-dev/$F
    DEST="$SLING/content/mailarchiveserver/import.mbox.html"
    echo "Importing $SRC to $DEST"
    curl -s $SRC | curl -u admin:admin -Fmboxfile=@- $DEST
  done
done
