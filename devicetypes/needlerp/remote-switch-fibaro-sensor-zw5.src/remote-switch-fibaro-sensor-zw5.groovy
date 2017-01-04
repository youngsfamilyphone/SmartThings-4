/**
 *  Fibaro Door/Window Sensor ZW5
 *
 *  Copyright 2016 Fibar Group S.A.
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
	definition (name: "Remote Switch (Fibaro Sensor ZW5)", namespace: "needlerp", author: "Paul Needler") {
		capability "Battery"
		capability "Contact Sensor"
		capability "Sensor"
        capability "Configuration"
        capability "Tamper Alert"
		capability "Switch"

        fingerprint deviceId: "0x0701", inClusters: "0x5E, 0x85, 0x59, 0x22, 0x20, 0x80, 0x70, 0x56, 0x5A, 0x7A, 0x72, 0x8E, 0x71, 0x73, 0x98, 0x2B, 0x9C, 0x30, 0x86, 0x84", outClusters: ""
	}

	simulator {
		
	}
        
    tiles(scale: 2) {
    	multiAttributeTile(name:"FGK", type:"lighting", width:6, height:4) {//with generic type secondary control text is not displayed in Android app
        	tileAttribute("device.switch", key:"PRIMARY_CONTROL") {
            	attributeState("on", label:"open", icon:"st.switch.on", backgroundColor:"#ffa81e")
                attributeState("off", label:"closed", icon:"st.switch.off", backgroundColor:"#79b821")
            }
            
            tileAttribute("device.tamper", key:"SECONDARY_CONTROL") {
				attributeState("active", label:'tamper active', backgroundColor:"#53a7c0")
				attributeState("inactive", label:'tamper inactive', backgroundColor:"#ffffff")
			}  
        }
                
        valueTile("battery", "device.battery", inactiveLabel: false, , width: 2, height: 2, decoration: "flat") {
            state "battery", label:'${currentValue}% battery', unit:""
        }
        
       
        main "FGK"
        details(["FGK","battery"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"   
    def result = []
    
    if (description.startsWith("Err 106")) {
		if (state.sec) {
			result = createEvent(descriptionText:description, displayed:false)
		} else {
			result = createEvent(
				descriptionText: "FGK failed to complete the network security key exchange. If you are unable to receive data from it, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				displayed: true,
			)
		}
	} else if (description == "updated") {
		return null
	} else {
    	def cmd = zwave.parse(description, [0x56: 1, 0x71: 3, 0x72: 2, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x98: 1])
    
    	if (cmd) {
    		log.debug "Parsed '${cmd}'"
        	zwaveEvent(cmd)
    	}
    }
}

//security
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x71: 3, 0x84: 2, 0x85: 2, 0x98: 1])
	if (encapsulatedCommand) {
		return zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		createEvent(descriptionText: cmd.toString())
	}
}

//crc16
def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd)
{
    def versions = [0x72: 2, 0x80: 1, 0x86: 1]
	def version = versions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		log.debug "Could not extract command from $cmd"
	} else {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
	//it is assumed that default notification events are used
    //(parameter 20 was not changed before device's re-inclusion)
    def map = [:]
    if (cmd.notificationType == 6) {
    	switch (cmd.event) {                
        	case 22:
            	map.name = "switch"
                map.value = "on"
                map.descriptionText = "${device.displayName}: is on"
            	break
                
            case 23:
            	map.name = "switch"
                map.value = "off"
                map.descriptionText = "${device.displayName}: is off"
            	break
        }
    } else if (cmd.notificationType == 7) {
    	switch (cmd.event) {
        	case 0:
            	map.name = "tamper"
                map.value = "inactive"
                map.descriptionText = "${device.displayName}: tamper alarm has been deactivated"
				break
                
        	case 3:
            	map.name = "tamper"
                map.value = "active"
                map.descriptionText = "${device.displayName}: tamper alarm activated"
            	break
        }
    }
    
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [:]
	map.name = "battery"
	map.value = cmd.batteryLevel == 255 ? 1 : cmd.batteryLevel.toString()
	map.unit = "%"
	map.displayed = true
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {    
    def event = createEvent(descriptionText: "${device.displayName} woke up", displayed: false)
    def cmds = []
    cmds << encap(zwave.batteryV1.batteryGet())
    cmds << "delay 1200"
    cmds << encap(zwave.wakeUpV1.wakeUpNoMoreInformation())
    [event, response(cmds)]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) { 
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
    log.debug "manufacturerName: ${cmd.manufacturerName}"
    log.debug "productId:        ${cmd.productId}"
    log.debug "productTypeId:    ${cmd.productTypeId}"
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) { 
	log.debug "deviceIdData:                ${cmd.deviceIdData}"
    log.debug "deviceIdDataFormat:          ${cmd.deviceIdDataFormat}"
    log.debug "deviceIdDataLengthIndicator: ${cmd.deviceIdDataLengthIndicator}"
    log.debug "deviceIdType:                ${cmd.deviceIdType}"
    
    if (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) {//serial number in binary format
		String serialNumber = "h'"
        
        cmd.deviceIdData.each{ data ->
        	serialNumber += "${String.format("%02X", data)}"
        }
        
        updateDataValue("serialNumber", serialNumber)
        log.debug "${device.displayName} - serial number: ${serialNumber}"
    }
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {	
    updateDataValue("version", "${cmd.applicationVersion}.${cmd.applicationSubVersion}")
    log.debug "applicationVersion:      ${cmd.applicationVersion}"
    log.debug "applicationSubVersion:   ${cmd.applicationSubVersion}"
    log.debug "zWaveLibraryType:        ${cmd.zWaveLibraryType}"
    log.debug "zWaveProtocolVersion:    ${cmd.zWaveProtocolVersion}"
    log.debug "zWaveProtocolSubVersion: ${cmd.zWaveProtocolSubVersion}"
}


/**
 *  COMMAND_CLASS_CONFIGURATION (0x70) : ConfigurationReport
 *
 *  Configuration reports tell us the current parameter values stored in the physical device.
 *
 *  Due to platform security restrictions, the relevent preference value cannot be updated with the actual
 *  value from the device, instead all we can do is output to the SmartThings IDE Log for verification.
 **/
def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    if (state.debug) log.trace "${device.displayName}: zwaveEvent(): ConfigurationReport received: ${cmd}"
    // Translate cmd.configurationValue to an int. This should be returned from zwave.parse() as
    // cmd.scaledConfigurationValue, but it hasn't been implemented by SmartThings yet! :/
    //  See: https://community.smartthings.com/t/zwave-configurationv2-configurationreport-dev-question/9771
    def scaledConfigurationValue = byteArrayToInt(cmd.configurationValue)
    log.info "${device.displayName}: Parameter #${cmd.parameterNumber} has value: ${cmd.configurationValue} (${scaledConfigurationValue})"
}


def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
	log.info "${device.displayName}: received command: $cmd - device has reset itself"
}

def installed() {
	configure
    }
    
def updated() {
	configure
    }

def configure() {
	log.debug "Executing 'configure'"
    
    def cmds = []
    
    cmds += zwave.wakeUpV2.wakeUpIntervalSet(seconds:21600, nodeid: zwaveHubNodeId)//FGK's default wake up interval
    cmds += zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    cmds += zwave.manufacturerSpecificV2.deviceSpecificGet()
    cmds += zwave.versionV1.versionGet()
    cmds += zwave.batteryV1.batteryGet()
    cmds += zwave.associationV2.associationSet(groupingIdentifier:1, nodeId: [zwaveHubNodeId])
    cmds += zwave.wakeUpV2.wakeUpNoMoreInformation()
// Added script here to reverse setting of switch from normally open to normally closed
//	cmds += zwave.configurationV2.configurationGet(parameterNumber: 2) //.format()
//    cmds += zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 2, size: 1)
	cmds += zwave.configurationV2.configurationGet(parameterNumber: 2) //.format()
//	cmds += zwave.switchMultilevelV3.switchMultilevelGet()
    
    encapSequence(cmds, 500)
    
    

}

private secure(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crc16(physicalgraph.zwave.Command cmd) {
	//zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
    "5601${cmd.format()}0000"
}

private encapSequence(commands, delay=200) {
	delayBetween(commands.collect{ encap(it) }, delay)
}

private encap(physicalgraph.zwave.Command cmd) {
    def secureClasses = [0x20, 0x2B, 0x30, 0x5A, 0x70, 0x71, 0x84, 0x85, 0x8E, 0x9C]
    
    //todo: check if secure inclusion was successful
    //if not do not send security-encapsulated command
	if (secureClasses.find{ it == cmd.commandClassId }) {
    	secure(cmd)
    } else {
    	crc16(cmd)
    }
}
/**
 *  byteArrayToInt(byteArray)
 *
 *  Converts an unsigned byte array to a int.
 *  Should use ByteBuffer, but it's not available in SmartThings.
 **/
private byteArrayToInt(byteArray) {
    // return java.nio.ByteBuffer.wrap(byteArray as byte[]).getInt()
    def i = 0
    byteArray.reverse().eachWithIndex { b, ix -> i += b * (0x100 ** ix) }
    return i
}