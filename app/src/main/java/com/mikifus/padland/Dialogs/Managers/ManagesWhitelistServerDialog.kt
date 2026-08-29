package com.mikifus.padland.Dialogs.Managers

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mikifus.padland.Database.ServerModel.ServerViewModel
import com.mikifus.padland.Dialogs.ConfirmDialog
import com.mikifus.padland.R

interface IManagesWhitelistServerDialog {
    var serverViewModel: ServerViewModel?
    fun showWhitelistServerDialog(activity: AppCompatActivity,
                                  url: String,
                                  onAddCallback: (dialogUrl: String) -> Unit,
                                  onNegativeCallback: (dialogUrl: String) -> Unit,
                                  onIgnoreCallback: (dialogUrl: String) -> Unit)
}
class ManagesWhitelistServerDialog: ManagesDialog(), IManagesWhitelistServerDialog {
    override val DIALOG_TAG: String = "DIALOG_WHITELIST_SERVER"

    override val dialog by lazy { ConfirmDialog() }
    override var serverViewModel: ServerViewModel? = null

    override fun showWhitelistServerDialog(activity: AppCompatActivity,
                                           url: String,
                                           onAddCallback: (dialogUrl: String) -> Unit,
                                           onNegativeCallback: (dialogUrl: String) -> Unit,
                                           onIgnoreCallback: (dialogUrl: String) -> Unit) {
        initViewModels(activity)
        initEvents(activity, url, onAddCallback, onNegativeCallback)

        dialog.setTitle(activity.getString(R.string.whitelist_server_dialog_title))
        dialog.setMessage(activity.getString(
            R.string.padview_toast_blacklist_url,
            ellipsizeUrl(url, 80)
        ))
        dialog.positiveButtonText = activity.getString(R.string.serverlist_dialog_new_server_title)
        dialog.negativeButtonText = activity.getString(R.string.whitelist_server_dialog_open_browser)
        dialog.neutralButtonText = activity.getString(android.R.string.cancel)

        dialog.show(activity.supportFragmentManager, DIALOG_TAG)
    }

    @Suppress("SameParameterValue")
    private fun ellipsizeUrl(url: String, maxLength: Int): String {
        return if (url.length <= maxLength) {
            url
        } else {
            url.take(
                maxLength - (maxLength / 2) - 2
            ) +
            Typography.ellipsis +
            url.takeLast(
                maxLength - (maxLength / 2) - 1
            )
        }
    }

    private fun initViewModels(activity: AppCompatActivity) {
        if(serverViewModel == null) {
            serverViewModel = ViewModelProvider(activity)[ServerViewModel::class.java]
        }
    }

    private fun initEvents(activity: AppCompatActivity,
                           url: String,
                           onAddCallback: (dialogUrl: String) -> Unit,
                           onNegativeCallback: (dialogUrl: String) -> Unit) {
        dialog.neutralButtonCallback = DialogInterface.OnClickListener { dialog, _ ->
            dialog.dismiss()
        }
        dialog.negativeButtonCallback = DialogInterface.OnClickListener { dialog, _ ->
            dialog.dismiss()
            onNegativeCallback(url)
        }
        dialog.positiveButtonCallback = DialogInterface.OnClickListener { _, _ ->
            onAddCallback(url)
        }
        dialog.isCancelable = false
    }
}
