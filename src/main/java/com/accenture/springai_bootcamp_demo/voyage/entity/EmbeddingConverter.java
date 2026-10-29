package com.accenture.springai_bootcamp_demo.voyage.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;

// Stores an embedding as raw 4-byte floats, so 768 dimensions take 3 KB instead of a long text column
@Converter
public class EmbeddingConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] embedding) {
        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * Float.BYTES);
        buffer.asFloatBuffer().put(embedding);
        return buffer.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] bytes) {
        float[] embedding = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes).asFloatBuffer().get(embedding);
        return embedding;
    }
}
