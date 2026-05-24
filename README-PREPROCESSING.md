# 🖼️ Image Preprocessing Pipeline

Dokumentasi lengkap untuk sistem preprocessing gambar berbasis **Command Pattern** di `image-tools-api`.

---

## 📁 Struktur Package

```
service/preprocess/
├── command/                    # Concrete commands (implementasi step)
│   ├── PreprocessCommand.java      ← Interface utama
│   ├── AdjustContrastCommand.java  ← Step: ADJUST_CONTRAST
│   ├── AdjustQualityCommand.java   ← Step: ADJUST_QUALITY
│   ├── KMeansQuantizationCommand.java ← Step: K_MEANS_QUANTIZATION
│   ├── RemoveOutlineCommand.java   ← Step: REMOVE_OUTLINE
│   ├── ResizeCommand.java          ← Step: RESIZE
│   └── SharpenCommand.java         ← Step: SHARPEN
├── model/
│   └── PreprocessConfig.java       ← Model konfigurasi (+ QualityConfig inner class)
├── pipeline/
│   └── PreprocessPipeline.java     ← Invoker: menjalankan step berurutan
├── registry/
│   └── PreprocessCommandRegistry.java ← Auto-discover semua command via Spring DI
└── util/
    └── PreprocessUtil.java         ← Utility: clamp, dll.
```

---

## 🏗️ Arsitektur: Command Pattern

```
PreprocessService
      │
      ▼
PreprocessPipeline (Invoker)
      │
      ├─ lookup step name → PreprocessCommandRegistry
      │                            │
      │                            └─ Map<String, PreprocessCommand>
      │                                 (auto-discovered via Spring @Component)
      ▼
PreprocessCommand.validate(config)
      ▼
PreprocessCommand.execute(image, config) → BufferedImage
      ▼
   (next step...)
```

### Cara Menambahkan Command Baru

1. Buat class baru yang `implements PreprocessCommand`
2. Tambahkan `@Component`
3. Implementasikan `getStepName()` → nama unik (huruf besar, e.g. `"MY_STEP"`)
4. Implementasikan `execute(BufferedImage, PreprocessConfig)`
5. (Opsional) Override `validate(PreprocessConfig)` untuk validasi config

Tidak perlu mendaftarkan ke mana pun — registry akan otomatis menemukannya.

---

## ⚙️ Daftar Command & Step Name

| Step Name | Class | Keterangan |
|---|---|---|
| `ADJUST_CONTRAST` | `AdjustContrastCommand` | Penyesuaian kontras linear |
| `ADJUST_QUALITY` | `AdjustQualityCommand` | Suite lengkap kualitas gambar (lihat detail di bawah) |
| `K_MEANS_QUANTIZATION` | `KMeansQuantizationCommand` | Reduksi warna dengan K-Means clustering |
| `REMOVE_OUTLINE` | `RemoveOutlineCommand` | Hapus outline/kontur transparan via erosi |
| `RESIZE` | `ResizeCommand` | Resize dengan batas maxHeight (aspek rasio tetap) |
| `SHARPEN` | `SharpenCommand` | Sharpening dengan 3×3 convolution kernel |

---

## 🗄️ Database Entity: `preprocess_config`

```sql
CREATE TABLE preprocess_config (
    id               BIGSERIAL PRIMARY KEY,
    config_code      VARCHAR(50),
    process          VARCHAR(20),     -- e.g. 'REMBG', 'UPSCALE', 'VECTORIZE'
    steps            JSONB,           -- urutan step, e.g. ["ADJUST_QUALITY","RESIZE"]
    k_colors         INTEGER,
    contrast         FLOAT,
    iterations       INTEGER,
    sharpen_kernel   JSONB,
    resize_max_height INTEGER,
    -- base entity fields: created_at, updated_at, deleted
);
```

> **Catatan:** Field `quality` (untuk `AdjustQualityCommand`) disimpan sebagai JSONB terpisah
> atau dimasukkan ke dalam field `steps` sebagai parameter inline, tergantung implementasi service.

---

## 📦 Model: `PreprocessConfig`

```java
PreprocessConfig config = PreprocessConfig.builder()
    .kColors(16)
    .iterations(10)
    .contrast(1.2f)
    .sharpenKernel(List.of(
        List.of(0f,-1f,0f),
        List.of(-1f,5f,-1f),
        List.of(0f,-1f,0f)
    ))
    .maxHeight(1024)
    .removeOutlineRadius(2)
    .quality(PreprocessConfig.QualityConfig.builder()
        .preset("REMOVE_BG")
        .build())
    .build();
```

---

## 🎨 AdjustQualityCommand — Detail Lengkap

Step name: **`ADJUST_QUALITY`**

Command ini menjalankan serangkaian operasi kualitas secara berurutan dalam satu langkah.

### Urutan Eksekusi Internal

```
1. Fill Transparent Background
2. Median Filter (salt-and-pepper noise)
3. Gaussian Denoise (blur lembut)
4. Normalize Exposure (histogram stretching)
5. White Balance (gray world)
6. Gamma Correction
7. Brightness Adjustment
8. Saturation
9. Histogram Equalization
10. CLAHE
11. Unsharp Mask
```

### Semua Parameter `QualityConfig`

#### 🔇 Denoising

| Field | Tipe | Default | Keterangan |
|---|---|---|---|
| `denoiseSigma` | `Double` | `null` (off) | Gaussian blur sigma. `0.5`–`1.0` = ringan, `>2.0` = agresif. `0` = nonaktif |
| `medianFilterEnabled` | `Boolean` | `null` (off) | Median filter 3×3 untuk salt-and-pepper noise |

#### ☀️ Exposure & Brightness

| Field | Tipe | Default | Keterangan |
|---|---|---|---|
| `brightness` | `Float` | `null` (off) | Rentang `-128` s/d `+128`. `0` = tidak berubah |
| `gamma` | `Double` | `null` (off) | `<1.0` = terang, `>1.0` = gelap. Formula: `(v/255)^γ × 255` |
| `normalizeExposureEnabled` | `Boolean` | `null` (off) | Histogram stretching per-channel (R/G/B) |

#### 🎨 Color & Saturation

| Field | Tipe | Default | Keterangan |
|---|---|---|---|
| `saturation` | `Float` | `null` (off) | `1.0` = tidak berubah, `0` = grayscale, `>1` = lebih vibrant |
| `whiteBalanceEnabled` | `Boolean` | `null` (off) | Gray World Assumption — samakan rata-rata R, G, B |

#### 🔆 Contrast Enhancement

| Field | Tipe | Default | Keterangan |
|---|---|---|---|
| `histogramEqualizationEnabled` | `Boolean` | `null` (off) | Equalization global pada luminance channel |
| `claheEnabled` | `Boolean` | `null` (off) | Adaptive histogram equalization per tile |
| `claheClipLimit` | `Double` | `2.0` | Batas clip CLAHE. Lebih besar = lebih agresif |
| `claheTileSize` | `Integer` | `8` | Ukuran region tile (pixel). Lebih kecil = lebih lokal |

#### ✨ Sharpening

| Field | Tipe | Default | Keterangan |
|---|---|---|---|
| `unsharpMaskRadius` | `Integer` | `1` | Radius blur untuk base unsharp mask |
| `unsharpMaskAmount` | `Float` | `null` (off) | Strength: `0.3`–`0.7` disarankan. `0` = nonaktif |

#### 🏳️ Transparency

| Field | Tipe | Default | Keterangan |
|---|---|---|---|
| `fillTransparentWith` | `String` | `null` (off) | Isi area transparan: `"WHITE"`, `"BLACK"`, `"GRAY"` |

#### 🎯 Preset

| Field | Tipe | Keterangan |
|---|---|---|
| `preset` | `String` | Jika diset, **override semua field di atas**. Pilihan: `REMOVE_BG`, `UPSCALE`, `VECTORIZE`, `BALANCED` |

---

## 🎯 Preset Configurations

Preset adalah shortcut konfigurasi cepat. Saat `preset` diset, semua field lain diabaikan.

### `REMOVE_BG` — Optimasi untuk AI Background Removal

> Tujuan: Gambar bersih, kontras baik, warna natural agar model AI dapat memisahkan foreground dengan akurat.

| Operasi | Nilai |
|---|---|
| Normalize Exposure | ✅ aktif |
| White Balance | ✅ aktif |
| Median Filter | ✅ aktif |
| CLAHE | ✅ aktif (clipLimit=2.0, tileSize=8) |
| Fill Transparent | ❌ (biarkan transparent jika ada) |

**Kapan pakai:** Gambar dari berbagai sumber dengan pencahayaan tidak konsisten, ada noise JPEG, atau ada color cast.

---

### `UPSCALE` — Optimasi untuk AI Upscaling

> Tujuan: Bersihkan noise tanpa merusak detail, agar model upscaling tidak memperbesar artefak.

| Operasi | Nilai |
|---|---|
| Gaussian Denoise | ✅ sigma=0.5 (ringan) |
| Gamma | ✅ 0.9 (sedikit terangkan) |
| Brightness | 0 (tidak berubah) |

**Kapan pakai:** Gambar resolusi kecil yang akan diupscale. Hindari sharpening — biarkan AI yang handle.

---

### `VECTORIZE` — Optimasi untuk Image-to-Vector

> Tujuan: Tingkatkan warna dan ketajaman agar batas/kontur gambar lebih jelas untuk proses vektorisasi.

| Operasi | Nilai |
|---|---|
| Saturation | ✅ 1.3 (warna lebih distinct) |
| Histogram Equalization | ✅ aktif |
| Unsharp Mask | ✅ radius=1, amount=0.5 |

**Kapan pakai:** Logo, ilustrasi, gambar flat dengan warna block yang akan dikonversi ke SVG/vector.

---

### `BALANCED` — Penggunaan Umum

> Tujuan: Perbaikan ringan menyeluruh tanpa efek ekstrem. Cocok saat tujuan akhir belum pasti.

| Operasi | Nilai |
|---|---|
| Gaussian Denoise | ✅ sigma=0.5 (ringan) |
| Normalize Exposure | ✅ aktif |
| Saturation | ✅ 1.1 (sedikit lebih vibrant) |
| Unsharp Mask | ✅ radius=1, amount=0.3 (ringan) |

**Kapan pakai:**
- Pipeline generik yang melayani berbagai jenis gambar
- Testing/development sebelum fine-tune ke preset spesifik
- Gambar yang tujuan akhirnya belum diketahui

---

## 📋 Contoh Konfigurasi JSON (di database `steps`)

### Minimal — hanya preset
```json
{
  "steps": ["ADJUST_QUALITY"],
  "quality": {
    "preset": "REMOVE_BG"
  }
}
```

### Full pipeline — remove background
```json
{
  "steps": ["ADJUST_QUALITY", "RESIZE"],
  "resizeMaxHeight": 1024,
  "quality": {
    "normalizeExposureEnabled": true,
    "whiteBalanceEnabled": true,
    "medianFilterEnabled": true,
    "claheEnabled": true,
    "claheClipLimit": 2.0,
    "claheTileSize": 8,
    "fillTransparentWith": "WHITE"
  }
}
```

### Full pipeline — vectorize
```json
{
  "steps": ["ADJUST_QUALITY", "K_MEANS_QUANTIZATION", "ADJUST_CONTRAST"],
  "kColors": 8,
  "iterations": 10,
  "contrast": 1.3,
  "quality": {
    "preset": "VECTORIZE"
  }
}
```

### Custom — tanpa preset
```json
{
  "steps": ["ADJUST_QUALITY", "SHARPEN"],
  "sharpenKernel": [[0,-1,0],[-1,5,-1],[0,-1,0]],
  "quality": {
    "denoiseSigma": 0.8,
    "brightness": 10.0,
    "gamma": 0.95,
    "saturation": 1.2,
    "whiteBalanceEnabled": true,
    "claheEnabled": true,
    "claheClipLimit": 3.0,
    "claheTileSize": 8,
    "unsharpMaskRadius": 1,
    "unsharpMaskAmount": 0.4,
    "fillTransparentWith": "WHITE"
  }
}
```

---

## 🔢 Panduan Nilai Parameter

### Denoising — Kapan Pakai Yang Mana?

| Situasi | Rekomendasi |
|---|---|
| Gambar foto natural, sedikit noise | `denoiseSigma: 0.5` |
| Gambar JPEG heavy compression | `medianFilterEnabled: true` + `denoiseSigma: 1.0` |
| Gambar scan dokumen | `medianFilterEnabled: true` |
| Gambar sudah bersih | Jangan aktifkan (bisa blur detail) |

### CLAHE vs Histogram Equalization

| | Histogram Equalization | CLAHE |
|---|---|---|
| Cara kerja | Global — satu LUT untuk seluruh gambar | Lokal — LUT berbeda per tile |
| Cocok untuk | Gambar dengan pencahayaan seragam | Gambar dengan variasi pencahayaan (backlit, shadow) |
| Risiko | Over-amplify noise di area gelap | Lebih aman karena ada clip limit |
| Rekomendasi | Vectorize | Remove BG, foto natural |

### Saturation Guide

| Nilai | Efek | Use Case |
|---|---|---|
| `0.0` | Grayscale | - |
| `0.8` | Warna lebih pucat | - |
| `1.0` | Tidak berubah | - |
| `1.1` | Sedikit lebih vibrant | Balanced |
| `1.3` | Warna lebih distinct | Vectorize |
| `>1.5` | Over-saturated | Hindari |

---

## ⚠️ Catatan Penting

1. **Urutan step di `steps` array menentukan urutan eksekusi** — urutan berpengaruh pada hasil akhir.
   - ✅ `["ADJUST_QUALITY", "K_MEANS_QUANTIZATION"]` — quality dulu baru quantize
   - ❌ `["K_MEANS_QUANTIZATION", "ADJUST_QUALITY"]` — quantize dulu bisa hilangkan detail

2. **Preset override semua field** — jika `preset` diset di `QualityConfig`, semua field lain diabaikan.

3. **Operasi tidak dijalankan jika field `null`** — tidak perlu mengeset `false`, cukup tidak masukkan field-nya ke JSON.

4. **Alpha channel dipertahankan** — semua operasi mempertahankan transparency kecuali `fillTransparentWith` diset.

5. **CLAHE dan Histogram Equalization tidak boleh diaktifkan bersamaan** — pilih salah satu. CLAHE direkomendasikan karena lebih aman.

6. **Unsharp Mask tidak disarankan sebelum AI upscaling** — model upscaling (ESRGAN, dll.) lebih baik bekerja pada gambar sedikit soft.

