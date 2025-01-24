package com.example.bezpiecznynotatnik.utils

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.text.getSpans
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat


class RichEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private var selectionChangedListener: (() -> Unit)? = null
    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        var inputConnection = super.onCreateInputConnection(outAttrs)

        // Declare supported MIME types
        val mimeTypes = arrayOf("image/gif", "image/png", "image/jpeg", "image/webp")
        EditorInfoCompat.setContentMimeTypes(outAttrs, mimeTypes)

        // Wrap input connection for commitContent compatibility
        inputConnection = RichInputConnectionWrapper(inputConnection, true, this)
        inputConnection = InputConnectionCompat.createWrapper(this, inputConnection, outAttrs)

        return inputConnection
    }

    fun toggleBold() {
        isBoldActive = !isBoldActive
        applyFormattingToSelection(Typeface.BOLD, isBoldActive)
    }

    fun toggleItalic() {
        isItalicActive = !isItalicActive
        applyFormattingToSelection(Typeface.ITALIC, isItalicActive)
    }

    fun toggleUnderline() {
        isUnderlineActive = !isUnderlineActive
        applyUnderlineFormatting(isUnderlineActive)
    }

    private fun applyFormattingToSelection(style: Int, enable: Boolean) {
        val start = selectionStart
        val end = selectionEnd
        val editable = text ?: return

        if (start == end) {
            // No selection: only update active formatting state
            return
        }

        val spans = editable.getSpans<StyleSpan>(start, end)
        spans.filter { it.style == style }.forEach { editable.removeSpan(it) }

        if (enable) {
            editable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyUnderlineFormatting(enable: Boolean) {
        val start = selectionStart
        val end = selectionEnd
        val editable = text ?: return

        if (start == end) {
            // No selection: only update active formatting state
            return
        }

        val spans = editable.getSpans<UnderlineSpan>(start, end)
        spans.forEach { editable.removeSpan(it) }

        if (enable) {
            editable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun isBold(): Boolean {
        return isStyleActive(Typeface.BOLD)
    }

    fun isItalic(): Boolean {
        return isStyleActive(Typeface.ITALIC)
    }

    fun isUnderline(): Boolean {
        val spans = text?.getSpans<UnderlineSpan>(selectionStart, selectionEnd)
        return spans?.isNotEmpty() == true
    }

    private fun isStyleActive(style: Int): Boolean {
        val spans = text?.getSpans<StyleSpan>(selectionStart, selectionEnd)
        return spans?.any { it.style == style } == true
    }

    fun setOnSelectionChangedListener(listener: () -> Unit) {
        selectionChangedListener = listener
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        selectionChangedListener?.invoke()
    }

    init {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyActiveFormattingToNewText()
            }
        })
    }

    private fun applyActiveFormattingToNewText() {
        val start = selectionStart
        val end = selectionEnd
        val editable = text ?: return

        if (start > 0) {
            // Apply active formatting to newly entered text
            if (isBoldActive) {
                editable.setSpan(StyleSpan(Typeface.BOLD), start - 1, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (isItalicActive) {
                editable.setSpan(StyleSpan(Typeface.ITALIC), start - 1, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (isUnderlineActive) {
                editable.setSpan(UnderlineSpan(), start - 1, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun insertRichContent(uri: Uri) {
        Log.d("RichEditText", "Processing URI: $uri")

        try {
            // Open InputStream for the URI
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                Glide.with(context)
                    .asDrawable()
                    .load(inputStream)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            Log.d("RichEditText", "Drawable loaded successfully for URI: $uri")

                            val editable = text as Editable
                            val start = selectionStart
                            val end = selectionEnd

                            // Replace selected text with a space for the image
                            editable.replace(start, end, " ")

                            // Insert ImageSpan for the loaded drawable
                            resource.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
                            val imageSpan = ImageSpan(resource, ImageSpan.ALIGN_BASELINE)
                            editable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                            // Update EditText content
                            text = editable
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            Log.d("RichEditText", "Drawable load cleared for URI: $uri")
                        }
                    })
            } else {
                Log.e("RichEditText", "Failed to open InputStream for URI: $uri")
            }
        } catch (e: Exception) {
            Log.e("RichEditText", "Error processing URI: $uri", e)
        }
    }
}

class RichInputConnectionWrapper(
    target: InputConnection?,
    mutable: Boolean,
    private val editText: RichEditText
) : InputConnectionWrapper(target, mutable) {

    override fun commitContent(
        inputContentInfo: android.view.inputmethod.InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        Log.d("RichInputConnection", "Received content info: $inputContentInfo")
        val mimeType = inputContentInfo.description.getMimeType(0) // Get the first MIME type
        Log.d("RichInputConnection", "Received MIME type: $mimeType")
        try {
            // Ensure the MIME type is supported
            if (mimeType in listOf("image/gif", "image/png", "image/jpeg", "image/webp")) {
                // Request temporary read permission
                inputContentInfo.requestPermission()

                // Insert the content into the RichEditText
                editText.insertRichContent(inputContentInfo.contentUri)

                // Release the permission after insertion
                inputContentInfo.releasePermission()
                return true
            } else {
                Log.w("RichInputConnection", "Unsupported MIME type: $mimeType")
            }
        } catch (e: SecurityException) {
            Log.e("RichInputConnection", "Permission error for URI: ${inputContentInfo.contentUri}", e)
        }
        return super.commitContent(inputContentInfo, flags, opts)
    }
}
