package id.rockierocker.imagetools.service.preprocess.model;

import lombok.*;

import java.util.List;

/**
 * Konfigurasi untuk semua PreprocessCommand.
 *
 * Setiap field hanya dibaca oleh command yang relevan.
 * Field yang tidak diset (null) akan menggunakan nilai default di masing-masing command.
 *
 * Contoh JSON di database:
 * {
 *   "kColors": 16,
 *   "iterations": 10,
 *   "contrast": 1.2,
 *   "sharpenKernel": [[0,-1,0],[-1,5,-1],[0,-1,0]],
 *   "removeOutlineRadius": 2,
 *   "maxHeight": 1024,
 *   "quality": {
 *     "denoiseSigma": 1.0,
 *     "brightness": 0.0,
 *     "gamma": 1.0,
 *     "saturation": 1.0,
 *     "whiteBalanceEnabled": false,
 *     "histogramEqualizationEnabled": false,
 *     "unsharpMaskRadius": 1,
 *     "unsharpMaskAmount": 0.5,
 *     "normalizeExposureEnabled": false,
 *     "medianFilterEnabled": false
 *   }
 * }
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Config {

    // ── KMeansQuantizationCommand ──────────────────────────────────────────
    /** Jumlah warna (k) untuk K-Means quantization */
    private Integer kColors;

    /** Maksimum iterasi K-Means */
    private Integer iterations;

    // ── AdjustContrastCommand ──────────────────────────────────────────────
    /** Faktor kontras: >1.0 = tingkatkan, <1.0 = turunkan, 1.0 = tidak berubah */
    private Float contrast;

    // ── SharpenCommand ─────────────────────────────────���───────────────────
    /** 3x3 convolution kernel untuk sharpening */
    private List<List<Float>> sharpenKernel;

    // ── ResizeCommand ───────────────────────────────────────────���──────────
    /** Tinggi maksimum setelah resize (aspek rasio tetap) */
    private Integer maxHeightOrWidth;

    // ── RemoveOutlineCommand ───────────────────────────────────────────────
    /** Radius erosi untuk menghapus outline/kontur transparan */
    private Integer removeOutlineRadius;

    // ── AdjustQualityCommand ───────────────────────────────────────────────
    /** Konfigurasi lengkap untuk AdjustQualityCommand */
    private QualityConfig quality;

    // ── CartoonizeCommand (OpenCV) ─────────────────────────────────────────
    /**
     * Ukuran kernel median blur untuk smoothing grayscale (harus ganjil, default: 7).
     * Semakin besar = lebih halus tapi detail hilang.
     */
    private Integer cartoonizeBlurSize;

    /**
     * Block size adaptive threshold untuk deteksi edge (harus ganjil & >1, default: 9).
     */
    private Integer cartoonizeEdgeBlockSize;

    /**
     * Konstanta C adaptive threshold (default: 2). Semakin besar = edge lebih sedikit.
     */
    private Integer cartoonizeEdgeC;

    /**
     * Jumlah iterasi bilateral filter untuk smoothing warna (default: 7).
     */
    private Integer cartoonizeBilateralIterations;

    /**
     * Diameter neighborhood bilateral filter (default: 9).
     */
    private Integer cartoonizeBilateralD;

    /**
     * Sigma color bilateral filter (default: 75). Semakin besar = warna lebih flat.
     */
    private Double cartoonizeSigmaColor;

    /**
     * Sigma space bilateral filter (default: 75).
     */
    private Double cartoonizeSigmaSpace;

    // ══════════════════════════════════════════════════════════════════════
    // Inner class: QualityConfig
    // ══════════════════════════════════════════════════════════════════════

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class QualityConfig {

        // ── Denoising ──────────────────────────────────────────────────
        /**
         * Gaussian blur sigma untuk denoising.
         * 0.0 = tidak ada denoising, 0.5-1.5 = ringan, >2.0 = agresif.
         * Rekomendasi sebelum AI: 0.5–1.0
         */
        private Double denoiseSigma;

        /**
         * Median filter untuk menghapus salt-and-pepper noise.
         * true = aktifkan (kernel 3x3).
         * Rekomendasi sebelum remove-bg: true jika ada noise JPEG.
         */
        private Boolean medianFilterEnabled;

        // ── Exposure & Brightness ──────────────────────────────────────
        /**
         * Brightness adjustment: rentang -128 hingga +128.
         * 0 = tidak berubah. Positif = lebih terang, negatif = lebih gelap.
         * Rekomendasi sebelum upscaling: 0 (biarkan AI menangani)
         */
        private Float brightness;

        /**
         * Gamma correction: >1.0 = gelap, <1.0 = terang, 1.0 = tidak berubah.
         * Rumus: output = (input/255)^gamma * 255
         * Rekomendasi sebelum vectorization: 0.8–0.9 untuk terangkan
         */
        private Double gamma;

        /**
         * Normalisasi exposure otomatis menggunakan histogram stretching.
         * Memperluas rentang pixel agar menggunakan penuh 0-255.
         * true = aktifkan. Rekomendasi sebelum remove-bg: true jika gambar kusam.
         */
        private Boolean normalizeExposureEnabled;

        // ── Color & Saturation ─────────────────────────────────────────
        /**
         * Saturation multiplier: 1.0 = tidak berubah, 0 = grayscale, >1 = lebih vibrant.
         * Rekomendasi sebelum vectorization: 1.2–1.5 untuk warna lebih distinct.
         * Rekomendasi sebelum remove-bg: 1.0 (biarkan natural).
         */
        private Float saturation;

        /**
         * White balance otomatis menggunakan gray world assumption.
         * Menyamakan rata-rata R, G, B channel.
         * true = aktifkan. Rekomendasi jika gambar ada color cast.
         */
        private Boolean whiteBalanceEnabled;

        // ── Contrast Enhancement ───────────────────────────────────────
        /**
         * Histogram equalization untuk meningkatkan kontras secara otomatis.
         * Bekerja di luminance channel (tidak ubah hue/saturation).
         * true = aktifkan. Rekomendasi sebelum vectorization: true.
         */
        private Boolean histogramEqualizationEnabled;

        /**
         * CLAHE (Contrast Limited Adaptive Histogram Equalization).
         * Lebih baik dari histogram equalization standar untuk gambar dengan variasi pencahayaan.
         * true = aktifkan. Rekomendasi sebelum remove-bg: true.
         */
        private Boolean claheEnabled;

        /**
         * Clip limit untuk CLAHE (default: 2.0). Semakin besar = lebih agresif.
         */
        private Double claheClipLimit;

        /**
         * Tile size untuk CLAHE (default: 8). Menentukan ukuran region lokal.
         */
        private Integer claheTileSize;

        // ── Sharpening ─────────────────────────────────────────────────
        /**
         * Unsharp mask radius (kernel size untuk blur base, default: 1).
         * Digunakan bersama unsharpMaskAmount.
         * Rekomendasi sebelum vectorization: 1–2.
         */
        private Integer unsharpMaskRadius;

        /**
         * Unsharp mask amount/strength: 0.0 = tidak ada efek, 1.0 = full.
         * Rekomendasi: 0.3–0.7 (agresif bisa munculkan noise).
         * Tidak direkomendasikan sebelum AI upscaling (AI akan handle sendiri).
         */
        private Float unsharpMaskAmount;

        // ── Alpha/Transparency ─────────────────────────────────────────
        /**
         * Expand background (fill transparent dengan warna) sebelum proses.
         * null = tidak ada, "WHITE", "BLACK", "GRAY"
         * Rekomendasi sebelum upscaling jika model tidak support RGBA.
         */
        private String fillTransparentWith;

        // ── Preset ─────────────────────────────────────────────────────
        /**
         * Preset konfigurasi cepat. Jika diset, override semua field di atas.
         * Pilihan: "REMOVE_BG", "UPSCALE", "VECTORIZE", "BALANCED"
         */
        private String preset;
    }
}
