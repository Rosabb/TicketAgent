package org.springai.ticketagent.services;


import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;

/**
 * 简单日志记录拦截器类
 * 功能：在AI聊天过程中记录请求和响应信息，用于调试和监控
 * 实现两种拦截器接口：
 * - CallAroundAdvisor: 处理同步调用
 * - StreamAroundAdvisor: 处理流式调用
 *
 * @author rosabb
 */
public class SimpleLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 是否启用阻塞保护：将处理移到弹性线程池，避免阻塞主线程
    private boolean protectFromBlocking = true;

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerAdvisor.class);

    /**
     * 请求前置处理方法（当前为空实现，可扩展）
     * @param request 请求对象
     * @return 处理后的请求
     */
    private AdvisedRequest before(AdvisedRequest request) {
        return request;
    }

    /**
     * 获取拦截器名称
     * @return 类名作为拦截器名称
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取执行顺序（数值越小优先级越高）
     * @return 执行顺序
     */
    @Override
    public int getOrder() {
        return 0;  // 最高优先级
    }

    /**
     * 响应后置观察方法：记录模型使用情况和响应内容
     * @param response 响应对象
     */
    private void observeAfter(AdvisedResponse response) {
        // 获取模型信息（当前版本可能为空字符串）
        String model = response.response().getMetadata().getModel();

        // 获取token使用统计
        Integer userPromptTokens = response.response().getMetadata().getUsage().getPromptTokens();      // 用户输入token数
        Integer assistantTokens = response.response().getMetadata().getUsage().getCompletionTokens();     // AI生成token数
        Integer totalTokens = response.response().getMetadata().getUsage().getTotalTokens();           // 总token数

        // 获取AI生成的响应内容
        // 先获取Generation对象
        Generation generation = response.response().getResults().get(0);
        // 从Generation对象中获取AssistantMessage对象
        AssistantMessage assistantMessage = generation.getOutput();
        // 从AssistantMessage对象中获取textContent的值
        // String textContent = assistantMessage.textContent();

        // 记录日志：模型、助手消息、token使用情况
        logger.info("模型：{}, 助手消息：{}, 用户提示词Token数：{}, AI生成Token数：{}, 总Token数：{}",
                model, assistantMessage, userPromptTokens, assistantTokens, totalTokens);
    }

    /**
     * 同步调用环绕处理方法
     * 在AI处理前后记录请求和响应信息
     *
     * @param advisedRequest 请求对象
     * @param chain 顾问调用链
     * @return 处理后的响应
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 获取用户发送的请求信息
        String userRequest = advisedRequest.userText();
        logger.info("用户请求: " + userRequest);

        // 调用链中的下一个Advisor，获取模型响应
        AdvisedResponse response = chain.nextAroundCall(advisedRequest);

        // 获取AI响应内容（这里获取的是建议上下文的字符串表示）
        String aiResponse = response.adviseContext().toString();
        logger.info("AI响应: " + aiResponse);

        // todo 这里可以添加将对话记录保存到文件、数据库等存储介质的逻辑
        // 例如使用前面记录对话日志中的方式，调用工具类保存记录

        return response;
    }

    /**
     * 流式调用环绕处理方法
     * 处理流式响应，使用消息聚合器聚合完整响应后记录日志
     *
     * @param advisedRequest 请求对象
     * @param chain 顾问调用链
     * @return 处理后的流式响应
     */
    @NotNull
    @Override
    public Flux<AdvisedResponse> aroundStream(@NotNull AdvisedRequest advisedRequest, @NotNull StreamAroundAdvisorChain chain) {
        // 执行前置处理并获取响应流
        Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain, this::before);

        // 使用消息聚合器聚合流式响应，完成后调用observeAfter方法记录日志
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }

    /**
     * 带阻塞保护的流式处理执行方法
     * 将处理任务移到弹性线程池，避免阻塞响应式流
     *
     * @param advisedRequest 请求对象
     * @param chain 顾问调用链
     * @param beforeAdvise 前置处理函数
     * @return 处理后的响应流
     */
    protected Flux<AdvisedResponse> doNextWithProtectFromBlockingBefore(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain, Function<AdvisedRequest, AdvisedRequest> beforeAdvise) {
        return this.protectFromBlocking
                // 如果启用阻塞保护（true）：在弹性线程池中执行处理，避免阻塞主线程
                ? Mono.just(advisedRequest)  // 将请求包装为 Mono 对象，以便进行响应式处理
                .publishOn(Schedulers.boundedElastic())  // 切换到弹性线程池执行后续操作，boundedElastic 适合阻塞或耗时操作
                .map(beforeAdvise)  // 对请求进行前置处理（转换或增强）， beforeAdvise 是一个函数，接收 AdvisedRequest 返回 AdvisedRequest
                .flatMapMany((request) -> {  // 将 Mono<AdvisedRequest> 转换为 Flux<AdvisedResponse>，flatMapMany 用于将单个元素展开为多个元素的流
                    return chain.nextAroundStream(request);  // 调用顾问链中的下一个流式处理器，返回 Flux<AdvisedResponse> 流
                })

                // 如果禁用阻塞保护（false）：在当前线程直接执行（可能阻塞）.直接对请求进行前置处理,然后调用顾问链中的下一个流式处理器
                : chain.nextAroundStream((AdvisedRequest)beforeAdvise.apply(advisedRequest));
    }

    /* 以下是已被注释的原始流式处理方法，保留以供参考
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        //logger.info(advisedRequest.toString());
        //logger.debug("BEFORE: {}", advisedRequest);
        //Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);

        // 获取用户发送的请求信息
        String userRequest = advisedRequest.userText();
        System.out.println("stream用户请求: " + userRequest);

        // 调用链中的下一个Advisor，获取模型响应
        Flux<AdvisedResponse> response = chain.nextAroundStream(advisedRequest);

        String aiResponse = response.toString();
        System.out.println("streamAI响应: " + aiResponse);

        Flux<AdvisedResponse> message = new MessageAggregator().aggregateAdvisedResponse(response,
                advisedResponse -> logger.debug("AFTER: {}", advisedResponse));
        //processResponse(message);
        // 这里可以添加将对话记录保存到文件、数据库等存储介质的逻辑
        // 例如使用前面记录对话日志中的方式，调用工具类保存记录
        return message;
    }*/

    /**
     * 处理响应流的方法（当前未在主要流程中使用）
     * 订阅响应流并处理每个响应消息
     *
     * @param responseFlux 响应流
     */
    public void processResponse(Flux<AdvisedResponse> responseFlux) {
        responseFlux.subscribe(
                // 处理每个响应
                advisedResponse -> {
                    // 假设直接从 advisedResponse 获取消息内容，需根据实际情况调整
                    String message = extractMessage(advisedResponse);
                    System.out.println("Received message: " + message);
                },
                // 错误处理
                error -> System.err.println("Error occurred: " + error),
                // 完成处理
                () -> System.out.println("Stream completed")
        );
    }

    /**
     * 从响应中提取消息内容
     * 当前实现返回建议上下文的字符串表示
     *
     * @param advisedResponse 响应对象
     * @return 提取的消息内容
     */
    private String extractMessage(AdvisedResponse advisedResponse) {
        // 这里需要根据 AdvisedResponse 实际结构实现消息提取逻辑
        // 例如，如果有 getContent 方法可以直接使用
        return advisedResponse.adviseContext().toString();
        //return null;
    }
}