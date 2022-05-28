package com.example.listacompras

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.listacompras.databinding.ActivityMainBinding
import com.example.listacompras.databinding.ItemBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    lateinit var database: DatabaseReference
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tratarLogin()

        binding.fab.setOnClickListener{
            novoItem()
        }
    }

    fun novoItem(){
        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Teste")
            .setView(editText)
            .setPositiveButton("OK"){ dialog, button ->

                val produto = Produto(nome = editText.text.toString())

                val novoNo = database.child("produtos").push()

                novoNo.key?.let {
                    produto.id = it
                    novoNo.setValue(produto)
                }
            }
            .create()
            .show()

    }

    fun tratarLogin() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            val provedores = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build()
            )

            val intent = AuthUI
                .getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(provedores)
                .build()

            startActivityForResult(intent, 1)
        }
        else{
            conectarFirebase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Toast.makeText(this, "Autenticado", Toast.LENGTH_LONG).show()
        }
        else {
            finishAffinity()
        }
    }

    fun conectarFirebase(){
        val user = FirebaseAuth.getInstance().currentUser

        database = FirebaseDatabase.getInstance().reference.child(user.uid)

        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                tratarDadosProdutos(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Falha de Conexão", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "conectarFirebase", error.toException())
            }
        }

        database.child("produtos").addValueEventListener(listener)
    }

    fun tratarDadosProdutos(snapshot: DataSnapshot){
        val listaProdutos = arrayListOf<Produto>()

        snapshot.children.forEach{
            var produto = it.getValue(Produto::class.java)

            produto?.let {
                listaProdutos.add(it)
            }
        }
        atualizarTela(listaProdutos)
    }

    fun atualizarTela(listaProdutos: List<Produto>){
        binding.container.removeAllViews()

        listaProdutos.forEach{
            val item = ItemBinding.inflate(layoutInflater)

            item.textNome.text = it.nome
            item.checkComprado.isChecked = it.comprado

            binding.container.addView(item.root)
        }
    }
}