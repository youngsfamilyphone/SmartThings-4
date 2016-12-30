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
    description: "SmartApp to automatically control a smart parcel box using a Fibaro RGBW controller (Red, Green LED, 12v lock and push-button) plus a Contact Sensor for the box lid.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Box Controller") {
		input "boxController", "capability.lock", required:true, title:"Select Fibaro RGBW as main controller"
	}
    section("Box Lid Sensor") {
    	input "boxSensor", "capability.contactSensor", required:true, title:"Select box lid contact sensor" 
    }
    section ("Operational Hours") {
    	input "unlockTime", "time", title:"Unlock Time", defaultValue:"08:00 am"
        input "lockTime", "time", title:"Lock Time", defaultValue:"22:00 pm"
        input "boxOpenTimeout", "number", title:"How long is the box lid open before error notification sent? (minutes)", defaultValue:"5"
        input "boxClearanceTimeout", "number", title:"How long after full box unlocked for emptying before re-locking? (minutes)", defaultValue:"2"
        input "openTime", "number", title:"How long after a manual box unlock will the box re-lock? (minutes)", defaultValue:"2"
    }
    section("Automatic Unlocking") {
    	input "doorBell", "capability.contactSensor", required:false, title:"Secondary Sensor"
		input "autoOpenCounter", "number", title:"How many seconds after box button pressed is box unlocked?" , defaultValue:"8"
        input "autoWaitTime", "number", title:"How long after doorbell is pressed can box button pushed to start auto-unlock (minutes)", defaultValue:"3"
        input "autoOpenTimeout", "number", title:"How long after auto-open to re-lock if box not opened (minutes)", defaultValue:"2"
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
//	state.boxStatus = "unknown"
	subscribe(boxController, "switchCh4.off", boxButtonhandler) // switchCh4 is the push button
    subscribe(boxController, "autoOpen", autoOpenhandler) // catch changes to auto-open status in app
    subscribe(boxController, "allowDelivery", allowDeliveryhandler) // catch when devicehandler allow delivery button pressed
    subscribe(boxController, "clearBox", clearBoxhandler) // catch when devicehandler clear box button pressed
    subscribe(boxSensor, "contact", boxSensorhandler)
	subscribe(doorBell, "contact", doorBellhandler)
	// add subscriptions for Routines  - allowboxopen and clearbox


// Schedule jobs here
	schedule(unlockTime, morningUnlock)
	schedule(lockTime, nightBox)
}

// Event handlers
def boxButtonhandler(evt) {  // box open request
//	log.trace "boxButtonhandler"
    log.trace "boxStatus: "+ state.boxStatus
    if (state.boxStatus == "empty") {  // box is empty, so it can be unlocked if during working hours
    	def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
        if (between) {
        	log.trace "box unlocked when empty"
            def action = unlockBox()
    	} else { // button pressed, but it's night-time so do nothing
        log.debug "box button pressed out of hours" // do nothing
        }
    	
    } else { // box is full or unknown status
    	sendNotification ("Box Open requested by push-button")
        // look at how we can change red light to flashing
        log.trace "autoOpen state: "+ state.autoOpen
        if (state.autoOpen == "on") { // auto-open after prescribed time
        	if (state.bellState == "pressed") { // doorbell was pressed within time limit
            	runIn(autoOpenCounter, unlockBox)
                sendNotification("Box automatically unlocked")
                runIn(60 * autoOpenTimeout, checkBoxStatus)
            } else {
            	sendNotification("autoOpen fired, but doorbell not pressed")
            }
        } else {
        log.trace "auto-open not true, so wait for manual open"
        // do nothing 
        }
        
    }
}

def boxSensorhandler(evt) {
//	log.trace "boxSensorhandler"
        if (evt.value == "open") { //box is open
        state.boxLid = "open"
        log.trace "box opened"
        state.lidOpened = now()
        runIn(60 * boxOpenTimeout, checkBoxStatus)
    } else { // box was closed
        log.trace "box closed"
        state.boxLid = "closed"
        switch (state.boxStatus) {
        case("empty"):  // box was empty, so assume now full
        	log.trace "box empty"
            sendNotification("Parcel delivered")
            def action = lockBox()
            state.boxStatus = "full"
            break
        case("full"): // box already full, so it's a repeat parcel drop
        	log.trace "box full"
            sendNotification("additional parcel delivered")
        	def action = lockBox()
            state.boxStatus = "full"
            break
        case("clearingfull"): // box just emptied
        	log.trace "box cleared"
            sendNotification("box emptied")
            state.boxStatus = "empty"
           	def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
            if (between) {
            	def action = unlockBox()
            } else {
            	def action = nightBox()
            }
            break
        case("clearingempty"): // box just emptied
        	log.trace "box cleared"
            sendNotification("box emptied")
            state.boxStatus = "empty"
           	def between = timeOfDayIsBetween(unlockTime, lockTime, new Date(), location.timeZone)
            if (between) {
            	def action = unlockBox()
            } else {
            	def action = nightBox()
            }
            break    
        default:
        	log.debug "box closed, unknown status"
            state.boxStatus = "unknown"
    	}
    }
}

def doorBellhandler(evt) {
//log.trace "doorBellhandler"
if (evt.value=="open") // Bell was pushed
	state.bellState = "pressed"
    runIn(60 * autoWaitTime, clearBell)
}

def autoOpenhandler(evt) {
	log.trace "autoOpenhandler"
    state.autoOpen = evt.value
    log.trace "autoOpen: "+state.autoOpen
    } 
    
def allowDeliveryhandler(evt) {
//	log.trace "allowDeliveryhandler"
    def action = allowDelivery()
}    
 
def clearBoxhandler(evt) {
	log.trace "clearBoxhandler"
    def action = clearancerequest()
} 

//Helper methods
def clearBell() {
	log.trace "clearBell"
    state.bellState = null
}

def unlockBox()  {//unlocks box, irrespective of box contents
	log.trace "unlockBox"
    def actionG = boxController.onGreen()
    def actionR = boxController.offRed()
    def actionL = boxController.unlock()
}
 
def lockBox() {
	log.trace "lockBox"
    def actionR = boxController.onRed()
    def actionG = boxController.offGreen()
    def actionL = boxController.lock()
}

def nightBox() {
	log.trace "nightBox"
    log.trace "nightBox boxStatus: "+ state.boxStatus
    def actionG = boxController.offGreen()
    def actionR = boxController.offRed()
    def actionL = boxController.lock()
}
    
def morningUnlock() {
	log.trace "morningUnlock"
    log.trace "morning boxStatus:" + state.boxStatus
    if (state.boxStatus == "empty") {
    	def action = unlockBox()
    } else {
        def action = lockBox()
    }
}

def checkBoxStatus() {
	log.trace "checkBoxStatus"
    log.trace "lid Status: "+ state.boxLid
    log.trace "boxState: " + state.boxStatus
        if (state.boxLid == "open") { //box is still open
        	log.trace "lid is still open - check for timeout"
            log.trace "lidOpened: " + state.lidOpened
            def elapsed = now() - state.lidOpened // how long has it been open for
            log.trace "elapsed: " + elapsed
            def threshold = (1000 * 60 * boxOpenTimeout)
            log.trace "threshold:" + threshold
            if (elapsed >= threshold) {
            	sendNotification ("Parcel box has been left open")
                log.trace "Notification sent: Lid Left Open"
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
                	case("clearingfull"): // box was previously full sore-lock as box not opened
                    	log.trace "checkBoxStatus: clearingfull"
                    	state.boxStatus = "full"
                        def action = lockBox()
                        break
                    case("clearingempty"): // box was empty, clearing button pressed but not opened
                    	log.trace "checkBoxStatus: clearingempty"
                        def action = unlockBox()
                    	break
                    case ("full"):  // re-lock to make sure - manual allow delivery
                    	log.trace "checkBoxStatus: full"
                        state.boxStatus="full"
                    	def action = lockBox()
                        break
                    case ("empty"):
                    	log.trace "checkBoxStatus: empty"
                        state.boxStatus="empty"
                    	def action = unlockBox()
                        break
                    default:
                    	log.trace "checkBoxStatus: unknown status"
                        break
                    }    
                } else {
                	log.trace "checkBoxStatus: nightBox"
                	def action = nightBox()
                }
            }
}

def allowDelivery() { //button pressed, allow remote delivery
	log.trace "allow delivery"
    def action = unlockBox()
    runIn(60*openTime, checkBoxStatus)
}

def clearancerequest() { // button presssed to empty box
	log.trace "clearancerequest"
    log.trace "boxStatus: "+ state.boxStatus
    if (state.boxStatus == "empty" || state.boxStatus == "clearingfull" || state.boxStatus == "unknown") {
    	state.boxStatus = "clearingfull"
    	log.trace "box clearance request submitted"
    	def action = unlockBox()
    	runIn (60*boxClearanceTimeout, checkBoxStatus)
    } else { // box is empty or clearingempty
    	def action = unlockBox()
        log.debug ("box clearance request but box already empty")
        state.boxStatus = "clearingempty"
        runIn (60*boxClearanceTimeout, checkBoxStatus)     
    }
}