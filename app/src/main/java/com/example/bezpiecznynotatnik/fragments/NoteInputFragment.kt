package com.example.bezpiecznynotatnik.fragments

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.data.Note
import com.example.bezpiecznynotatnik.data.NoteDao
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil
import com.example.bezpiecznynotatnik.utils.RichEditText

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NoteInputFragment : Fragment() {

    private lateinit var noteTitleInput: EditText
    private lateinit var noteInput: RichEditText
    private lateinit var saveButton: Button
    private lateinit var noteDao: NoteDao

    private lateinit var buttonBold: Button
    private lateinit var buttonItalic: Button
    private lateinit var buttonUnderline: Button

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
        noteInput = view.findViewById(R.id.noteInput)
        saveButton = view.findViewById(R.id.saveButton)
        buttonBold = view.findViewById(R.id.floating_toolbar_button_bold)
        buttonItalic = view.findViewById(R.id.floating_toolbar_button_italic)
        buttonUnderline = view.findViewById(R.id.floating_toolbar_button_underlined)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        // Load arguments
        noteId = arguments?.getInt("noteId") ?: -1
        originalNoteTitle = arguments?.getString("noteTitle") ?: ""
        originalNoteContent = arguments?.getString("noteContent") ?: ""

        setupTextWatcher()

        if (noteId != 0) {
            noteTitleInput.setText(originalNoteTitle)
            noteInput.setText(loadRichContent(originalNoteContent))
        }

        saveButton.setOnClickListener {
            saveNote()
        }
        updateSaveButtonState()
        setupFormattingButtons()

        // Adjust padding for soft keyboard
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(16, 16, 16, imeInsets.bottom + 16) // Adjust bottom padding dynamically
            insets
        }
        return view
    }

    private fun setupFormattingButtons() {
        buttonBold.setOnClickListener {
            noteInput.toggleBold()
            updateButtonStates()
        }

        buttonItalic.setOnClickListener {
            noteInput.toggleItalic()
            updateButtonStates()
        }

        buttonUnderline.setOnClickListener {
            noteInput.toggleUnderline()
            updateButtonStates()
        }

        noteInput.setOnSelectionChangedListener {
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        buttonBold.isSelected = noteInput.isBold()
        buttonItalic.isSelected = noteInput.isItalic()
        buttonUnderline.isSelected = noteInput.isUnderline()
    }

    private fun setupTextWatcher() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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

    private fun saveRichContent(spannable: Spannable): String {
        return HtmlCompat.toHtml(spannable, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    }

    private fun loadRichContent(htmlContent: String): Spannable {
        val spannable = SpannableStringBuilder(
            HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT, { source ->
                // Use CompletableDeferred to handle asynchronous image loading
                val drawableFuture = CompletableDeferred<Drawable?>()
                Glide.with(requireContext())
                    .asDrawable()
                    .load(source)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            resource.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
                            drawableFuture.complete(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                            drawableFuture.complete(null) // Fallback in case of failure
                        }
                    })
                runBlocking {
                    try {
                        drawableFuture.await() // Await the drawable in a coroutine context
                    } catch (e: Exception) {
                        null // Return null if the drawable fails to load
                    }
                }
            }, null)
        )
        return spannable
    }

    private fun saveNote() {
        val title = noteTitleInput.text.toString().trim()
        val noteContentHtml = saveRichContent(noteInput.text ?: SpannableString(""))

        lifecycleScope.launch {
            try {
                val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(noteContentHtml)
                // Create a new Note object
                val note = Note(
                    id = noteId,
                    title = title,
                    encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                    iv = ByteArrayUtil.toBase64(iv)
                )
                if (noteId == 0) {
                    noteDao.insert(note)
                }
                else {
                    noteDao.update(note)
                }

                Toast.makeText(requireContext(), getString(R.string.note_added), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()

            } catch (e: Exception) {
                Log.e("NoteInputFragment", "Error saving note: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.add_note_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }
}