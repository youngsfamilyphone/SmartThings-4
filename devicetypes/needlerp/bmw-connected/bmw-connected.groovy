/**
 *  Copyright 2018 Paul Needler (needlerp)
 *
 *  SmartThings Device Handler to interface with BMW Connected web interface as Child Device
 *
 *  Source: https://github.com/needlerp/SmartThings/tree/master/devicetypes/needlerp/bmw-connected
 *
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
 metadata {
     definition (name: "BMW ConnectedDrive", namespace: "needlerp", author: "Paul Needler") {
         capability "Light"
         capability "Lock"
         capability "Presence Sensor"
         capability "Switch"
         capability "Battery"
         capability "Sensor"
         capability "Door Control"

         // Custom attributes
         attribute "txtUpdated", "string"
         attribute "mileage", "number"
         attribute "fuel", "number"
         attribute "refreshState", "string"
         attribute "gps", "string"
         attribute "isHome","string"
         attribute "VIN", "string"
         //attribute "bonnet", "string"
         //attribute "boot", "string"
         //attribute "frontdriver", "string"
         //attribute "FrontPassenger", "string"
         //attribute "RearDriver", "string"
         //attribute "RearPassenger", "string"



         // commands
         command refresh
}

tiles (scale: 2){
  standardTile("lock", "device.lock", width: 2, height: 1) {
    state "UNKNOWN", label: '${currentValue}', action: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/unlocked.png", backgroundColor: "#FFFFFF"
    state "UNLOCKED", label: '${currentValue}', action: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/unlocked.png", backgroundColor: "#FFFFFF"
    state "LOCKED", label: '', action: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/locked.png", backgroundColor: "#FFFFFF"
    state "SECURED", label: '', action: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/locked.png", backgroundColor: "#FFFFFF"
    state "SELECTIVELOCKED", label: '${currentValue}', action: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/unlocked.png", backgroundColor: "#FFFFFF"
}
  valueTile("txtUpdatedTime", "device.txtUpdatedTime", width: 2, height: 1) {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("txtUpdatedDate", "device.txtUpdatedDate", width: 2, height: 1) {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("VIN", "device.VIN", width: 2, height: 1) {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("mileage", "device.mileage", width: 2, height: 1) {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("fuell", "device.fuell", width: 2, height:1, backgroundColor:"#00A0DC") {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("fuelm", "device.fuelm", width: 2, height:1, backgroundColor:"#00A0DC") {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("gpsLat", "device.gpsLat", width: 2, height: 1) {
    state "default", label: '${currentValue}', defaultState: true
  }
  valueTile("gpsLon", "device.gpsLon", width: 2, height: 1) {
    state "default", label: '${currentValue}', defaultState: true
  }
  standardTile("presence", "device.presence", width: 1, height: 1, canChangeBackground: true) {
    state "unknown", label: "Unknown", icon:"st.bmw.bmw-logo-icon", backgroundColor: "#FFFFFF"
    state "present", label: "Home", icon:"st.bmw.bmw-logo-icon", backgroundColor: "#00A0DC"
    state "not present", label: "Away", icon:"st.bmw.bmw-logo-icon", backgroundColor: "#FFFFFF"
  }
  standardTile("isHome", "device.isHome", width: 2, height: 1, canChangeBackground: true) {
    state "unknown", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/isAway.png"
    state "present", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/isHome.png"
    state "not present", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/isAway.png"
  }

  standardTile("refresh", "device.refreshState", width: 2, height: 2) {
   	 state "none", label: "refresh", action: "refresh", icon: "st.secondary.refresh-icon", backgroundColor: "#FFFFFF"
     state "want", label: "refresh", action: "refresh", icon: "st.secondary.refresh-icon",  backgroundColor: "#FFFFFF"
     state "waiting", label: "refresh", action: "refresh", icon: "st.secondary.refresh-icon",  backgroundColor: "#FFFFFF"
}
	carouselTile("map", "device.image", width: 4, height: 2) { }

standardTile("FrontDriver", "device.FrontDriver", width: 2, height: 1) {
    state "unknown", label: "", icon: "", backgroundColor: "#FFFFFF"
    state "closed", label: "", icon: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFD_closed.png", backgroundColor: "#FFFFFF"
    state "door", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFD_d.png", backgroundColor: "#FFFFFF"
    state "window", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFD_w.png", backgroundColor: "#FFFFFF"
    state "doorwindow", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFD_dw.png", backgroundColor: "#FFFFFF"
  }
 standardTile("FrontPassenger", "device.FrontPassenger", width: 2, height: 1) {
    state "unknown", label: "", icon: "", backgroundColor: "#FFFFFF"
    state "closed", label: "", icon: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFP_closed.png", backgroundColor: "#FFFFFF"
    state "door", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFP_d.png", backgroundColor: "#FFFFFF"
    state "window", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFP_w.png", backgroundColor: "#FFFFFF"
    state "doorwindow", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorFP_dw.png", backgroundColor: "#FFFFFF"
  }
  standardTile("RearDriver", "device.RearDriver", width: 2, height: 1) {
    state "unknown", label: "", icon: "", backgroundColor: "#FFFFFF"
    state "closed", label: "", icon: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRD_closed.png", backgroundColor: "#FFFFFF"
    state "door", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRD_d.png", backgroundColor: "#FFFFFF"
    state "window", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRD_w.png", backgroundColor: "#FFFFFF"
    state "doorwindow", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRD_dw.png", backgroundColor: "#FFFFFF"
  }
  standardTile("RearPassenger", "device.RearPassenger", width: 2, height: 1, inactiveLabel: true, decoration:"flat") {
    state "unknown", label: "", icon: "", backgroundColor: "#FFFFFF"
    state "closed", label: "", icon: "https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRP_closed.png", backgroundColor: "#FFFFFF"
    state "door", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRP_d.png", backgroundColor: "#FFFFFF"
    state "window", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRP_w.png", backgroundColor: "#FFFFFF"
    state "doorwindow", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/DoorRP_dw.png", backgroundColor: "#FFFFFF"
  }
  standardTile("Boot", "device.Boot", width: 4, height: 2, inactiveLabel: true, decoration:"flat") {
    state "unknown", label: "", icon: "", backgroundColor: "#FFFFFF"
    state "open", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/Boot_open.png", backgroundColor: "#FFFFFF"
    state "closed", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/Boot_closed.png", backgroundColor: "#FFFFFF"
  }
  standardTile("Bonnet", "device.Bonnet", width: 4, height: 2, inactiveLabel: true, decoration:"flat") {
    state "unknown", label: "", icon: "", backgroundColor: "#FFFFFF"
    state "open", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/Bonnet_open.png", backgroundColor: "#FFFFFF"
    state "closed", label: "", icon:"https://raw.githubusercontent.com/needlerp/SmartThings/master/icons/Bonnet_closed.png", backgroundColor: "#FFFFFF"
  }
}

main(["presence"])
details(["isHome","Bonnet",,"lock","VIN","FrontPassenger","FrontDriver","fuell","RearPassenger","RearDriver","fuelm","Boot","gpsLat","gpsLon","txtUpdatedDate","txtUpdatedTime","refresh"])

}

def refresh() {
    // if we haven't hit refresh in longer than 10 seconds, we'll just start again
    if ((device.currentState("refreshState")?.value == "none")
    	|| (state.refreshTime == null) || ((now() - state.refreshTime) > 10000)) {
    	log.trace "Refreshing vehicle data"
    	sendEvent(name: "refreshState", value: "want", displayed: false)
        state.refreshTime = now()
      //log.trace "calling parent.refreshInfo()"
    	parent.refreshInfo(device.deviceNetworkId)
    }
}
