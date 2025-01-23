package com.example.bezpiecznynotatnik.fragments

import com.example.bezpiecznynotatnik.data.NoteDao
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.adapters.NotesAdapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var adapter: NotesAdapter? = null
    private lateinit var noteDao: NoteDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
            val notes = noteDao.getAllNotes().sortedByDescending { it.id }

            // Pass the notes to the adapter
            adapter = NotesAdapter(notes.toCollection(ArrayList())) { note, decryptedContent ->
                val displayNoteFragment = DisplayNoteFragment().apply {
                    arguments = Bundle().apply {
                        putInt("noteId", note.id)
                        putString("noteTitle", note.title)
                        putString("noteContent", decryptedContent)
                    }
                }
                displayNoteFragment.show(parentFragmentManager, "DisplayNoteFragment")
            }

            recyclerView.adapter = adapter
        }
    }
}
