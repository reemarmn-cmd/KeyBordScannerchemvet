package io.rami.barcodekeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class BarcodeInputService : InputMethodService(), ZXingScannerView.ResultHandler {

    private var scannerView: ZXingScannerView? = null
    private var button: Button? = null

    private var lastText = ""
    private var lastTime: Long = 0

    override fun onCreateInputView(): View {

        val v = layoutInflater.inflate(R.layout.input, null)

        scannerView = v.findViewById(R.id.zxing_scanner)

        button = v.findViewById(R.id.button)

        button!!.setOnClickListener {
            val i = Intent(this, PermissionCheckActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
        }

        enforcePermission()

        return v
    }

    private fun enforcePermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            val i = Intent(this, PermissionCheckActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)

        } else {

            button?.visibility = View.GONE
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {

        super.onStartInputView(info, restarting)

        scannerView?.setResultHandler(this)

        enforcePermission()

        try {
            scannerView?.startCamera(0)
        } catch (e: Exception) {
            scannerView?.startCamera()
        }

        scannerView?.setAutoFocus(true)
    }

    override fun onFinishInput() {

        super.onFinishInput()

        scannerView?.stopCamera()
    }

    override fun handleResult(rawResult: Result) {

        scannerView?.resumeCameraPreview(this)

        if (
            rawResult.text == lastText &&
            System.currentTimeMillis() - lastTime < 3000
        ) {
            return
        }

        lastText = rawResult.text
        lastTime = System.currentTimeMillis()

        ToneGenerator(
            AudioManager.STREAM_NOTIFICATION,
            100
        ).startTone(
            ToneGenerator.TONE_PROP_ACK,
            200
        )

        currentInputConnection.also { ic: InputConnection ->

            ic.commitText(rawResult.text, 1)

            ic.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER
                )
            )

            ic.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_ENTER
                )
            )
        }
    }
}
