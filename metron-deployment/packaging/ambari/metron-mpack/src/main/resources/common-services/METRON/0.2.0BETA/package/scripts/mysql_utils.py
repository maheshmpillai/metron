#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile
from resource_management.libraries.functions.format import format

import mysql_users


def mysql_configure():
    from params import params

    # required for running hive
    replace_bind_address = ('sed', '-i', 's|^bind-address[ \t]*=.*|bind-address = 0.0.0.0|', params.mysql_configname)
    Execute(replace_bind_address,
            sudo=True,
            )

    # this also will start mysql-server
    mysql_users.mysql_adduser()

    File(params.mysql_create_geoip_path,
         mode=0755,
         content=StaticFile('createMysqlGeoIp.sh')
         )

    geoip_setup_cmd = format("bash -x {mysql_create_geoip_path} {daemon_name} {geo_ip_ddl}")
    Execute(geoip_setup_cmd,
            tries=3,
            try_sleep=5,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            )