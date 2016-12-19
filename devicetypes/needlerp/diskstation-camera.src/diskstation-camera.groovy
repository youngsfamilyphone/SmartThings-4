/**
 *  Diskstation Camera
 *
 *  Copyright 2014 David Swanson
 *
 *  Additional coding for live stream view provided by Paul Needler, based on Generic Video Camera
 *  script originally provided by Patrick Stuart.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Diskstation Camera", namespace: "needlerp", author: "Paul Needler") {
      capability "Image Capture"
        capability "Switch"
        capability "Motion Sensor"
        capability "Refresh"
        //NeedlerP: additional live stream capabilities
  		capability "Configuration"
        capability "Switch"
        capability "Video Camera"
		capability "Video Capture"

        attribute "panSupported", "string"
        attribute "tiltSupported", "string"
        attribute "zoomSupported", "string"
        attribute "homeSupported", "string"
        attribute "maxPresets", "string"
        attribute "numPresets", "string"
        attribute "curPreset", "string"
        attribute "numPatrols", "string"
        attribute "curPatrol", "string"
        attribute "refreshState", "string"
        attribute "recordStatus", "string" //Needlerp added due to multiple switch
        attribute "autoTake", "string"
        attribute "takeImage", "string"

        command "left"
    	command "right"
    	command "up"
    	command "down"
        command "zoomIn"
        command "zoomOut"
        command "home"
        command "presetup"
        command "presetdown"
        command "presetgo"
        command "presetGoName", ["string"]
        command "patrolup"
        command "patroldown"
        command "patrolgo"
        command "patrolGoName", ["string"]
        command "refresh"
        command "autoTakeOff"
        command "autoTakeOn"
        command "motionActivated"
        command "motionDeactivate"
        command "initChild"
        command "doRefreshWait"
        command "doRefreshUpdate"
        command "recordEventFailure"
        command "putImageInS3"
 //NeedlerP: Additional command to start streaming
       command "start"
       command "recordon"
       command "recordoff"

	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
			state "default", label: "", action: "", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
		}

		carouselTile("cameraDetails", "device.image", width: 2, height: 2) { }

//Needlerp: additional tile for video streaming
//Needlerp: note the multiAttributTile is automatically scale:2, so the remaining tiles need to be changed to width:2, height:2
        multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 3, height: 2) {
			tileAttribute("device.switch", key: "CAMERA_STATUS") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", action: "switch.off", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", action: "switch.on", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", action: "refresh.refresh", backgroundColor: "#F22000")
			}

			tileAttribute("device.errorMessage", key: "CAMERA_ERROR_MESSAGE") {
				attributeState("errorMessage", label: "", value: "", defaultState: true)
			}

			tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", backgroundColor: "#F22000")
			}

			tileAttribute("device.startLive", key: "START_LIVE") {
				attributeState("live", action: "start", defaultState: true)
			}

			tileAttribute("device.stream", key: "STREAM_URL") {
				attributeState("activeURL", defaultState: true)
			}
       }


		standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
			state "taking", label:'Taking', action: "", icon: "st.camera.dropcam", backgroundColor: "#53a7c0"
			state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
		}

        standardTile("up", "device.tiltSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "up", action: "up", icon: "st.thermostat.thermostat-up"
            state "no", label: "unavail", action: "", icon: "st.thermostat.thermostat-up"
    	}

        standardTile("down", "device.tiltSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "down", action: "down", icon: "st.thermostat.thermostat-down"
            state "no", label: "unavail", action: "", icon: "st.thermostat.thermostat-down"
    	}

        standardTile("left", "device.panSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "left", action: "left", icon: ""
            state "no", label: "unavail", action: "", icon: ""
    	}

		standardTile("right", "device.panSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "right", action: "right", icon: ""
            state "no", label: "unavail", action: "", icon: ""
    	}

        standardTile("zoomIn", "device.zoomSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "zoom in", action: "zoomIn", icon: "st.custom.buttons.add-icon"
            state "no", label: "zoom unavail", action: "", icon: "st.custom.buttons.add-icon"
    	}

        standardTile("zoomOut", "device.zoomSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "zoom out", action: "zoomOut", icon: "st.custom.buttons.subtract-icon"
            state "no", label: "zoom unavail", action: "", icon: "st.custom.buttons.subtract-icon"
    	}


        standardTile("home", "device.homeSupported", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false) {
      		state "yes", label: "home", action: "home", icon: "st.Home.home2"
            state "no", label: "unavail", action: "", icon: "st.Home.home2"
    	}

        standardTile("presetdown", "device.curPreset", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "preset", action: "presetdown", icon: "st.thermostat.thermostat-down"
            state "0", label: "preset", action: "", icon: "st.thermostat.thermostat-down"
    	}

        standardTile("presetup", "device.curPreset", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "preset", action: "presetup", icon: "st.thermostat.thermostat-up"
            state "0", label: "preset", action: "", icon: "st.thermostat.thermostat-up"
    	}

        standardTile("presetgo", "device.curPreset", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false) {
      		state "yes", label: '${currentValue}', action: "presetgo", icon: "st.motion.acceleration.inactive"
            state "0", label: "N/A", action: "", icon: "st.motion.acceleration.inactive"
    	}

        standardTile("patroldown", "device.curPatrol", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "patrol", action: "patroldown", icon: "st.thermostat.thermostat-down"
            state "0", label: "patrol", action: "", icon: "st.thermostat.thermostat-down"
    	}

        standardTile("patrolup", "device.curPatrol", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
      		state "yes", label: "patrol", action: "patrolup", icon: "st.thermostat.thermostat-up"
            state "0", label: "patrol", action: "", icon: "st.thermostat.thermostat-up"
    	}

        standardTile("patrolgo", "device.curPatrol", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false) {
      		state "yes", label: '${currentValue}', action: "patrolgo", icon: "st.motion.motion-detector.active"
            state "0", label: "N/A", action: "", icon: "st.motion.motion-detector.active"
    	}

         standardTile("refresh", "device.refreshState", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false) {
      		state "none", label: "refresh", action: "refresh", icon: "st.secondary.refresh-icon", backgroundColor: "#FFFFFF"
            state "want", label: "refresh", action: "refresh", icon: "st.secondary.refresh-icon",  backgroundColor: "#53A7C0"
            state "waiting", label: "refresh", action: "refresh", icon: "st.secondary.refresh-icon",  backgroundColor: "#53A7C0"
    	}

        standardTile("recordStatus", "device.recordStatus", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false) {
      		state "off", label: "record", action: "recordon", icon: "st.camera.camera", backgroundColor: "#FFFFFF"
    	  	state "on", label: "stop", action: "recordoff", icon: "st.camera.camera",  backgroundColor: "#53A7C0"
	    }

		standardTile("motion", "device.motion", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state("active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0")
			state("inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff")
		}

    	standardTile("auto", "device.autoTake", width: 2, height: 2, canChangeIcon: false, canChangeBackground: false) {
			state "off", label: 'No Take', action: "autoTakeOn", icon: "st.motion.motion.active", backgroundColor: "#ffffff"
			state "on", label: 'Take', action: "autoTakeOff", icon: "st.motion.motion.active", backgroundColor: "#53a7c0"
		}

        main(["motion","videoPlayer"])

//        getAttributes()

/* Needlerp:
* details shortened as my cameras don't have PTZ capability
		details(["videoPlayer",
        		"take", "motion", "recordStatus",
          	  	"presetup", "presetgo", "presetdown",
           	 	"patrolup", "patrolgo", "patroldown",
           	 	"zoomIn", "up", "zoomOut",
           	 	"left", "home", "right",
            	"refresh", "down", "auto","cameraDetails"]) */

         details(["videoPlayer",
        		"take", "motion", "recordStatus",
            	"refresh", "auto","cameraDetails"])

	}

   preferences {
       input "takeStream", "number", title: "Stream to capture image from",
              description: "Leave blank unless want to use another stream.", defaultValue: "",
              required: false, displayDuringSetup: true

//Needlerp: additional preference to enable/disable PTZ cabapilities
//	input title: "", description: "PTZ Capability", type: "paragraph", element: "paragraph"
 //   	input name: "isPTZ", type: "bool", title: "Enable PTZ ?\n", defaultValue: "false", displayDuringSetup: true

    input title: "", description: "Logging", type: "paragraph", element: "paragraph"
    	input name: "isLogLevelTrace", type: "bool", title: "Show trace log level ?\n", defaultValue: "false", displayDuringSetup: true
    	input name: "isLogLevelDebug", type: "bool", title: "Show debug log level ?\n", defaultValue: "true", displayDuringSetup: true
    }
}

mappings {
   path("/getInHomeURL") {
       action:
       [GET: "getInHomeURL"]
   }
}


def start() {
	def cameraId = getCameraID()
    log.debug "Start Streaming Camera " + cameraID

	def hubStreamOutHome = getStreamURL_Child("SYNO.SurveillanceStation.VideoStreaming", "Stream", "cameraId=${cameraId}&format=mjpeg", 1, "OutHome")
    log.trace "hubStreamOutHome: " + hubStreamOutHome
    
    def hubStreamInHome = getStreamURL_Child("SYNO.SurveillanceStation.VideoStreaming", "Stream", "cameraId=${cameraId}&format=mjpeg", 1, "InHome")
    log.trace "hubStreamInHome: " + hubStreamInHome
    
    def dataLiveVideo = [
		OutHomeURL  : "https://" + hubStreamOutHome,
		InHomeURL   : "http://" + hubStreamInHome,
		ThumbnailURL: "http://cdn.device-icons.smartthings.com/camera/dlink-indoor@2x.png",
		cookie      : [key: "key", value: "value"]
	]

	def event = [
		name           : "stream",
		value          : groovy.json.JsonOutput.toJson(dataLiveVideo).toString(),
		data		   : groovy.json.JsonOutput.toJson(dataLiveVideo),
		descriptionText: "Starting the livestream",
		eventType      : "VIDEO",
		displayed      : false,
		isStateChange  : true
	]
//    log.trace event
	sendEvent(event)
    log.trace "Streaming..."
}

def installed() {
	configure()
}

def updated() {
	configure()
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def setProfile(profile) {
	log.trace "setProfile(): ${profile}"
	sendEvent(name: "profile", value: profile, displayed: false)
}


// handle commands
def configure() {
	log.debug "Executing 'configure'"
//    getAttributes()
   	def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.VideoStreaming", "Query", "cameraId=${cameraId}", 1)
    hubAction
    sendEvent(name:"switch", value: "on")
}

def getInHomeURL() {
	def cameraId = getCameraID()
	 [InHomeURL: "http://" + getStreamURL_Child("SYNO.SurveillanceStation.VideoStreaming", "Stream", "cameraId=${cameraId}&format=mjpeg", 1, "InHome")]
}

def getOutHomeURL() {
	def cameraId = getCameraID()
	 [InHomeURL: "https://" + getStreamURL_Child("SYNO.SurveillanceStation.VideoStreaming", "Stream", "cameraId=${cameraId}&format=mjpeg", 1, "OutHome")]
}

def putImageInS3(map) {
	def s3ObjectContent

	try {
		def imageBytes = getS3Object(map.bucket, map.key + ".jpg")

		if(imageBytes)
		{
        	def picName = getPictureName()
			s3ObjectContent = imageBytes.getObjectContent()
			def bytes = new ByteArrayInputStream(s3ObjectContent.bytes)
			storeImage(picName, bytes)
            log.trace "image stored = " + picName
		}

	}
	catch(Exception e) {
		log.error e
	}
	finally {
		//explicitly close the stream
		if (s3ObjectContent) { s3ObjectContent.close() }
	}
}

def getCameraID() {
    def cameraId = parent.getDSCameraIDbyChild(this)
    if (cameraId == null) {
    	log.trace "could not find device DNI = ${device.deviceNetworkId}"
    }
    return (cameraId)
}

// handle commands
def take() {
    try {
    	def lastNum = device.currentState("takeImage")?.integerValue
    	sendEvent(name: "takeImage", value: "${lastNum+1}")
    }
	catch(Exception e) {
		log.error e
        sendEvent(name: "takeImage", value: "0")
	}
    def hubAction = null
    def cameraId = getCameraID()
    if ((takeStream != null) && (takeStream != "")){
    	log.trace "take picture from camera ${cameraId} stream ${takeStream}"
    	hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.Camera", "GetSnapshot", "cameraId=${cameraId}&camStm=${takeStream}", 4)
    }
    else {
    	log.trace "take picture from camera ${cameraId} default stream"
    	hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.Camera", "GetSnapshot", "cameraId=${cameraId}", 1)
    }
    log.debug "take command is: ${hubAction}"
    hubAction
}

def left() {
	log.trace "move"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Move", "cameraId=${cameraId}&direction=left", 1)
    hubAction
}

def right() {
	log.trace "move"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Move", "cameraId=${cameraId}&direction=right", 1)
    hubAction
}

def up() {
	log.trace "move"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Move", "cameraId=${cameraId}&direction=up", 1)
    hubAction
}

def down() {
	log.trace "move"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Move", "cameraId=${cameraId}&direction=down", 1)
    hubAction
}

def zoomIn() {
	log.trace "zoomIn"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Zoom", "cameraId=${cameraId}&control=in", 1)
    hubAction
}

def zoomOut() {
	log.trace "zoomOut"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Zoom", "cameraId=${cameraId}&control=out", 1)
    hubAction
}

def home() {
	log.trace "home"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "Move", "cameraId=${cameraId}&direction=home", 1)
    hubAction
}

def presetup() {
	log.trace "ps up"
    def maxPresetNum = device.currentState("numPresets")?.integerValue
    if (maxPresetNum > 0) {
		def presetNum = state.curPresetIndex
        presetNum = ((presetNum+1) <= maxPresetNum) ? (presetNum + 1) : 1
        state.curPresetIndex = presetNum
    	sendEvent(name: "curPreset", value: parent.getPresetString(this, presetNum))
    }
}

def presetdown() {
	log.trace "ps down"
    def maxPresetNum = device.currentState("numPresets")?.integerValue
    if (maxPresetNum > 0) {
		def presetNum = state.curPresetIndex
        presetNum = ((presetNum-1) > 0) ? (presetNum - 1) : maxPresetNum
        state.curPresetIndex = presetNum
    	sendEvent(name: "curPreset", value: parent.getPresetString(this, presetNum))
    }
}

def presetgo() {
	log.trace "ps go"
    def cameraId = getCameraID()
    def presetIndex = state.curPresetIndex
    def presetNum = parent.getPresetId(this, presetIndex)
    if (presetNum != null) {
    	def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "GoPreset", "cameraId=${cameraId}&presetId=${presetNum}", 1)
    	return hubAction
    }
}

def presetGoName(name) {
	log.trace "ps go name"
    def cameraId = getCameraID()
    def presetNum = parent.getPresetIdByString(this, name);

    if (presetNum != null) {
    	def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "GoPreset", "cameraId=${cameraId}&presetId=${presetNum}", 1)
    	return hubAction
    }
}

def patroldown() {
	log.trace "pt down"
    def patrols = device.currentState("numPatrols")?.integerValue
    if (patrols > 0) {
		def patrolNum = state.curPatrolIndex
        patrolNum = ((patrolNum-1) > 0) ? (patrolNum - 1) : patrols
        state.curPatrolIndex = patrolNum
    	sendEvent(name: "curPatrol", value: parent.getPatrolString(this, patrolNum))
    }
}

def patrolup() {
	log.trace "pt up"
    def patrols = device.currentState("numPatrols")?.integerValue
    if (patrols > 0) {
		def patrolNum = state.curPatrolIndex
        patrolNum = ((patrolNum+1) <= patrols) ? (patrolNum + 1) : 1
        state.curPatrolIndex = patrolNum
    	sendEvent(name: "curPatrol", value: parent.getPatrolString(this, patrolNum))
    }
}

def patrolgo() {
	log.trace "pt go"
    def cameraId = getCameraID()
    def patrolIndex = state.curPatrolIndex
    def patrolNum = parent.getPatrolId(this, patrolIndex)
    if (patrolNum != null) {
    	def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "RunPatrol", "cameraId=${cameraId}&patrolId=${patrolNum}", 2)
    	return hubAction
    }
}

def patrolGoName(name) {
	log.trace "pt go name"
    def cameraId = getCameraID()
    def patrolNum = parent.getPatrolIdByString(this, name);

    if (patrolNum != null) {
    	def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.PTZ", "RunPatrol", "cameraId=${cameraId}&patrolId=${patrolNum}", 2)
    	return hubAction
    }
}

def refresh() {
	log.trace "refresh"

    // if we haven't hit refresh in longer than 10 seconds, we'll just start again
    if ((device.currentState("refreshState")?.value == "none")
    	|| (state.refreshTime == null) || ((now() - state.refreshTime) > 30000)) {
    	log.trace "refresh starting"
    	sendEvent(name: "refreshState", value: "want")
        state.refreshTime = now()
    	parent.refreshCamera(this)
    }
}


// recording on / off
def recordon() {
	  log.trace "start recording"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.ExternalRecording", "Record", "cameraId=${cameraId}&action=start", 2)
    sendEvent(name: "recordStatus", value: "on")
	  return hubAction

}

def recordoff() {
	log.trace "stop recording"
    def cameraId = getCameraID()
    def hubAction = queueDiskstationCommand_Child("SYNO.SurveillanceStation.ExternalRecording", "Record", "cameraId=${cameraId}&action=stop", 2)
	sendEvent(name: "recordStatus", value: "off")
	return hubAction

}

def recordEventFailure() {
	if (device.currentState("recordStatus")?.value == "on") {
    	// recording didn't start, turn it off
    	sendEvent(name: "recordStatus", value: "off")
    }
}

void logDebug(str) {
	if (isLogLevelDebug) {
        log.debug str
	}
}

void logTrace(str) {
	if (isLogLevelTrace) {
        log.trace str
	}
}

def motionActivated() {
    if (device.currentState("motion")?.value != "active") {
    	log.trace "Motion activated"
        sendEvent(name: "motion", value: "active")
        } else {
        log.trace "Motion detected, already active, not sending an event"
    }
}

def motionDeactivate() {
	log.trace "Motion deactivated"
	sendEvent(name: "motion", value: "inactive")
}

def autoTakeOn() {
	log.trace "autoon"
	sendEvent(name: "autoTake", value: "on")
}

def autoTakeOff() {
	log.trace "autooff"
	sendEvent(name: "autoTake", value: "off")
}

def doRefreshWait() {
	sendEvent(name: "refreshState", value: "waiting")
}

def doRefreshUpdate(capabilities) {
	initChild(capabilities)
}

def initChild(Map capabilities)
{
   	sendEvent(name: "panSupported", value: (capabilities.ptzPan) ? "yes" : "no")
    sendEvent(name: "tiltSupported", value: (capabilities.ptzTilt) ? "yes" : "no")
    sendEvent(name: "zoomSupported", value: (capabilities.ptzZoom) ? "yes" : "no")
    sendEvent(name: "homeSupported", value: (capabilities.ptzHome) ? "yes" : "no")

    sendEvent(name: "maxPresets", value: capabilities.ptzPresetNumber)
    def numPresets = parent.getNumPresets(this).toString()
    sendEvent(name: "numPresets", value: numPresets)
    def curPreset = (numPresets == "0") ? 0 : 1
    state.curPresetIndex = curPreset
    sendEvent(name: "curPreset", value: parent.getPresetString(this, curPreset))

    def numPatrols = parent.getNumPatrols(this).toString()
    sendEvent(name: "numPatrols", value: numPatrols)
    def curPatrol = (numPatrols == "0") ? 0 : 1
    state.curPatrolIndex = curPatrol
    sendEvent(name: "curPatrol", value: parent.getPatrolString(this, curPatrol))

    sendEvent(name: "motion", value: "inactive")
    sendEvent(name: "refreshState", value: "none")
    sendEvent(name: "recordStatus", value: "off")
    if (device.currentState("autoTake")?.value == null) {
    	sendEvent(name: "autoTake", value: "off")
    }

    sendEvent(name: "takeImage", value: "0")
}

def queueDiskstationCommand_Child(String api, String command, String params, int version) {
    def commandData = parent.createCommandData(api, command, params, version)
	def hubAction = parent.createHubAction(commandData)
    hubAction

}

def getStreamURL_Child(String api, String command, String params, int version, String location) {
    def commandData = parent.createCommandData(api, command, params, version)

        def hubStream = parent.createStreamURL(commandData, location)
    	hubStream
 
//    log.trace hubStream
}

//helper methods
private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	return device.deviceNetworkId.replaceAll(" ", "_") + "_$pictureUuid" + ".jpg"
}