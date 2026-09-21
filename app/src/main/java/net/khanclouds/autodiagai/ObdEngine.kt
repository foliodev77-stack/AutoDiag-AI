package net.khanclouds.autodiagai
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import com.github.eltonvs.obd.command.control.*
import com.github.eltonvs.obd.command.engine.*
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import kotlinx.coroutines.runBlocking
import java.util.UUID
data class VehicleSnapshot(val vin:String?=null,val dtcs:List<String> = emptyList(),val rpm:Int?=null,val coolant:Int?=null,val voltage:Double?=null)
class ObdEngine {
 private var socket:BluetoothSocket?=null; private var conn:ObdDeviceConnection?=null; private var last=VehicleSnapshot()
 fun connect(d:BluetoothDevice){close();socket=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket!!.connect();conn=ObdDeviceConnection(socket!!.inputStream,socket!!.outputStream)}
 suspend fun identity():String { val c=conn?:error("Non connecté"); val v=c.run(VINCommand(),useCache=false,maxRetries=5).value.trim();last=last.copy(vin=v);return "VIN réel ECU: "+v }
 suspend fun quickScan():String { val c=conn?:error("Non connecté"); val a=listOf(TroubleCodesCommand(),PendingTroubleCodesCommand(),PermanentTroubleCodesCommand()).flatMap { cmd -> runCatching{c.run(cmd,useCache=false,maxRetries=5).value.split(",")}.getOrDefault(emptyList()) }.map{it.trim().uppercase()}.filter{Regex("^[PCBU][0-3][0-9A-F]{3}$").matches(it)&&it!="P0000"}.distinct();last=last.copy(dtcs=a);return if(a.isEmpty()) "TEST RAPIDE\nAucun code défaut OBD-II détecté." else "TEST RAPIDE — DTC ECU RÉELS\n"+a.joinToString("\n") }
 suspend fun liveData():String { val c=conn?:error("Non connecté");fun n(x:String)=x.filter{it.isDigit()||it=='.'||it=='-'}.toDoubleOrNull();val rr=c.run(RPMCommand(),useCache=false,maxRetries=4);val tt=c.run(EngineCoolantTemperatureCommand(),useCache=false,maxRetries=4);val r=n(rr.value)?.toInt();val t=n(tt.value)?.toInt();last=last.copy(rpm=r,coolant=t);return "DONNÉES ECU RÉELLES\nRPM: "+rr.value+" "+rr.unit+"\nLiquide refroidissement: "+tt.value+" "+tt.unit }
 fun snapshot()=last
 fun close(){runCatching{socket?.close()};socket=null;conn=null}
}