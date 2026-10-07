package net.hitnail.zazen

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val SILENCE_SAMPLE_RATE = 8_000
private const val SILENCE_BYTES_PER_SAMPLE = 2
private const val RIFF_HEADER_BYTES = 12
private const val CHUNK_HEADER_BYTES = 8
private const val WAV_HEADER_BYTES = 44

/** Playback length of a RIFF/WAVE file, from its `fmt ` byte rate and `data` size. */
fun wavDurationMs(bytes: ByteArray): Long {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    require(bytes.size >= RIFF_HEADER_BYTES && bytes.ascii(0) == "RIFF" && bytes.ascii(8) == "WAVE") {
        "Not a WAV file"
    }
    var byteRate: Int? = null
    var offset = RIFF_HEADER_BYTES
    while (offset + CHUNK_HEADER_BYTES <= bytes.size) {
        val id = bytes.ascii(offset)
        val size = buffer.getInt(offset + 4).toLong() and 0xFFFFFFFFL
        when (id) {
            "fmt " -> byteRate = buffer.getInt(offset + CHUNK_HEADER_BYTES + 8)
            "data" -> {
                val rate = requireNotNull(byteRate) { "WAV data chunk precedes fmt chunk" }
                return size * 1_000L / rate
            }
        }
        // Chunks are padded to an even size.
        offset += CHUNK_HEADER_BYTES + size.toInt() + (size.toInt() and 1)
    }
    throw IllegalArgumentException("WAV file has no data chunk")
}

/** Mono 16-bit PCM silence, 8 kHz. */
fun silenceWav(durationMs: Long): ByteArray {
    val dataBytes = (durationMs * SILENCE_SAMPLE_RATE / 1_000L).toInt() * SILENCE_BYTES_PER_SAMPLE
    val byteRate = SILENCE_SAMPLE_RATE * SILENCE_BYTES_PER_SAMPLE
    return ByteBuffer.allocate(WAV_HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(Charsets.US_ASCII))
        putInt(WAV_HEADER_BYTES - CHUNK_HEADER_BYTES + dataBytes)
        put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        putInt(16)
        putShort(1) // PCM
        putShort(1) // mono
        putInt(SILENCE_SAMPLE_RATE)
        putInt(byteRate)
        putShort(SILENCE_BYTES_PER_SAMPLE.toShort())
        putShort((SILENCE_BYTES_PER_SAMPLE * 8).toShort())
        put("data".toByteArray(Charsets.US_ASCII))
        putInt(dataBytes)
    }.array()
}

private fun ByteArray.ascii(offset: Int) = String(this, offset, 4, Charsets.US_ASCII)
