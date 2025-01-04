package com.example.bezpiecznynotatnik.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bezpiecznynotatnik.NoteDao
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil
import com.example.bezpiecznynotatnik.utils.NotesAdapter
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter
    private lateinit var noteDao: NoteDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewNotes)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        setupRecyclerView()
        loadNotes()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadNotes() {
        lifecycleScope.launch {
            val notes = noteDao.getAllNotes()

            val decryptedNotes = notes.map { note ->
                try {
                    EncryptionUtil.decryptMessage(
                        ByteArrayUtil.fromBase64(note.encryptedMessage),
                        ByteArrayUtil.fromBase64(note.iv)
                    )
                } catch (e: Exception) {
                    "Error decrypting note"
                }
            }

            adapter = NotesAdapter(
                decryptedNotes,
                notes,
                onEditNote = { note ->
                    val action = NotesFragmentDirections.actionNavNotesViewToNavEditNote(
                        note.id, decryptedNotes[notes.indexOf(note)]
                    )
                    findNavController().navigate(action)
                }
            )
            recyclerView.adapter = adapter
        }
    }
}