package com.fil.app.ui.contribution

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fil.app.R

object ContributionLinks {
    const val DONATE_URL = "https://theophile.world/sponsor"
    const val FEEDBACK_EMAIL = "contact@theophile.world"
    const val PACKAGE_ID = "com.fil.app"
}

@Composable
fun ContributionPopup(
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.contribution_title)) },
        text = {
            Column {
                Text(stringResource(R.string.contribution_body))
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        openDonate(context)
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.contribution_action_donate))
                }
                OutlinedButton(
                    onClick = {
                        openReview(context)
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.contribution_action_review))
                }
                OutlinedButton(
                    onClick = {
                        openFeedback(context)
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.contribution_action_feedback))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.contribution_action_dismiss))
            }
        },
    )
}

private fun openDonate(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ContributionLinks.DONATE_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openReview(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${ContributionLinks.PACKAGE_ID}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${ContributionLinks.PACKAGE_ID}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(webIntent)
    }
}

private fun openFeedback(context: Context) {
    val subject = context.getString(R.string.contribution_feedback_subject)
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${ContributionLinks.FEEDBACK_EMAIL}")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No email client — silently no-op; popup already closed by caller.
    }
}
