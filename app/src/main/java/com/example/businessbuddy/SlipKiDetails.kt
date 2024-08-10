package com.example.businessbuddy

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.businessbuddy.databinding.ActivitySlipKiDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class SlipKiDetails : AppCompatActivity() {
    private val binding: ActivitySlipKiDetailsBinding by lazy {
        ActivitySlipKiDetailsBinding.inflate(layoutInflater)
    }

    private lateinit var slipNumber: String
    private lateinit var slipDate: String
    private lateinit var slipName: String
    private lateinit var slipDriver: String
    private lateinit var slipVehicleNo: String
    private lateinit var slipItem: String
    private lateinit var slipQuantity: String
    private lateinit var slipAmount: String
    private lateinit var auth: FirebaseAuth

    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val userEmail = auth.currentUser?.email.toString()
        if (userEmail == "contractorravinder@gmail.com") {
            binding.btnPrintSlip.visibility = View.VISIBLE
        } else {
            binding.btnPrintSlip.visibility = View.INVISIBLE
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        slipNumber = intent.getStringExtra("SlipNumber").toString()
        slipDate = intent.getStringExtra("SlipDate").toString()
        slipName = intent.getStringExtra("SlipName").toString()
        slipDriver = intent.getStringExtra("SlipDriver").toString()
        slipVehicleNo = intent.getStringExtra("SlipVehicleNo").toString()
        slipItem = intent.getStringExtra("SlipItem").toString()
        slipQuantity = intent.getStringExtra("SlipQuantity").toString()
        slipAmount = intent.getStringExtra("SlipAmount").toString()

        binding.apply {
            tvNumber.text = slipNumber
            tvDate.text = slipDate
            tvName.text = slipName
            tvDriver.text = slipDriver
            tvVehicleNo.text = slipVehicleNo
            tvItem.text = slipItem
            tvQuantity.text = slipQuantity
            tvAmount.text = slipAmount
        }

        binding.btnDownloadPDF.setOnClickListener {
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE)) {
                val bitmap = captureViewAsBitmap(binding.pdfDownload)
                saveBitmapAsPDF(bitmap, "Slip_$slipNumber")
            }
        }

        binding.btnPrintSlip.setOnClickListener {
            val bitmap = captureViewAsBitmap(binding.pdfDownload)
            printBitmapAsPDF(bitmap, "Slip_$slipNumber")
        }
    }

    private fun checkPermission(permission: String, requestCode: Int): Boolean {
        return if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun captureViewAsBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun saveBitmapAsPDF(bitmap: Bitmap, fileName: String) {
        val pdfDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MyApp")
        if (!pdfDir.exists()) {
            pdfDir.mkdir()
        }

        val pdfFile = File(pdfDir, "$fileName.pdf")

        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = document.startPage(pageInfo)

            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)

            document.writeTo(FileOutputStream(pdfFile))
            document.close()

            Toast.makeText(this, "PDF saved to ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun printBitmapAsPDF(bitmap: Bitmap, fileName: String) {
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            private lateinit var pdfDocument: PrintedPdfDocument

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                pdfDocument = PrintedPdfDocument(this@SlipKiDetails, newAttributes!!)

                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val layoutResult = android.print.PrintDocumentInfo.Builder(fileName)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(layoutResult, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas

                val scale = Math.min(
                    canvas.width.toFloat() / bitmap.width,
                    canvas.height.toFloat() / bitmap.height
                )
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )

                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)

                try {
                    pdfDocument.writeTo(FileOutputStream(destination?.fileDescriptor))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                    return
                } finally {
                    pdfDocument.close()
                }
                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            }
        }

        val jobName = "${getString(R.string.app_name)} Document"
        printManager.print(jobName, printAdapter, null)
    }
}
