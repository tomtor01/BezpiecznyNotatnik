package com.example.bezpiecznynotatnik.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.data.Note
import com.example.bezpiecznynotatnik.data.NoteDao
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil
import kotlinx.coroutines.launch

class NoteInputFragment : Fragment() {

    private lateinit var noteInput: EditText
    private lateinit var saveButton: Button
    private lateinit var noteDao: NoteDao

    private var noteId: Int = -1
    private var originalNoteContent: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_input, container, false)

        noteInput = view.findViewById(R.id.messageInput)
        saveButton = view.findViewById(R.id.saveButton)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        // Load arguments
        noteId = arguments?.getInt("noteId") ?: -1
        originalNoteContent = arguments?.getString("noteContent") ?: ""

        if (noteId != 0) {
            // Editing an existing note
            noteInput.setText(originalNoteContent)
        }
        setupSaveButton()
        setupTextWatcher()

        // Adjust padding for soft keyboard
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(16, 16, 16, imeInsets.bottom + 16) // Adjust bottom padding dynamically
            insets
        }
        return view
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            Log.d("NoteInputFragment", "Save button clicked with noteId: $noteId")
            if (noteId == 0) {
                saveNote() // Add new note
            } else {
                updateNote() // Update existing note
            }
        }
    }

    private fun setupTextWatcher() {
        noteInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val contentChanged = s.toString() != originalNoteContent
                saveButton.isEnabled = s?.isNotEmpty() == true && contentChanged

                val (backgroundTintList, textColor) = if (saveButton.isEnabled) {
                    ContextCompat.getColorStateList(requireContext(), R.color.md_theme_primary) to
                            ContextCompat.getColorStateList(requireContext(), R.color.md_theme_onPrimary)
                } else {
                    ContextCompat.getColorStateList(requireContext(), R.color.inactive_button_color) to
                            ContextCompat.getColorStateList(requireContext(), R.color.md_theme_inverseOnSurface)
                }
                saveButton.backgroundTintList = backgroundTintList
                saveButton.setTextColor(textColor)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun saveNote() {
        val newNote = noteInput.text.toString()
        lifecycleScope.launch {
            try {
                val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(newNote)
                val note = Note(
                    id = 0,
                    encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                    iv = ByteArrayUtil.toBase64(iv)
                )
                noteDao.insert(note)

                Toast.makeText(requireContext(), getString(R.string.note_added), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e("NoteFragment", "Error adding note: ${e.message}")
                Toast.makeText(requireContext(), getString(R.string.add_note_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNote() {
        val updatedContent = noteInput.text.toString()
        lifecycleScope.launch {
            try {
                val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(updatedContent)
                val updatedNote = Note(
                    id = noteId,
                    encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                    iv = ByteArrayUtil.toBase64(iv)
                )
                noteDao.update(updatedNote)

                Toast.makeText(requireContext(), getString(R.string.note_updated), Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_notesView)
            } catch (e: Exception) {
                Log.e("NoteFragment", "Error updating note: ${e.message}")
                Toast.makeText(requireContext(), getString(R.string.save_note_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }
}