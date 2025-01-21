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

    private lateinit var noteTitleInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var saveButton: Button
    private lateinit var noteDao: NoteDao

    private var noteId: Int = -1
    private var originalNoteTitle: String = ""
    private var originalNoteContent: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_note_input, container, false)

        noteTitleInput = view.findViewById(R.id.note_title)
        noteInput = view.findViewById(R.id.messageInput)
        saveButton = view.findViewById(R.id.saveButton)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        // Load arguments
        noteId = arguments?.getInt("noteId") ?: -1
        originalNoteTitle = arguments?.getString("noteTitle") ?: ""
        originalNoteContent = arguments?.getString("noteContent") ?: ""

        if (noteId != 0) {
            // Editing an existing note
            noteTitleInput.setText(originalNoteTitle)
            noteInput.setText(originalNoteContent)
        }
        setupTextWatcher()
        setupSaveButton()
        updateSaveButtonState()

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
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        noteTitleInput.addTextChangedListener(textWatcher)
        noteInput.addTextChangedListener(textWatcher)
    }

    private fun updateSaveButtonState() {
        val currentTitle = noteTitleInput.text.toString().trim()
        val currentContent = noteInput.text.toString().trim()

        // Check if the title or content has changed
        val titleChanged = currentTitle != originalNoteTitle
        val contentChanged = currentContent != originalNoteContent

        // Enable save button:
        // - When editing: Both fields must be non-empty, and one must have changed
        // - When adding: Only content must be non-empty
        saveButton.isEnabled = if (noteId == 0) {
            currentContent.isNotEmpty() // Adding a new note
        } else {
            (titleChanged || contentChanged) && currentContent.isNotEmpty() // Editing a note
        }

        // Update button appearance based on state
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


    private fun saveNote() {
        val title = noteTitleInput.text.toString()
        val newNote = noteInput.text.toString()
        lifecycleScope.launch {
            try {
                val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(newNote)
                val note = Note(
                    id = 0,
                    title = title,
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
        val updatedTitle = noteTitleInput.text.toString()
        val updatedContent = noteInput.text.toString()
        lifecycleScope.launch {
            try {
                val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(updatedContent)
                val updatedNote = Note(
                    id = noteId,
                    title = updatedTitle,
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