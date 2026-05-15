package id.rockierocker.imagetools.constant;

public enum ResponseCode {
    SUCCESS(
            "RC000",
            "Sukses",
            "Success",
            "Sukses",
            "Success"
    ),
    EXTENSION_NOT_SUPPORTED(
            "RC001",
            "Extensi file tidak didukung.",
            "File extension is not supported.",
            "Error Ekstensi Tidak Didukung",
            "Ekstensi file tidak didukung."
    ),
    UKNOWN_ERROR(
            "RC999",
            "Uknown error occurred.",
            "Uknown error occurred.",
            "Error",
            "Error"
    ),
    FAILED_READ_FILE(
            "RC002",
            "Failed to read file.",
            "Failed to read file.",
            "Error",
            "Error"
    ),
    PBM_CONVERSION_FAILED(
            "RC003",
            "Failed convert to PBM.",
            "Failed convert to PBM.",
            "Error",
            "Error"
    ),
    SVG_CONVERSION_FAILED(
            "RC004",
            "Failed convert to SVG.",
            "Failed convert to SVG.",
            "Error",
            "Error"
    ),
    FAILED_CREATE_TEMP_FILE(
            "RC005",
            "Failed to create temporary file.",
            "Failed to create temporary file.",
            "Error",
            "Error"
    ),
    VECTORIZE_FAILED(
            "RC006",
            "Vectorize failed.",
            "Vectorize failed.",
            "Error",
            "Error"
    ),
    VTRACE_CONFIG_NOT_FOUND(
            "RC007",
            "VTrace Config not found.",
            "VTrace Config not found.",
            "Error",
            "Error"
    ),
    PREPROCESS_FAIELD(
            "RC008",
            "Preprocess Failed.",
            "Preprocess Failed.",
            "Error",
            "Error"
    ),
    PREPROCESS_FAIELD_TO_BUFFERED_IMAGE(
            "RC009",
            "Preprocess Failed to Buffered Image.",
            "Preprocess Failed to Buffered Image.",
            "Error",
            "Error"
    ),
    DATA_NOT_FOUND(
            "RC010",
            "Data tidak ditemukan.",
            "Data not found.",
            "Error",
            "Data tidak ditemukan."
    ),
    DATA_ALREADY_EXISTS(
            "RC011",
            "Data sudah ada.",
            "Data already exists.",
            "Error",
            "Data sudah ada."
    ),
    FAILED_SAVE_DATA(
            "RC012",
            "Gagal menyimpan data.",
            "Failed to save data.",
            "Error",
            "Gagal menyimpan data."
    ),
    FAILED_UPDATE_DATA(
            "RC013",
            "Gagal mengupdate data.",
            "Failed to update data.",
            "Error",
            "Gagal mengupdate data."
    ),
    FAILED_DELETE_DATA(
            "RC014",
            "Gagal menghapus data.",
            "Failed to delete data.",
            "Error",
            "Gagal menghapus data."
    ),
    FAILED_TO_REMOVE_BACKGROUND(
            "RC015",
            "Failed to remove background.",
            "Failed to remove background.",
            "Error",
            "Error"
    ),
    REMOVE_BACKGROUND_CONFIG_NOT_FOUND(
            "RC016",
            "Remove Background Config not found.",
            "Remove Background Config not found.",
            "Error",
            "Error"
    ),
    INTERNAL_SERVER_ERROR(
            "RC017",
            "Internal server error.",
            "Internal server error.",
            "Error",
            "Internal server error."
    ),
    CHARACTER_NOT_FOUND(
            "RC018",
            "Karakter tidak ditemukan.",
            "Character not found.",
            "Error",
            "Error."
    ),
    FAILED_TO_GENERATE_PROMPT(
            "RC019",
            "Failed generate prompt.",
            "Failed generate prompt.",
            "Error",
            "Error."
    ),
    CHARACTER_NAME_ALREADY_EXISTS(
            "RC020",
            "Character name already exist.",
            "Character name already exist.",
            "Error",
            "Error."
    ),
    BAD_REQUEST(
            "RC021",
            "bad request.",
            "bad request.",
            "Error",
            "Error."
    ), SOME_EXPRESSIONS_NOT_FOUND(
            "RC022",
            "Terdapat expression yang tidak ditemukan.",
            "Data not found.",
            "Error",
            "Error."
    ), CODE_ALREADY_EXISTS(
            "RC023",
            "Code already exists.",
            "Code already exists.",
            "Error",
            "Error."
    ),
    UNAUTHORIZED(
            "RC401",
            "Token tidak valid atau sudah expired.",
            "Token is invalid or has expired.",
            "Unauthorized",
            "Unauthorized"
    ),
    IMAGE_NOT_FOUND(
            "RC024",
            "Token tidak valid atau sudah expired.",
            "Token is invalid or has expired.",
            "Unauthorized",
            "Unauthorized"
    ),
    ;

    private ResponseCode(String code, String defaultMessageId, String defaultMessageEn, String defaultTitleId, String defaultTitleEn) {
        this.code = code;
        this.messageId = defaultMessageId;
        this.messageEn = defaultMessageEn;
        this.titleId = defaultTitleId;
        this.titleEn = defaultTitleEn;
    }

    private String code;
    private boolean status;
    private String messageId;
    private String messageEn;
    private String titleId;
    private String titleEn;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public void setMessageEn(String messageEn) {
        this.messageEn = messageEn;
    }

    public String getTitleId() {
        return titleId;
    }

    public void setTitleId(String titleId) {
        this.titleId = titleId;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public static ResponseCode findByCode(String code) {
        for (ResponseCode responseCode : values()) {
            if (responseCode.getCode().equals(code)) {
                return responseCode;
            }
        }
        return null;
    }
}
