package com.example.bezpiecznynotatnik.adapters

import com.example.bezpiecznynotatnik.data.Note
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private val notes: ArrayList<Note>,
    private val onViewNote: (Note, String) -> Unit // Pass both Note and decrypted content
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Decrypt note content on-demand
        val decryptedContent = try {
            EncryptionUtil.decryptMessage(
                ByteArrayUtil.fromBase64(note.encryptedMessage),
                ByteArrayUtil.fromBase64(note.iv)
            )
        } catch (e: Exception) {
            holder.itemView.context.getString(R.string.error_decrypting_note)
        }

        // Bind data to the view holder
        holder.bind(note.title, decryptedContent)

        // Handle item click
        holder.itemView.setOnClickListener {
            onViewNote(note, decryptedContent)
        }
    }

    override fun getItemCount() = notes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitle: TextView = itemView.findViewById(R.id.noteTitle)
        private val textContent: TextView = itemView.findViewById(R.id.noteContent)

        fun bind(title: String, content: String) {
            textTitle.text = title.ifEmpty {
                itemView.context.getString(R.string.untitled)
            }
            // Use Html.fromHtml to render formatted text
            textContent.text = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}
