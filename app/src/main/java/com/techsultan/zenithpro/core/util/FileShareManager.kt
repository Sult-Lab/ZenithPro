package com.techsultan.zenithpro.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class FileShareManager(
    private val context: Context
) {

    fun share(
        file: File,
        mimeType: String,
        title: String
    ) {

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

        val intent = Intent(
            Intent.ACTION_SEND
        ).apply {

            type = mimeType

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                title
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
        )
    }
}