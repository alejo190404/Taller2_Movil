package com.example.taller2.Logica

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.Adapter.ContactsAdapter
import com.example.taller2.databinding.ActivityContactsBinding
import com.example.taller2.Datos.Data

class ContactsActivity : AppCompatActivity() {
    lateinit var text: TextView
    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null
    private lateinit var binding: ActivityContactsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pedirPermiso(this, Manifest.permission.READ_CONTACTS, "Se necesita para que la aplicacion funcione", Data.MY_PERMISSION_REQUEST_READ_CONTACTS)

        mProjection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        mContactsAdapter = ContactsAdapter(this, null, 0)
        binding.listView.adapter = mContactsAdapter

        initView()

    }

    fun initView() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            mCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
            )
            mContactsAdapter?.changeCursor(mCursor)
        }
    }


    private fun pedirPermiso(context: Activity, permiso: String, justificacion: String, idCode: Int) {
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permiso)){
                //Request the permission
                Toast.makeText(this, "Deberías aceptarlo. De veritas", Toast.LENGTH_LONG).  show()
            }
            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)
        }
        else{
            initView()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            Data.MY_PERMISSION_REQUEST_READ_CONTACTS -> {
                //If request is cancelled, the result arrrays are empty
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    Toast.makeText(this, "Gracias", Toast.LENGTH_LONG).show()
                    initView()
                } else {
                    Toast.makeText(this, "Funcionalidad limitada", Toast.LENGTH_LONG).show()
                    binding.textView.text = "PERMISO DENEGADO"
                    binding.textView.setTextColor(Color.parseColor("#FF0000")) // Sets the text color to green

                }
            } else -> {

        }
        }
    }
}