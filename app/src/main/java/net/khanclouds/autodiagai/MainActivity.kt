package net.khanclouds.autodiagai
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
class MainActivity:AppCompatActivity(){private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);private val obd=ObdEngine();private lateinit var out:TextView;private lateinit var status:TextView
 override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,24);setBackgroundColor(0xFF090B10.toInt())};fun t(s:String,z:Float=16f)=TextView(this).apply{text=s;textSize=z;setTextColor(0xFFF5F6F8.toInt());setPadding(8,12,8,12)}
 root.addView(t("AutoDiag AI",28f));root.addView(t("Diagnostic automobile intelligent • données ECU réelles",14f));status=t("● Déconnecté");root.addView(status)
 fun btn(s:String,f:()->Unit){root.addView(Button(this).apply{text=s;setOnClickListener{f()}})}
 btn("CONNECTER ELM327"){connect()};btn("IDENTIFICATION / VIN"){go{obd.identity()}};btn("TEST RAPIDE"){go{obd.quickScan()}};btn("DONNÉES EN DIRECT"){go{obd.liveData()}};btn("AI DIAGNOSIS"){go{AiDiagnostic.analyse(obd.snapshot())}}
 out=t("Aucune donnée démo. Connecte l'ELM327 puis lance un diagnostic.",15f);root.addView(ScrollView(this).apply{addView(out)},LinearLayout.LayoutParams(-1,0,1f));setContentView(root)}
 private fun go(f:suspend()->String){scope.launch{out.text="Lecture ECU…";out.text=try{withContext(Dispatchers.IO){f()}}catch(e:Exception){"Erreur: "+(e.message?:"inconnue")}}}
 private fun connect(){if(android.os.Build.VERSION.SDK_INT>=31&&ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT),7);return};val d=BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.firstOrNull{it.name?.contains("OBD",true)==true};if(d==null){out.text="Aucun adaptateur OBD appairé.";return};scope.launch{try{withContext(Dispatchers.IO){obd.connect(d)};status.text="● Connecté — "+d.name;out.text="ELM327 connecté."}catch(e:Exception){out.text="Connexion impossible: "+e.message}}}
 override fun onDestroy(){obd.close();scope.cancel();super.onDestroy()}}
