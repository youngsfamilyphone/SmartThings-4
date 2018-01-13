/**
 *  Copyright 2018 Paul Needler (needlerp)
 *
 *  SmartThings Device Handler to interface with BMW Connected web interface as Child Device
 *
 *  Source: https://github.com/needlerp/SmartThings/tree/master/devicetypes/needlerp/bmw-connected
 *
 *
 *  ClientID must be obtained by logging into the BMW-Connected-Drive website and finding the "gcdm-api-key" from the page resources.
 *
 *  Your use of this Device Handler is entirely at your own risk. It is neither officially provided nor sanctioned.
 *
 *  License:
 *   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *   for the specific language governing permissions and limitations under the License.
 *
 **/


definition(
    name: "BMW Connected Drive",
    namespace: "needlerp",
    author: "Paul Needler",
    description: "Creates device handler to interface with BMW Connected Drive ",
    category: "My Apps",
    iconUrl: "https://github.com/needlerp/SmartThings/blob/master/icons/connected.jpg?raw=1",
    iconX2Url: "https://github.com/needlerp/SmartThings/blob/master/icons/connected.jpg?raw=1",
    iconX3Url: "https://github.com/needlerp/SmartThings/blob/master/icons/connected.jpg?raw=1")


preferences {
  page(name:"BMWSettings", title:"Enter your BMW Connected Drive Settings!", content:"BMWSettings")
  page(name:"BMWConnection", title:"Connecting to your BMW!", content:"BMWConnection", refreshTimeout:4)

}


def BMWSettings () {
// clear the refresh count for the next page
state.refreshCount = 0
state.error=""
state.connected = false
state.getBMWinfo = true
log.trace "Initiating BMW ConnectedDrive App..."

  return dynamicPage(name:"BMWSettings", title:"Enter your BMW Connected Drive Settings!", nextPage:"BMWConnection", uninstall: true){
    section("Vehicle Details") {
        input "configEmail", "email", title: "Email Address", required: true, multiple:false, autoCorrect:false
        input "configPassword", "password", title: "Password", required: true, multiple:false, autoCorrect:false
        input "configVIN", "text", title: "Vehicle VIN", required: true, multiple:false, autoCorrect:false
        input "configClientId", "text", title: "Client ID", required: true, multiple:false, autoCorrect:false
        }
    }
}

def BMWConnection () {
  // attempt connection
  if ((state.getBMWinfo == true) || state.getBMWinfo == null) {
    log.trace "Attempting Connection...."
    getBMWInfo()
    getModelInfo()
  }

  // check for timeout error
  state.refreshCount = state.refreshCount+1
  //log.trace state.refreshCount
  if (state.refreshCount > 10) {state.error = "Connection Timeout. Check your login details."}

  if (state.error == "")
  {
    if (!state.connected) {
        // we're waiting for the list to be created
          return dynamicPage(name:"BMWConnection", title:"BMW Connected Drive", nextPage:"", refreshInterval:4, uninstall: true) {
              section("Connecting to Vehicle ${configVIN}") {
                paragraph "This can take a minute. Please wait..."
              }
          }
      } else {
        // we have the list now
          return dynamicPage(name:"BMWConnection", title:"BMW Connected Drive", nextPage:"", install: true, uninstall: true) {
              section("") {
                  paragraph "Your BMW ${state.model} (${state.device}) has been found. Click save to create device."
              }
          }
      }
  }
  else
  {
    def error = state.error

      // clear the error
      state.error = ""

      // show the message
      return dynamicPage(name:"BMWConnection", title:"Connection Error", nextPage:"", uninstall: true) {
        section() {
            paragraph error
          }
      }
  }

}


def getAuth() {
  def authRefresh = needsAuthRefresh()
  if (authRefresh == true || state.authToken == null) {
  log.trace "Getting Auth Token..."
  def params = [
    uri: "https://customer.bmwgroup.com/gcdm/oauth/authenticate",
    requestContentType: "application/x-www-form-urlencoded",
    headers: [
    'Host': "customer.bmwgroup.com",
    'Origin':	"https://customer.bmwgroup.com",
		'Accept-Encoding': "br, gzip, deflate",
		'Content-Type' : "application/x-www-form-urlencoded",
    'User-Agent': "Mozilla/5.0 (iPhone; CPU iPhone OS 11_1_1 like Mac OS X) AppleWebKit/604.3.5 (KHTML, like Gecko) Version/11.0 Mobile/15B150 Safari/604.1"
    ],
    body: [
      'username': "${configEmail}",
      'password': "${configPassword}",
      'client_id': "${configClientId}",
      'response_type': "token",
      'redirect_uri':	"https://www.bmw-connecteddrive.com/app/default/static/external-dispatch.html",
      'scope': "authenticate_user fupo",
      'state': "eyJtYXJrZXQiOiJnYiIsImxhbmd1YWdlIjoiZW4iLCJkZXN0aW5hdGlvbiI6ImxhbmRpbmdQYWdlIiwicGFyYW1ldGVycyI6Int9In0",
      'locale': "GB-en"
    ]
]

try {
  httpPost(params) {resp ->
            if (resp.status == 302) {
            //log.debug "Headers: ${resp.headers.location}"
            def queryParams = resp.headers.location.split('\\#')[1].split('&')
            def mapParams = queryParams.collectEntries { param -> param.split('=').collect { it }}
            state.authToken = mapParams['access_token']
            state.tokenExpires = mapParams['expires_in']
            state.tokenExpires = state.tokenExpires.toInteger()
            def d = new Date();
 				    def n = d.getTime();
            log.debug "Token: ${state.authToken}, expires in ${state.tokenExpires}"
            state.refreshTime = n + state.tokenExpires * 1000
            }
  }
} catch (e) {
  log.debug "Something went wrong: $e"
}
}
}

def getBMWInfo() {
    state.getBMWinfo = false
    getAuth()
    def params = [
      uri: "https://www.bmw-connecteddrive.co.uk/api/vehicle/dynamic/v1/" + configVIN,
      headers: [
        'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 11_1_1 like Mac OS X) AppleWebKit/604.3.5 (KHTML, like Gecko) Version/11.0 Mobile/15B150 Safari/604.1',
        'Authorization': 'Bearer ' + state.authToken
        ]
    ]
    try {
      httpGet(params) {resp ->
                if (resp.status == 200) {
                  state.connected = true
                  //log.trace "Connection succeeded!"
                }
                log.debug "Data: ${resp.data}"
                state.mileage = resp.data.attributesMap.mileage
                state.updatedTime = resp.data.attributesMap.updateTime_converted_time
                state.updatedDate = resp.data.attributesMap.updateTime_converted_date
                state.fuell = resp.data.attributesMap.remaining_fuel
                state.fuelm = resp.data.attributesMap.beRemainingRangeFuelMile
                state.gpsLat = new BigDecimal(resp.data.attributesMap.gps_lat)
                state.gpsLon = new BigDecimal(resp.data.attributesMap.gps_lng)
                state.lock = resp.data.attributesMap.door_lock_state
                state.doorDF = resp.data.attributesMap.door_driver_front
                state.doorPF = resp.data.attributesMap.door_passenger_front
                state.doorDR = resp.data.attributesMap.door_driver_rear
                state.doorPR = resp.data.attributesMap.door_passenger_rear
                state.boot = resp.data.attributesMap.trunk_state
                state.bonnet = resp.data.attributesMap.hood_state
                state.windowDF = resp.data.attributesMap.window_driver_front
                state.windowPF = resp.data.attributesMap.window_passenger_front
                state.windowDR = resp.data.attributesMap.window_driver_rear
                state.windowPR = resp.data.attributesMap.window_passenger_rear


      }
    } catch (e) {
      log.debug "Something went wrong: $e"
    }
}
def getModelInfo() {
    getAuth()
    def params = [
      uri: "https://www.bmw-connecteddrive.co.uk/api/me/vehicles/v2?all=true", //+ configVIN,
      headers: [
        'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 11_1_1 like Mac OS X) AppleWebKit/604.3.5 (KHTML, like Gecko) Version/11.0 Mobile/15B150 Safari/604.1',
        'Authorization': 'Bearer ' + state.authToken
        ]
    ]
    try {
      httpGet(params) {resp ->
                if (resp.status == 200) {
                  state.connected = true
                }
                state.model = resp.data.modelName.toString()
                state.model = state.model.replaceAll('\\[','')
                state.model = state.model.replaceAll('\\]','')
                log.debug "Model: ${state.model}"
      }
    } catch (e) {
      log.debug "Something went wrong: $e"
    }
}

def needsAuthRefresh() {
  def currentDate = new Date()
  def now = currentDate.getTime()
  //log.debug "Now: ${now}"
  //log.debug "Later: ${state.refreshTime}"
  if (now > state.refreshTime) {
    //log.debug "Auth Refresh"
    return true
  }
  //log.trace "Token still valid: ${state.authToken}"
  return false
  }

def installed() {
    log.debug "Installed"
    initialize()
    log.debug "Subscribing to ${state.device}"
	runEvery1Minute(runeveryHandler)
}
def updated() {
    log.debug "Updated"
    unsubscribe()
    initialize()
    log.debug "Subscribing to ${state.device}"
    runEvery1Minute(runeveryHandler)
}
def initialize() {
    //log.debug "Initialising"
    //getBMWInfo()

    if (state.connected) {
      addDevice()
    }

    // Delete any that are no longer in settings
    def childName = "BMW ${state.model}"
	  def delete = getChildDevices().findAll { !childName }
	  //log.info "Delete ${delete}"
    removeChildDevices(delete)
    updateInfo(state.device)
}
def uninstalled() {
	log.debug "Removing app"
    def childName = "BMW ${state.model}"
	def delete = getChildDevices().findAll { !childName }
	removeChildDevices(delete)
}

def addDevice() {
    def s = configVIN
    def device = s?.size() < 7 ? s : s.substring(s.size() - 7)
    state.device = device
    def d = getChildDevice(device)
    if (!d) {
      log.debug "Create New device for $state.model"
      d = addChildDevice ("needlerp", "BMW ConnectedDrive", device, null, [name:"BMW ${state.model}",label:"BMW ${state.model}"])
    } else {
      //log.debug "${device} already exists"
    }
}

private removeChildDevices(delete)
{
if (delete.size() > 0) {
	log.debug "deleting ${delete.size()} Vehicles"
	delete.each {
		state.suppressDelete[it.deviceNetworkId] = true
		deleteChildDevice(it.deviceNetworkId)
		state.suppressDelete.remove(it.deviceNetworkId)
	}
  }
}

private updateInfo(String dni) {
  log.trace "Polling ${state.model}-${dni}"
  def childDevice = getChildDevice(dni)
  if (childDevice) {
  //log.trace "Child Device Found"
  //log.debug "Sending Mileage: ${state.mileage}"
  def d = new Date()
  def t = d.getTime()
  def updateTime = new Date( ((long)t)).format("HH:mm, EEE dd MMM")
  childDevice?.sendEvent(name: 'VIN', value:dni, displayed: true)
  childDevice?.sendEvent(name: 'txtUpdatedTime', value:state.updatedTime, displayed: true)
  childDevice?.sendEvent(name: 'txtUpdatedDate', value:state.updatedDate, displayed: true)
  childDevice?.sendEvent(name: 'mileage', value:state.mileage, displayed: true)
  childDevice?.sendEvent(name: 'fuell', value:state.fuell + ' litres', displayed: true)
  childDevice?.sendEvent(name: 'fuelm', value:state.fuelm + ' miles', displayed: true)
  childDevice?.sendEvent(name: 'gpsLat', value:state.gpsLat+' N')
  childDevice?.sendEvent(name: 'gpsLon', value:state.gpsLon+ ' W')
 def distance = calculateDistance(location.latitude, location.longitude, state.gpsLat, state.gpsLon)

  log.trace "Distance: ${distance}"
  if (state.lock == "SECURED") {
    childDevice?.sendEvent(name: 'lock', value:state.lock, displayed: true)
    if (distance == 0) {
      childDevice?.sendEvent(name: 'presence', value:'present')
      childDevice?.sendEvent(name: 'isHome', value:'present')
    } else {
     childDevice?.sendEvent(name: 'presence', value:'not present')
     childDevice?.sendEvent(name: 'isHome', value:'not present')
    }
  } else {
    childDevice?.sendEvent(name: 'lock', value:state.lock, displayed: true)
    childDevice?.sendEvent(name: 'presence', value:'not present')
    childDevice?.sendEvent(name: 'isHome', value:'not present')
  }
  //Bonnet
  if (state.bonnet == "CLOSED") {
      childDevice?.sendEvent(name: 'Bonnet', value:'closed', displayed: true)
  } else {
  	childDevice?.sendEvent(name: 'Bonnet', value:'open', displayed: true)
  }
  //Boot
  if (state.boot == "CLOSED") {
      childDevice?.sendEvent(name: 'Boot', value:'closed', displayed: true)
  } else {
  	childDevice?.sendEvent(name: 'Boot', value:'open', displayed: true)
  }
  //Front Driver
    log.debug "Front Driver: ${state.doorDF}, ${state.windowDF}"
  if (state.doorDF == "CLOSED") {
  	if (state.windowDF == "CLOSED") {
    		childDevice?.sendEvent(name: 'FrontDriver', value:'closed', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'FrontDriver', value:'window', displayed: true)
        }
  } else {
  	if (state.windowDF == "CLOSED") {
    		childDevice?.sendEvent(name: 'FrontDriver', value:'door', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'FrontDriver', value:'doorwindow', displayed: true)
        }
  }
  //Front Passenger
  log.debug "Front Passenger: ${state.doorPF}, ${state.windowPF}"
  if (state.doorPF == "CLOSED") {
  	if (state.windowPF == "CLOSED") {
    		childDevice?.sendEvent(name: 'FrontPassenger', value:'closed', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'FrontPassenger', value:'window', displayed: true)
        }
  } else {
  	if (state.windowPF == "CLOSED") {
    		childDevice?.sendEvent(name: 'FrontPassenger', value:'door', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'FrontPassenger', value:'doorwindow', displayed: true)
        }
  }
  //Rear Driver
  log.debug "Rear Driver: ${state.doorDR}, ${state.windowDR}"
  if (state.doorDR == "CLOSED") {
  	if (state.windowDR == "CLOSED") {
    		childDevice?.sendEvent(name: 'RearDriver', value:'closed', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'RearDriver', value:'window', displayed: true)
        }
  } else {
  	if (state.windowDR == "CLOSED") {
    		childDevice?.sendEvent(name: 'RearDriver', value:'door', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'RearDriver', value:'doorwindow', displayed: true)
        }
  }
  //Rear Passenger
  log.debug "Rear Passenger: ${state.doorPR}, ${state.windowPR}"
  if (state.doorPR == "CLOSED") {
  	if (state.windowPR == "CLOSED") {
    		childDevice?.sendEvent(name: 'RearPassenger', value:'closed', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'RearPassenger', value:'window', displayed: true)
        }
  } else {
        if (state.windowPR == "CLOSED") {
    		childDevice?.sendEvent(name: 'RearPassenger', value:'door', displayed: true)
    	} else {
        	childDevice?.sendEvent(name: 'RearPassenger', value:'doorwindow', displayed: true)
        }
  }
  } else {
  log.debug "No Child Device Found"
  }
}

def refreshInfo(String dni) {
  log.debug "Force Refresh ${dni}"
  getBMWInfo()
  updateInfo(dni)
}

def runeveryHandler() {
  //log.debug "Handlermethod"
  //log.debug state.device
  //log.debug "Handlermethod: ${state.device}"
  getBMWInfo()
  updateInfo(state.device)
}


    /**
     * Calculate distance based on Haversine algorithmus
     * (thanks to http://www.movable-type.co.uk/scripts/latlong.html)
     *
     * @param latitudeFrom
     * @param longitudeFrom
     * @param latitudeTo
     * @param longitudeTo
     * @return distance in Kilometers
     */
    BigDecimal calculateDistance(BigDecimal latitudeFrom, BigDecimal longitudeFrom,
                                 BigDecimal latitudeTo, BigDecimal longitudeTo) {

        double EARTH_RADIUS = 6371
        def dLat = Math.toRadians(latitudeFrom - latitudeTo)
        def dLon = Math.toRadians(longitudeFrom - longitudeTo)

        //a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
        //distance = 2.EARTH_RADIUS.atan2(√a, √(1−a))
        def a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.cos(Math.toRadians(latitudeFrom)) *
                Math.cos(Math.toRadians(latitudeTo)) * Math.pow(Math.sin(dLon / 2), 2)
        def dist = Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 2 * EARTH_RADIUS
        dist = (dist * 100)
        dist = dist.toInteger() / 100
    return dist
    }
