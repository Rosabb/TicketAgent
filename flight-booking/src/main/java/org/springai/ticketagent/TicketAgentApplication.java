package org.springai.ticketagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.StdioTransportAutoConfiguration;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

/**
 * 航班预订应用主启动类
 * 核心功能：
 * - 启动 Spring Boot 后台应用
 * - 配置 AI 相关组件（向量存储、聊天记忆等）
 * - 初始化 RAG 知识库
 * - 提供基础服务 Bean
 */
@SpringBootApplication(exclude = {McpClientAutoConfiguration.class, StdioTransportAutoConfiguration.class})
public class TicketAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(TicketAgentApplication.class);


    public static void main(String[] args) {
        SpringApplication.run(TicketAgentApplication.class, args);
    }

    /**
     * 命令行运行器：在应用启动后执行知识库初始化
     * 功能：将服务条款文档摄入到向量存储中，构建 RAG 知识库
     *
     * @param embeddingModel 嵌入模型，用于文本向量化（自动注入）
     * @param vectorStore 向量存储，用于存储和检索向量化文档（自动注入）
     * @param termsOfServiceDocs 服务条款文档资源（从 classpath 加载）
     * @return CommandLineRunner 实例，Spring Boot 会自动执行
     */
    @Bean
    CommandLineRunner ingestTermOfServiceToVectorStore(EmbeddingModel embeddingModel, VectorStore vectorStore,
                                                       @Value("classpath:rag/terms-of-service.txt") Resource termsOfServiceDocs) {

        return args -> {
            logger.info("开始初始化 RAG 知识库...");

            // 文档处理流水线：
            // 1. TextReader 读取 resources/rag/terms-of-service.txt 文件内容
            // 2. TokenTextSplitter 按token长度切分文本（避免大文本超出模型限制）
            // 3. 向量化存储：通过 VectorStore.write() 将文本向量存入内存（后续可用于RAG检索）
            vectorStore.write(
                    new TokenTextSplitter()
                            .transform(
                                new TextReader(termsOfServiceDocs).read()
                            )
            );

            logger.info("服务条款文档已成功摄入向量存储");

            // 相似性搜索检测：验证向量存储是否正常工作
            // 搜索与"取消预订"相关的文档片段
            logger.info("执行相似性搜索测试...");
            vectorStore.similaritySearch("Cancelling Bookings").forEach(doc -> {
                logger.info("检索到的相关文档片段: {}", doc.getText());
            });

            logger.info("RAG 知识库初始化完成");
        };
    }

    /**
     * 配置向量存储 Bean
     * 向量存储是 RAG（检索增强生成）的核心组件，用于：
     * - 存储文档的向量化表示
     * - 支持基于相似度的文档检索
     * - 为 AI 模型提供上下文知识
     *
     * @param embeddingModel 嵌入模型，用于将文本转换为向量（自动注入）
     * @return 基于内存的简单向量存储实例
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 使用 SimpleVectorStore 构建器创建向量存储，特点：基于内存、轻量级、适合开发和测试环境
        // 生产环境可考虑使用 PersistentVectorStore
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 配置聊天记忆 Bean
     * 功能：存储多轮对话历史，实现上下文感知的连续对话
     * 基于内存的实现，重启应用后记忆会丢失
     *
     * @return 内存聊天记忆实例
     */
    @Bean
    public ChatMemory chatMemory() {
        // InMemoryChatMemory 特点：
        // - 基于 ConcurrentHashMap 实现，线程安全
        // - 存储用户与AI的对话历史
        // - 支持按会话ID隔离不同用户的对话上下文
        // - 简单易用，适合中小型应用
        return new InMemoryChatMemory();
    }

    /**
     * 配置 REST 客户端构建器 Bean
     * 功能：提供可自定义的HTTP客户端，用于调用外部API
     * 条件：当容器中不存在其他 RestClient.Builder 实例时才创建
     *
     * @return REST 客户端构建器实例
     */
    @Bean
    @ConditionalOnMissingBean  // 条件注解：只有当容器中不存在该类型的Bean时才创建
    public RestClient.Builder restClientBuilder() {
        // RestClient 是 Spring 6 引入的新的 HTTP 客户端
        // 特点：
        // - 替代传统的 RestTemplate
        // - 函数式风格，更简洁的API
        // - 更好的性能和非阻塞支持
        // - 用于调用外部服务，如支付网关、天气API等
        return RestClient.builder();
    }

}