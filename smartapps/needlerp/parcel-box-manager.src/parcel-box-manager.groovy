/**
 *  Parcel Box Manager
 *
 *  Copyright 2016 Paul Needler
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
definition(
    name: "Parcel Box Manager",
    namespace: "needlerp",
    author: "Paul Needler",
    description: "SmartApp to automatically control a smart parcel box using a Fibaro RGBW controller (Red, Green LED, 12v lock and push-button)",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/App-MailboxMonitor.png",
    iconX2Url: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/App-MailboxMonitor@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/App-MailboxMonitor@2x.png")


preferences {
	page(name: "selections") // call dynamic page to allow enumeration of installed routines
    }

def selections() {
	dynamicPage(name: "selections", ttile: "Provide your details", install: true, uninstall: true) {
	section("Box Controller") {
		input("boxController", "capability.lock", required:true, title:"Select Fibaro RGBW as main controller")
    	input("boxSensor", "capability.contactSensor", required:true, title:"Select box lid contact sensor")
    }
    section("Manual box emptying") {
    	input("emptySwitch", "capability.switch", required:false, title:"Select switch to empty box manually (optional)")
    }    
    section ("Operational Hours") {
    	input("unlockTime", "time", title:"Unlock Time", defaultValue:"08:00 am")
        input("lockTime", "time", title:"Lock Time", defaultValue:"22:00 pm")
        }
    section ("Timeout settings") {    
		input("boxOpenTimeout", "number", title:"How long is box lid left open for, before error notification sent? (minutes)", defaultValue:"5")
		input("openTime", "number", title:"How long after a manual box unlock will box re-lock if not opened? (minutes)", defaultValue:"2")
        input("boxClearanceTimeout", "number", title:"How long after clearance request for box emptying will box re-lock if not opened? (minutes)", defaultValue:"2")
		input("numFlashes", "number", title: "How many times should red light flash on after box button pressed (default 20 = 10 seconds)", defaultValue:"20", required: false)
		}
    section("Automatic Unlocking") {
    	input("doorBell", "capability.contactSensor", required:false, title:"Secondary Sensor", submitOnChange: true)
		if (doorBell) {    
        input("autoWaitTime", "number", title:"How long after $doorBell is pressed can box button be pushed to auto-unlock (minutes)", defaultValue:"3")
		input("autoOpenCounter", "number", title:"How many seconds after box button pressed is box unlocked?" , defaultValue:"8")
        input("autoOpenTimeout", "number", title:"How long after automatic unlock will box re-lock if not opened (minutes)", defaultValue:"2")
        	}
            }
	section("Voice Announcements") {
		input("voiceEnabled", "bool", title:"Enable voice announcements?", defaultValue:false, submitOnChange: true)
        if (voiceEnabled) {
		input("sonos", "capability.musicPlayer", title: "Through these speakers:", required: false ,multiple:true)
//        input("resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true)
		input("volume", "number", title: "Temporarily change volume", description: "0-100%", required: false)
		}
        }        

        
    // get the available routines / actions
    def actions = location.helloHome?.getPhrases()*.label
    if (actions) {
            // sort them alphabetically
            actions.sort()
            section("Routines") {
 //              log.trace actions
                // use the actions as the options for an enum input
               input "allowdeliveryRoutine", "enum", title: "Select an action to allow delivery when box is full", options: actions
               input "boxclearanceRoutine", "enum", title: "Select an action to open box for emptying", options: actions
			}
    	}
    }    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(boxController, "switchCh4.on", boxButtonhandler) // switchCh4 is the push button
    subscribe(boxController, "autoOpen", autoOpenhandler) // catch changes to auto-open status in app
    subscribe(boxController, "powerState", powerhandler) // catch changes to auto-open status in app
    subscribe(boxController, "allowDelivery", allowDeliveryhandler) // catch when devicehandler allow delivery button pressed
    subscribe(boxController, "clearBox", clearBoxhandler) // catch when devicehandler clear box button pressed
    subscribe(boxController, "forceLock", forceLockhandler) // catch when devicehandler force lock button pressed
    subscribe(boxController, "forceReset", forceResethandler) //catch when devicehandler force reset/ force Empty button pressed
    subscribe(boxSensor, "contact", boxSensorhandler)
	subscribe(doorBell, "contact", doorBellhandler)
    subscribe(emptySwitch, "switch.on", clearBoxhandler) // catch manual switch to clear box
	// add subscriptions for Routines  - allowboxopen and clearbox
	subscribe(location, "routineExecuted", routinehandler)

// Schedule jobs here
	schedule(unlockTime, morningUnlock)
	schedule(lockTime, nightBox)
}

// Event handlers
def boxButtonhandler(evt) {  // box open request
	log.trace "boxButtonhandler"
    log.trace "boxStatus: "+ state.boxStatus
    if (state.forceOff != true) { // box is turned off within app - do nothing
    if (state.boxStatus == "empty") {  // box is empty, so it can be unlocked if during working hours
    	def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
        if (between) {
        	log.trace "box unlocked when empty"
            def action = unlockBox()
    	} else { // button pressed, but it's night-time so do nothing
        log.debug "box button pressed out of hours" // do nothing
        }
    	
    } else { // box is full or unknown status
        // look at how we can change red light to flashing
        log.trace "autoOpen state: " + state.autoOpen
        def flash = flashRed()
        if (state.autoOpen == "on") { // auto-open after prescribed time
        	if (state.bellState == "pressed") { // doorbell was pressed within time limit
            	def action = setMode("autoOpen")
                runIn(autoOpenCounter, unlockBox)
                sendNotification("Auto Delivery Activated")
                log.info "Auto delivery activated"
                runIn(60 * autoOpenTimeout, checkBoxStatus)
            } else {
            	sendNotification("Delivery Requested (doorbell fail)")
                playTTS("Someone is trying to make a parcel delivery")
                log.info "Delivery Requested (doorbell fail)"
                def mode = setMode("waiting")
            }
        } else {
        log.info "Auto-open off, so wait for manual open"
        sendNotification ("Delivery Requested (manual)")
        playTTS("Someone is trying to make a parcel delivery")
        def action = setMode("waiting")
		runIn(60 * boxClearanceTimeout, checkBoxStatus) 
        }
        
    }
    }
}

def boxSensorhandler(evt) {
//	log.trace "boxSensorhandler"
        if (evt.value == "open") { //box is open
//        state.boxLid = "open"
        def actionl = setlidStatus("open")
        log.info "box lid opened"
        def action = setlidOpened(now())
        //state.lidOpened = now()
        runIn(60 * boxOpenTimeout, checkBoxStatus)
    } else { // box was closed
        log.info "box lid closed"
//        state.boxLid = "closed"
        def actionl = setlidStatus("closed")
        switch (state.boxStatus) {
        case("empty"):  // box was empty, so assume now full
        	log.trace "box empty"
            sendNotification("Parcel delivered")
            playTTS("A parcel has been delivered")
            state.parcelCount = state.parcelCount + 1
            def parcelcount = setparcelCount(state.parcelCount)
            def action = lockBox()
		    def actionM = setMode("locked")
//            state.boxStatus = "full"
			def actionB = setboxStatus("full")
			break
        case("full"): // box already full, so it's a repeat parcel drop
        	log.trace "box full"
            sendNotification("Additional parcel delivered")
            playTTS("A parcel has been delivered")            
            state.parcelCount = state.parcelCount + 1
            def parcelcount = setparcelCount(state.parcelCount)            
        	def action = lockBox()
		    def actionM = setMode("locked")
//            state.boxStatus = "full"
 			def actionB = setboxStatus("full")
			break
        case("clearingfull"): // box just emptied
        	log.trace "box cleared"
            sendNotification("Box emptied")
            state.parcelCount = 0
            def parcelcount = setparcelCount(state.parcelCount)
//            state.boxStatus = "empty"
			def actionS = setboxStatus("empty")
           	def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
            if (between) {
/* code change - even if empty, re-lock box to reduce load on lock
				def action = unlockBox()
				def actionM = setMode("unlocked")
                */
                def action = lockBox()
                def actionM = setMode("locked")
            } else {
            	def action = nightBox()
            }
            break
        case("clearingempty"): // box just emptied
        	log.trace "box cleared when empty"
            sendNotification("Box emptied")
            state.parcelCount = 0
            def parcelcount = setparcelCount(state.parcelCount)
//            state.boxStatus = "empty"
			def actionS = setboxStatus("empty")
           	def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
            if (between) {
/* code change - even if empty, re-lock box to reduce load on lock
				def action = unlockBox()
				def actionM = setMode("unlocked")
                */
                def action = lockBox()
                def actionM = setMode("locked")
            } else {
            	def action = nightBox()
            }
            break    
        default:
        	log.debug "box closed, unknown status"
        	def action = setboxStatus("unknown")
//			state.boxStatus = "unknown"
    	}
    }
}

def doorBellhandler(evt) {
//log.trace "doorBellhandler"
if (evt.value=="open") // Bell was pushed
	state.bellState = "pressed"
    def bellStatus = boxController.setBellStatus("on")
    runIn(60 * autoWaitTime, clearBell)
}

def autoOpenhandler(evt) {
	log.trace "autoOpenhandler"
    state.autoOpen = evt.value
    log.trace "autoOpen: "+state.autoOpen
    } 
    
def powerhandler(evt) { // handle manual power on/off commands from devicehandler
	log.trace "powerhandler: " + evt.value
    switch (evt.value) {
    case ("on"):// power on request
    	log.trace "power on"
        state.forceOff = false
        def action = morningUnlock(false)
        break
    case("disabled"): // force power off
    	log.trace "Disabled"
        state.forceOff = true
        def action = nightBox(false)
        break
    case("off"): //     
    	log.trace "Off"
        state.forceOff = false
        def action = nightBox(false)
        break
}
}

def allowDeliveryhandler(evt) {
//	log.trace "allowDeliveryhandler"
    def action = allowDelivery()
}    
 
def clearBoxhandler(evt) {
	log.trace "clearBoxhandler"
    def action = clearancerequest()
} 

def routinehandler(evt) {
	log.trace "routinehandler"
    switch (evt.displayName) {
    case(allowdeliveryRoutine):
       	log.trace "Allow Delivery called via Routine" 
        def action = allowDelivery()
        break
    case(boxclearanceRoutine):
    	log.trace "Box Clearance called via Routine"
        def action = clearancerequest()
        break 
    }
}

def forceLockhandler(evt) {
	log.trace "forceLockhandler: " + evt.value
    if (evt.value == "forcelock") {
    def action = lockBox()
    def mode = setMode("locked")
    } else if (evt.value == "forceunlock") {
    def action = unlockBox()
    def mode = setMode("unlocked")
    } else { //normal unlock so do nothing
    }
}

def forceResethandler(evt) {
	log.trace "forceResethandler"
/* code change - even if empty, re-lock box to reduce load on lock
				def action = unlockBox()
				def actionM = setMode("unlocked")
                */
	def action = lockBox()
	def actionM = setMode("locked")
	state.parcelCount = 0
    def parcelcount = setparcelCount(state.parcelCount)
    def boxStatus = setboxStatus("empty")  
}

//Helper methods
def clearBell() {
	log.trace "clearBell"
    def bellStatus = boxController.setBellStatus("off")    
    state.bellState = null
}

def unlockBox()  {//unlocks box, irrespective of box contents
	log.trace "unlockBox"
    def actionL = boxController.unlock()
    def actionG = boxController.onGreen()
    def actionR = boxController.offRed()
}
 
def lockBox() {
	log.trace "lockBox"
    def actionL = boxController.lock()
    def actionR = boxController.onRed()
    def actionG = boxController.offGreen()
}

def nightBox(param) {
    unsubscribe()
    if (param != false) {
            boxController.setLockStatus()
    		def actionP = boxController.powerOff(false)
    }
    if (state.forceOff == true) {
       		log.trace "nightBox - Disable"
            def setMode = setMode("disabled")
        } else {
    		log.trace "nightBox - Off"
	        def setMode = setMode("off")
        }
    def offGreen = boxController.offGreen()
    def offRed = boxController.offRed()
    def lock = boxController.lock()
    def disable = disableApp("off") 
	//initialize() don't re-initialise as the box is off - wait until morning so it ignored box opening for maintenance etc.
        subscribe(boxController, "powerState", powerhandler)  // listen only for power on command
}
    
def morningUnlock(param) {
    log.trace "morning Unlock boxStatus: " + state.boxStatus
    unsubscribe()
    if (state.forceOff == false) { // box not manually turned on/off
    if (state.boxStatus == "empty") {
 /* lock the box irrespective of contents
 		log.trace "box empty - unlocking"
    	def action = unlockBox()
        def action1= setMode("unlocked")
    	} else {
    	log.trace "box full - locking"
        */
        def action = lockBox()
        def action1 = setMode("locked")
        }
    def enable = disableApp("on") // enable icons
	if (param != false) {
    	def action = boxController.powerOn()
    	}
	} else {
    	log.trace "morningUnlock but box is disabled - doing nothing"
    }
    initialize()
}

def checkBoxStatus() {
//	log.trace "checkBoxStatus"
    log.trace "lid Status: "+ state.boxLid
    log.trace "boxState: " + state.boxStatus
        if (state.boxLid == "open") { //box is still open
            def elapsed = now() - state.lidOpened // how long has it been open for
            def threshold = (1000 * 60 * boxOpenTimeout)
            if (elapsed >= threshold) {
            	sendNotification ("Parcel box has been left open")
                log.trace "Notification sent: Lid Left Open"
            	playTTS("The parcel box lid has been left open") 
                def action1 = lockBox() // lock the box, just in case there's a problem with the lid sensor
                def action = setMode("error")
            	} else {
                runIn (60 / 2 * boxOpenTimeout, checkBoxStatus) // time threshold not yet reached - schedule to check again shortly    
                log.trace "boxOpenTimeout not yet exceeded - checkBoxStatus re-scheduled"
                }
            } else { //box has been closed
            	log.trace "lid Status: "+state.boxLid
            	log.trace "checkBoxStatus boxStatus" + state.boxStatus
                def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
                if (between) {
                	switch (state.boxStatus) {
                	case("clearingfull"): // box was previously full so re-lock as box not opened
                    	log.trace "checkBoxStatus: clearingfull"
//                    	state.boxStatus = "full"
						def action1 = setboxStatus("full")
                        def action = lockBox()
                        def mode = setMode("locked")
                        break
                    case("clearingempty"): // box was empty, clearing button pressed but not opened
	                  	log.trace "checkBoxStatus: clearingempty"
						/* code change - even if empty, re-lock box to reduce load on lock
				def action = unlockBox()
				def actionM = setMode("unlocked")
                */
                		def action = lockBox()
                		def actionM = setMode("locked")
                    	break
                    case ("full"):  // re-lock to make sure - manual allow delivery
                    	log.trace "checkBoxStatus: full"
						def action1 = setboxStatus("full")
						def action = lockBox()
                        def mode = setMode("locked")
                        break
                    case ("empty"):
                    	log.trace "checkBoxStatus: empty"
						setboxStatus("empty")
                    	/* code change - even if empty, re-lock box to reduce load on lock
				def action = unlockBox()
				def actionM = setMode("unlocked")
                */
                		def action = lockBox()
                		def actionM = setMode("locked")
                        break
                    default:
                    	log.trace "checkBoxStatus: unknown status"
                        break
                    }    
                } else {
                	log.trace "checkBoxStatus: nightBox"
                	def action = nightBox()
                    def mode = setMode("off")
                }
            }
}

def allowDelivery() { //button pressed, allow remote delivery
	log.trace "allow delivery"
	def actionM = setMode("accept")
    sendNotification "Box unlocked to allow delivery"
    def action = unlockBox()
    runIn(60*openTime, checkBoxStatus)
}

def clearancerequest() { // button presssed to empty box
	log.trace "clearancerequest"
    sendNotification "Box unlocked for emptying"
    playTTS("Parcel box unlocked for emptying")
	if (state.boxStatus == "full" || state.boxStatus == "clearingfull" || state.boxStatus == "unknown") {
        def boxStatus = setboxStatus("clearingfull")
        def mode = setMode("clearing")
    	def action = unlockBox()
    	runIn (60*boxClearanceTimeout, checkBoxStatus)
    } else { // box is empty or clearingempty
    	def action = unlockBox()
        log.debug ("box clearance request but box already empty")
		def boxStatus = setboxStatus("clearingempty")
        runIn (60*boxClearanceTimeout, checkBoxStatus)     
    }
}

def setboxStatus(status) {  // set box status and update devicehandler
	state.boxStatus = status
//    log.trace "setboxStatus: " + state.boxStatus
    boxController.setboxStatus(status)
}

def setlidStatus(status) {  // set lid status and update devicehandler
	state.boxLid = status
//    log.trace "boxLid: " + state.boxLid
    boxController.setlidStatus(status)
}  

def setMode(mode) { // captures mode to update devicehandler
	boxController.setMode(mode)
}	
    
def setlidOpened(time) { //time lid last opened
	state.lidOpened = time
    def eventTime = new Date( ((long)time)).format("HH:mm, EEE dd MMM")
    boxController.setlidOpened(eventTime)
    log.trace "lid Opened: "+ eventTime
}

def setparcelCount(val) { // send count of parcels
//	log.trace "parcelCount: "+ val
    boxController.setparcelCount(val)
}

def disableApp(param) { //send disable command to DTH
//	log.trace "disable app: " + param
    boxController.disableApp(param)
    }
    
def flashRed() {
	def numFlashes = numFlashes ?: 6
	log.trace "start flashing red LED $numFlashes times"
	boxController.flashRed(numFlashes)
}

private playTTS(message) {
			if (voiceEnabled) {
			log.trace "Playing Message: "+message
        	message = message.length() >100 ? message[0..90] :message
        	def speech = [uri: "x-rincon-mp3radio://translate.google.com/translate_tts?ie=UTF-8&tl=en&client=tw-ob&q=" + URLEncoder.encode(message, "UTF-8").replaceAll(/\+/,'%20') +"&sf=//s3.amazonaws.com/smartapp-", duration: "${5 + Math.max(Math.round(message.length()/12),2)}"]
            log.trace "speech.uri: "+ speech.uri
			sonos.playTrackAndResume(speech.uri, speech.duration, volume)
            }
}



