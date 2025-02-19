package com.example.bezpiecznynotatnik.fragments

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.data.NoteDao

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class DisplayNoteFragment : BottomSheetDialogFragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var noteDao: NoteDao
    private var noteId: Int = -1
    private var noteTitle: String = ""
    private var noteContent: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_for_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

        val bottomSheet : FrameLayout = dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        // Load arguments
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()
        noteId = arguments?.getInt("noteId") ?: -1
        noteTitle = arguments?.getString("noteTitle").takeUnless { it.isNullOrEmpty() } ?: getString(R.string.untitled)
        noteContent = arguments?.getString("noteContent") ?: "No content available"

        val titleTextView = view.findViewById<TextView>(R.id.note_title)
        val contentTextView = view.findViewById<TextView>(R.id.note_content)
        titleTextView.text = noteTitle
        contentTextView.text = HtmlCompat.fromHtml(noteContent, HtmlCompat.FROM_HTML_MODE_COMPACT).trimEnd()

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        setupBottomSheetBehavior()

        setupToolbar(toolbar)
    }

    private fun setupBottomSheetBehavior() {
        bottomSheetBehavior.apply {
            // Set the initial state to collapsed
            peekHeight = resources.displayMetrics.heightPixels
            state = BottomSheetBehavior.STATE_HALF_EXPANDED

            // Allow the sheet to expand to fullscreen
            isFitToContents = true
            skipCollapsed = true
            isHideable = true
            expandedOffset = 0 // Fullscreen when expanded

            // Optional: Listen for state changes
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {}
                        BottomSheetBehavior.STATE_COLLAPSED -> {}
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            dismiss() // Dismiss fragment if hidden
                        }
                        else -> {}
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Handle slide offset (e.g., change toolbar transparency)
                }
            })
        }
    }
    private fun setupToolbar(toolbar: MaterialToolbar) {
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    val action = NotesFragmentDirections.actionNavDisplayNoteToNavNoteInput(
                        noteId, noteTitle, noteContent
                    )
                    findNavController().navigate(action)
                    dismiss()
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog()
                    false
                }
                else -> false
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_note_dialog_tittle))
            .setMessage(getString(R.string.delete_note_dialog_text))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> deleteNote() }
            .setNegativeButton(getString(R.string.no), null)
            .create()
            .show()
    }

    private fun deleteNote() {
        lifecycleScope.launch {
            try {
                noteDao.deleteById(noteId)

                if (isAdded) {
                    Toast.makeText(requireContext(), getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_notesView)
                }
                dismiss()
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), getString(R.string.delete_note_failure), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}