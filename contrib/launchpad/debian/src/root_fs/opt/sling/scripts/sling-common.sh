#
#Copyright (c) 2002-08 Chris Lawrence
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without
#modification, are permitted provided that the following conditions
#are met:
#1. Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#2. Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#3. Neither the name of the author nor the names of other contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
#THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
#IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
#WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
#ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
#LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
#CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
#SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
#BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
#WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
#OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
#EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


SLING_DEFAULTS=/etc/default/sling
[ "$DEBUG" ] && set -x
if [ -r $SLING_DEFAULTS ] ; then
	. $SLING_DEFAULTS
else
	echo "Missing $SLING_DEFAULTS file, cannot read env settings." 1>&2
	exit -1
fi

# Make sure premissions & directoreis are OK.
# $1 = force sets all permissions
function check_sling_permissions() {

	chmod +x /etc/init.d/sling

	for d in SLING_DATA SLING_EXEC SLING_CFG SLING_LOG_DIR ; do
		eval n=\$$d
		if [ ! "${n}" ] ; then
			log_failure_msg "Missing $d"
			exit -1
		fi
		if [ ! -d ${n} ] ; then
			echo "Fix type of ${n}"
			rm -rf ${n}
			install --directory --owner=${SLING_USER} --group=${SLING_GROUP} ${n}
		else
			if [ "$(stat -c '%U' ${n})" != "${SLING_USER}" ] || [ "$1" = "force" ]; then
				[ "$1" == "force" ] || echo "Update ownership of ${n}"
				chown -R ${SLING_USER}:${SLING_GROUP} ${n}
			fi
		fi
	done
}
