package com.example.hangman

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    // ==== Latihan 6: kunci untuk simpan state ====
    companion object {
        private const val KEY_KATA = "KEY_KATA"
        private const val KEY_BENAR = "KEY_BENAR"
        private const val KEY_SALAH = "KEY_SALAH"
        private const val KEY_SKOR = "KEY_SKOR"
        private const val KEY_NYAWA = "KEY_NYAWA"
        private const val KEY_OVERLAY_MULAI_TAMPIL = "KEY_OVERLAY_MULAI"
        private const val KEY_OVERLAY_KESULITAN_TAMPIL = "KEY_OVERLAY_KESULITAN"
    }

    // ==== Latihan 1: tombol huruf A-Z ====
    private lateinit var gridHuruf: GridLayout
    private val tombolHuruf = mutableMapOf<Char, Button>()

    // ==== data permainan ====
    private lateinit var kataRahasia: String
    private val hurufBenar = mutableSetOf<Char>()
    private val hurufSalah = mutableSetOf<Char>()
    private var maksNyawa = 6

    // ==== Peta Petunjuk Kata ====  <-- [DI SINI TEMPATNYA]
    private val petunjukKata = mapOf(
        "ANDROID" to "Sistem operasi mobile paling populer di dunia",
        "KOTLIN" to "Bahasa pemrograman resmi untuk Android",
        "LAYOUT" to "Bagian yang mengatur tata letak tampilan",
        "AKTIVITAS" to "Satu layar dalam aplikasi Android",
        "INTENT" to "Alat berpindah antar layar/aplikasi",
        "WIDGET" to "Komponen UI seperti tombol dan teks"
    )

    // ==== Latihan 4: skor ====
    private var skor = 0

    // ==== Latihan 5: kesulitan ====
    private lateinit var overlayKesulitan: LinearLayout
    private var maksNyawaDipilih = 6   // default sedang

    // ==== view ====
    private lateinit var tvNyawa: TextView
    private lateinit var tvSkor: TextView
    private lateinit var tvKata: TextView
    private lateinit var tvRiwayat: TextView
    private lateinit var etTebak: EditText
    private lateinit var overlayHasil: LinearLayout
    private lateinit var overlayMulai: LinearLayout
    private lateinit var tvHasil: TextView
    private lateinit var tvKataAkhir: TextView


    // ==== gambar gantungan: 6 bagian tubuh, urut sesuai kesalahan ====
    private lateinit var bagianTubuh: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi View
        tvNyawa = findViewById(R.id.tvNyawa)
        tvSkor = findViewById(R.id.tvSkor)
        tvKata = findViewById(R.id.tvKata)
        tvRiwayat = findViewById(R.id.tvRiwayat)
        etTebak = findViewById(R.id.etTebak)
        overlayHasil = findViewById(R.id.overlayHasil)
        overlayMulai = findViewById(R.id.overlayMulai)
        overlayKesulitan = findViewById(R.id.overlayKesulitan)
        tvHasil = findViewById(R.id.tvHasil)
        tvKataAkhir = findViewById(R.id.tvKataAkhir)

        bagianTubuh = listOf(
            findViewById(R.id.imgKepala),
            findViewById(R.id.imgBadan),
            findViewById(R.id.imgTanganKiri),
            findViewById(R.id.imgTanganKanan),
            findViewById(R.id.imgKakiKiri),
            findViewById(R.id.imgKakiKanan)
        )

        gridHuruf = findViewById(R.id.gridHuruf)
        buatTombolHuruf()

        // 2. Set Listener Tombol
        findViewById<Button>(R.id.btnTebak).setOnClickListener { prosesTebakan() }
        findViewById<Button>(R.id.btnMainLagi).setOnClickListener { mulaiPermainan() }
        findViewById<Button>(R.id.btnMulai).setOnClickListener {
            overlayMulai.visibility = View.GONE
            mulaiPermainan()
        }

        findViewById<Button>(R.id.btnMudah).setOnClickListener { pilihKesulitan(8) }
        findViewById<Button>(R.id.btnSedang).setOnClickListener { pilihKesulitan(6) }
        findViewById<Button>(R.id.btnSulit).setOnClickListener { pilihKesulitan(4) }

        // ==== LETAKKAN KODE GAMBAR DI SINI (GANTI BAGIAN AKHIR) ====[cite: 9]
        if (savedInstanceState != null) {
            // Pulihkan permainan sebelumnya setelah rotate[cite: 9]
            kataRahasia = savedInstanceState.getString(KEY_KATA) ?: run {
                val daftarKata = resources.getStringArray(R.array.daftar_kata)
                daftarKata.random()
            }

            hurufBenar.clear()
            hurufBenar.addAll(savedInstanceState.getCharArray(KEY_BENAR)?.toList() ?: emptyList())
            hurufSalah.clear()
            hurufSalah.addAll(savedInstanceState.getCharArray(KEY_SALAH)?.toList() ?: emptyList())
            skor = savedInstanceState.getInt(KEY_SKOR, 0)
            maksNyawa = savedInstanceState.getInt(KEY_NYAWA, 6)

            tvSkor.text = "Skor: $skor"
            buatTombolHuruf()

            // Nonaktifkan tombol huruf yang sudah pernah ditebak[cite: 9]
            (hurufBenar + hurufSalah).forEach { tombolHuruf[it]?.isEnabled = false }
            gambarPapan()

            overlayKesulitan.visibility =
                if (savedInstanceState.getBoolean(KEY_OVERLAY_KESULITAN_TAMPIL)) View.VISIBLE else View.GONE
            overlayMulai.visibility =
                if (savedInstanceState.getBoolean(KEY_OVERLAY_MULAI_TAMPIL)) View.VISIBLE else View.GONE
        } else {
            // Aplikasi baru pertama kali dibuka[cite: 9]
            mulaiPermainan()
            overlayKesulitan.visibility = View.VISIBLE
        }
    }

    /** Latihan 5: dipanggil saat salah satu tombol kesulitan dipilih */
    private fun pilihKesulitan(nyawa: Int) {
        maksNyawaDipilih = nyawa
        maksNyawa = nyawa
        overlayKesulitan.visibility = View.GONE
        overlayMulai.visibility = View.VISIBLE

        // Panggil mulaiPermainan di sini agar kata baru & petunjuk baru diacak saat masuk ke overlayMulai
        mulaiPermainan()
    }

    /** Latihan 1: membuat 26 tombol huruf A-Z secara dinamis dan menyusunnya di GridLayout */
    private fun buatTombolHuruf() {
        gridHuruf.removeAllViews()
        tombolHuruf.clear()

        for (kode in 'A'..'Z') {
            val tombol = Button(this).apply {
                text = kode.toString()
                textSize = 14f
                setPadding(0, 8, 0, 8)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_tombol_huruf)
                setTextColor(0xFFE2E8F0.toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener { tebakDariTombol(kode, this) }
            }
            gridHuruf.addView(tombol)
            tombolHuruf[kode] = tombol
        }
    }

    /** Dipanggil saat salah satu tombol huruf A-Z ditekan */
    private fun tebakDariTombol(huruf: Char, tombol: Button) {
        etTebak.setText(huruf.toString())
        prosesTebakan()
        // huruf yang sudah ditebak dinonaktifkan agar tidak bisa ditekan lagi
        if (hurufBenar.contains(huruf) || hurufSalah.contains(huruf)) {
            tombol.isEnabled = false
        }
    }

    /**
     * Menampilkan gambar bagian tubuh sebanyak [kesalahan].
     * List bagianTubuh urut dari kepala -> badan -> tangan -> kaki,
     * jadi cukup membandingkan indeksnya dengan jumlah kesalahan.
     */
    private fun perbaruiGantungan(kesalahan: Int) {
        bagianTubuh.forEachIndexed { urutan, gambar ->
            val seharusnyaTampil = urutan < kesalahan
            val sedangTampil = gambar.visibility == View.VISIBLE

            if (seharusnyaTampil && !sedangTampil) {
                // Latihan 2: baru muncul -> animasikan alpha dari 0 ke 1
                gambar.alpha = 0f
                gambar.visibility = View.VISIBLE
                gambar.animate().alpha(1f).setDuration(300).start()
            } else if (!seharusnyaTampil) {
                gambar.visibility = View.INVISIBLE
                gambar.alpha = 1f   // reset alpha untuk kemunculan berikutnya
            }
        }
    }

    private fun mulaiPermainan() {
        val daftarKata = resources.getStringArray(R.array.daftar_kata)
        kataRahasia = daftarKata.random()

        hurufBenar.clear()
        hurufSalah.clear()

        overlayHasil.visibility = View.GONE
        etTebak.text.clear()
        etTebak.isEnabled = true

        buatTombolHuruf()
        gambarPapan()


    }

    private fun gambarPapan() {
        perbaruiGantungan(hurufSalah.size)

        tvKata.text = kataRahasia
            .map { if (hurufBenar.contains(it)) it else '_' }
            .joinToString(" ")

        // Perbarui petunjuk di papan permainan utama
        findViewById<TextView>(R.id.tvPetunjukPapan)?.text =
            "💡 Petunjuk: ${petunjukKata[kataRahasia] ?: "-"}"

        val sisaNyawa = maksNyawa - hurufSalah.size
        tvNyawa.text = "Nyawa: $sisaNyawa"

        tvRiwayat.text = if (hurufSalah.isEmpty()) {
            "Salah: -"
        } else {
            "Salah: ${hurufSalah.joinToString(" ")}"
        }
    }

    private fun prosesTebakan() {
        val input = etTebak.text.toString().trim().uppercase()

        // 1) harus tepat satu huruf
        if (input.length != 1 || !input[0].isLetter()) {
            Toast.makeText(this, "Masukkan satu huruf", Toast.LENGTH_SHORT).show()
            etTebak.text.clear()
            return
        }

        val huruf = input[0]

        // 2) huruf yang sama tidak dihitung dua kali
        if (hurufBenar.contains(huruf) || hurufSalah.contains(huruf)) {
            Toast.makeText(this, "Huruf $huruf sudah ditebak", Toast.LENGTH_SHORT).show()
            etTebak.text.clear()
            return
        }

        // 3) benar -> buka hurufnya; salah -> tambah kesalahan
        if (kataRahasia.contains(huruf)) {
            hurufBenar.add(huruf)
        } else {
            hurufSalah.add(huruf)
        }

        // 4) bersihkan input dan gambar ulang papan
        etTebak.text.clear()
        gambarPapan()

        // 5) periksa apakah permainan sudah berakhir
        periksaAkhirPermainan()
    }

    private fun periksaAkhirPermainan() {
        val semuaTerbuka = kataRahasia.all { hurufBenar.contains(it) }

        when {
            semuaTerbuka -> tampilkanHasil(menang = true)
            hurufSalah.size >= maksNyawa -> tampilkanHasil(menang = false)
        }
    }

    private fun tampilkanHasil(menang: Boolean) {
        tvHasil.text = if (menang) getString(R.string.menang) else getString(R.string.kalah)
        tvHasil.setTextColor(
            if (menang) 0xFF3DDC84.toInt() else 0xFFF87171.toInt()
        )
        tvKataAkhir.text = "Kata: $kataRahasia"

        // Latihan 4: tambah skor hanya saat menang
        if (menang) {
            skor += 10
            tvSkor.text = "Skor: $skor"
        }

        etTebak.isEnabled = false

        overlayHasil.alpha = 0f
        overlayHasil.visibility = View.VISIBLE
        overlayHasil.animate()
            .alpha(1f)
            .setDuration(350)
            .start()

        overlayHasil.requestFocus()
    }

    // ==== Latihan 6: Simpan keadaan (state) apabila skrin dipusingkan ====
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_KATA, kataRahasia)
        outState.putCharArray(KEY_BENAR, hurufBenar.toCharArray())
        outState.putCharArray(KEY_SALAH, hurufSalah.toCharArray())
        outState.putInt(KEY_SKOR, skor)
        outState.putInt(KEY_NYAWA, maksNyawa)
        outState.putBoolean(KEY_OVERLAY_MULAI_TAMPIL, overlayMulai.visibility == View.VISIBLE)
        outState.putBoolean(KEY_OVERLAY_KESULITAN_TAMPIL, overlayKesulitan.visibility == View.VISIBLE)
    }
}
