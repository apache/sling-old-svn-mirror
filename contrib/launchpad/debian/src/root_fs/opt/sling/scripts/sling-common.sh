# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

SLING_DEFAULTS=/etc/default/sling
[ "$DEBUG" ] && set -x
if [ -r $SLING_DEFAULTS ] ; then
	. $SLING_DEFAULTS
else
	echo "Missing $SLING_DEFAULTS file, cannot read env settings." 1>&2
	exit -1
fi

# Make sure premissions & directories are OK.
# $1 = force sets all permissions
function check_sling_permissions() {

	chmod +x /etc/init.d/sling

	for d in SLING_DATA SLING_EXEC SLING_LOG_DIR ; do
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
