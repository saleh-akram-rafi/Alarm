package com.discipline.alarm.domain.model

data class Mission(
    val id: Long = 0,
    val name: String,
    val image1Path: String = "",
    val image2Path: String = "",
    val image3Path: String = "",
    val embedding1: ByteArray? = null,
    val embedding2: ByteArray? = null,
    val embedding3: ByteArray? = null,
    val threshold: Float = 0.75f,
    val createdAt: Long = System.currentTimeMillis()
) {
    val imagePaths: List<String>
        get() = listOf(image1Path, image2Path, image3Path).filter { it.isNotBlank() }

    val hasAllImages: Boolean
        get() = image1Path.isNotBlank() && image2Path.isNotBlank() && image3Path.isNotBlank()

    val hasEmbeddings: Boolean
        get() = embedding1 != null && embedding2 != null && embedding3 != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mission

        if (id != other.id) return false
        if (name != other.name) return false
        if (threshold != other.threshold) return false
        if (image1Path != other.image1Path) return false
        if (image2Path != other.image2Path) return false
        if (image3Path != other.image3Path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + threshold.hashCode()
        result = 31 * result + image1Path.hashCode()
        result = 31 * result + image2Path.hashCode()
        result = 31 * result + image3Path.hashCode()
        return result
    }
}
