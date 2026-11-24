package com.cloud.programmer.knowledge.embedding;

import java.util.List;

public interface EmbeddingClient {

    /**
     * 将文本转换为向量
     */
    List<Float> embed(String text);

    /**
     * 批量转换文本
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     */
    int dimensions();

    /**
     * 获取模型名称
     */
    String modelName();
}