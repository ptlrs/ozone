# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

*** Settings ***
Resource            string.robot


*** Variables ***
${OM_SERVICE_ID}     om


*** Keywords ***
Format FS URL
    [arguments]    ${scheme}    ${volume}    ${bucket}    ${path}=${EMPTY}

    IF    '${scheme}' == 'o3fs'
        ${url} =    Format o3fs URL    ${volume}    ${bucket}    ${path}
    ELSE IF    '${scheme}' == 'ofs'
        ${url} =    Format ofs URL     ${volume}    ${bucket}    ${path}
    ELSE
        Fail    Unsupported FS scheme: ${scheme}
    END

    RETURN       ${url}

Format o3fs URL
    [arguments]    ${volume}    ${bucket}    ${path}=${EMPTY}    ${om}=${OM_SERVICE_ID}
    IF    '${om}' != '${EMPTY}'
        ${om_with_leading} =    Ensure Leading    .    ${om}
    ELSE
        ${om_with_leading} =    Set Variable    ${EMPTY}
    END
    RETURN       o3fs://${bucket}.${volume}${om_with_leading}/${path}

Format ofs URL
    [arguments]    ${volume}    ${bucket}    ${path}=${EMPTY}    ${om}=${OM_SERVICE_ID}

    IF    '${om}' != '${EMPTY}'
        ${om_with_trailing} =    Ensure Trailing   /    ${om}
    ELSE
        ${om_with_trailing} =    Set Variable    ${EMPTY}
    END

    IF    '${path}' != '${EMPTY}'
        ${path_with_leading} =    Ensure Leading    /    ${path}
    ELSE
        ${path_with_leading} =    Set Variable    ${EMPTY}
    END

    RETURN       ofs://${om_with_trailing}${volume}/${bucket}${path_with_leading}
