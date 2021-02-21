package com.example.printapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.printapp.databinding.ActivityMainBinding
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val binds by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var mPairedDevice = ArrayList<String>()
    private lateinit var mArrayAdapter: ArrayAdapter<String>
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothSocket: BluetoothSocket? = null
    private var mOutputStream: OutputStream? = null

    private var pairedDevice: Set<BluetoothDevice>? = null

    /**
     * Hint: If you are connecting to a Bluetooth serial board then try using
     * the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However
     * if you are connecting to an Android peer then please generate your own unique UUID.
     */

    companion object{
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binds.root)

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        populateSpinnerListBluetooth()

        with(binds){
            btnConnect.setOnClickListener(this@MainActivity)
            btnPrint.setOnClickListener(this@MainActivity)
            btnOpenCash.setOnClickListener(this@MainActivity)
            btnCutPaper.setOnClickListener(this@MainActivity)
        }
    }

    private fun populateSpinnerListBluetooth(){
        mPairedDevice.add("Pilih Device yang telah pairing ...")
        mArrayAdapter = ArrayAdapter<String>(
            this,
            R.layout.support_simple_spinner_dropdown_item,
            mPairedDevice
        )
        binds.spinnerBt.adapter = mArrayAdapter
        binds.spinnerBt.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action != MotionEvent.ACTION_UP) {
                    return false
                }
                try {
                    when {
                        mBluetoothAdapter == null -> {
                            binds.tvNotes.text = "No Bluetooth Adapter"
                            Log.d("BT_APP", "No Bluetooth Adapter")
                        }
                        mBluetoothAdapter!!.isEnabled -> {
                            var name = mBluetoothAdapter?.name
                            pairedDevice = mBluetoothAdapter?.bondedDevices
                            while (mPairedDevice.size > 1) {
                                mPairedDevice.removeAt(1)
                            }
                            if (pairedDevice?.size == 0) {
                                alertDialog("no pairing device listed")
                            }

                            for (device in pairedDevice!!) {
                                name = "${device.name} #${device.address}"
                                mPairedDevice.add(name)
                            }
                        }
                        else -> {
                            Log.d("BT_APP", "Bluetooth Adapter Not Open ...")
                            alertDialog("no pairing device listed")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("BT_APP", e.message!!)
                    binds.tvNotes.text = e.message
                }
                return false
            }
        })
    }

    private fun alertDialog(msg: String){
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage(msg)
        alertDialogBuilder.setCancelable(true)

        alertDialogBuilder.setPositiveButton(
            getString(android.R.string.ok)
        ) { dialog, _ ->
            dialog.cancel()
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnConnect -> {
                var tempString = binds.spinnerBt.selectedItem as String
                if (binds.spinnerBt.selectedItem != 0) {
                    if (binds.btnConnect.text == "Disconnect") {
                        try {
                            mOutputStream?.close()
                            mBluetoothSocket?.close()
                            binds.btnConnect.text = "Disconnect"
                            binds.btnConnect.isEnabled = false
                        } catch (e: Exception) {
                            Log.d("BT_APP", e.message!!)
                            binds.tvNotes.text = e.message
                        }
                        return
                    }
                    tempString = tempString.substring(tempString.length - 17)
                    try {
                        binds.btnConnect.text = "Connecting"
                        mBluetoothDevice = mBluetoothAdapter?.getRemoteDevice(tempString)
                        mBluetoothSocket = mBluetoothDevice?.createRfcommSocketToServiceRecord(
                            SPP_UUID
                        )
                        mBluetoothSocket?.connect()
                        binds.btnConnect.text = "Disconnect"
                        binds.btnConnect.isEnabled = true
                    } catch (e: Exception) {
                        Log.d("BT_APP", e.message!!)
                        binds.btnConnect.text = "Disconnect"
                        binds.btnConnect.isEnabled = false
                    }
                } else {
                    Log.d("BT_APP", "Please connect a bluetooth device")
                }
            }
            R.id.btnPrint -> {
                val etContent = binds.etTextContent.text.toString()
                val contentPrint = contentPrinted()
                try {
                    if (binds.etTextContent.textSize == 0f) {
                        binds.tvNotes.text = "Please fill content you want to print"
                    }
                    mOutputStream = mBluetoothSocket?.outputStream
                    mOutputStream?.apply {
                        val byte1 = byteArrayOf(27, 33, 0)
                        write(byte1)
                        write(contentPrint.encodeToByteArray())
                        flush()
                    }
                    Log.d("BT_APP", "Printed.....")
                } catch (e: Exception) {
                    Log.d("BT_APP", e.message!!)
                }
            }
            R.id.btnOpenCash -> {
                try {
                    if (binds.etTextContent.textSize == 0f) {
                        binds.tvNotes.text = "Please fill content you want to print"
                    }
                    mOutputStream = mBluetoothSocket?.outputStream
                    mOutputStream?.apply {
                        write(byteArrayOf(0x1b, 0x70, 0x00, 0x1e, 0xff.toByte(), 0x00))
                        flush()
                    }
                    Log.d("BT_APP", "Printed.....")
                } catch (e: Exception) {
                    Log.d("BT_APP", e.message!!)
                }
            }
            R.id.btnCutPaper -> {
                try {
                    if (binds.etTextContent.textSize == 0f) {
                        binds.tvNotes.text = "Please fill content you want to print"
                    }
                    mOutputStream = mBluetoothSocket?.outputStream
                    mOutputStream?.apply {
                        write(byteArrayOf(0x0a, 0x0a, 0x1d, 0x56, 0x01))
                        flush()
                    }
                    Log.d("BT_APP", "Printed.....")
                } catch (e: Exception) {
                    Log.d("BT_APP", e.message!!)
                }
            }
        }
    }

    private fun contentPrinted(): String{
        val arrayData = arrayListOf(
            Items("item-001", "20", "2.0", "10.000"),
            Items("item-002", "21", "2.2", "12.000"),
            Items("item-003", "10", "2.4", "15.000"),
            Items("item-004", "23", "2.2", "13.000"),
            Items("item-005", "14", "1.0", "11.000")
        )
        var data =
                "        XXXX BEDDU MART XXX        \n" +
                "     jl. jendral sudirman no 20    \n" +
                "          NO 25 ABC ABCDE          \n"
        data = "$data ------------------------------\n"
        data += String.format("%1$-5s %2$9s %3$7s %4$5s", "Item", "Qty", "Rate", "Total")+"\n"
        for (index in arrayData){
            data += String.format(
                "%1$-5s %2$5s %3$7s %4$5s",
                index.itemId, index.itemQty, index.rate, index.total
            )+ "\n"
        }

        return data
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mBluetoothSocket != null){
                mBluetoothSocket?.close()
            }
        }catch (e: Exception){
            Log.d("BT_APP", e.message!!)
        }
    }

    private fun Int.toByteArray(): ByteArray{
        val b: ByteArray = ByteBuffer.allocate(4).putInt(this).array()
        return b
    }
}

data class Items(
    var itemId: String,
    var itemQty: String,
    var rate: String,
    var total: String
)