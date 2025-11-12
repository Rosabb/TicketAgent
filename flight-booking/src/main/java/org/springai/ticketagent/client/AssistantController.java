package org.springai.ticketagent.client;

import org.springai.ticketagent.services.CustomerSupportAssistant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 智能助手控制器
 * 提供与前端交互的 REST API 接口
 * 功能：处理用户聊天请求，返回AI助手的流式响应
 */
@RequestMapping("/api/assistant")
@RestController
@CrossOrigin
public class AssistantController {

	// 注入客户支持助手服务
	private final CustomerSupportAssistant agent;

	/**
	 * 构造函数
	 * @param agent 客户支持助手服务实例
	 */
	public AssistantController(CustomerSupportAssistant agent) {
		this.agent = agent;
	}

	/**
	 * 聊天接口：处理用户消息并返回AI的流式响应
	 * 使用 Server-Sent Events (SSE) 技术实现实时流式传输，produces = MediaType.TEXT_EVENT_STREAM_VALUE
	 *
	 * @param chatId 会话ID，用于区分不同用户的对话上下文
	 * @param message 用户发送的消息内容
	 * @return Flux<String> 流式响应，包含AI的逐词输出
	 * @throws InterruptedException 当线程等待被中断时抛出
	 */
	@RequestMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(String chatId, String message) throws InterruptedException {
		// 调用AI服务获取流式响应，并在流结束时添加完成标记
		Flux<String> messages = agent.chat(chatId, message).concatWith(Flux.just("[complete]"));

		// 打印格式化后的响应内容（用于调试和日志记录）
		System.out.println("答复:" + formatStr(chatId, messages));

		// 返回流式响应给前端
		return messages;
	}

	/**
	 * 格式化流式响应为字符串
	 * 将 Flux<String> 流中的所有元素收集并合并为单个字符串
	 * 注意：这个方法会阻塞当前线程直到流完成，仅用于调试目的
	 *
	 * @param chatId 会话ID
	 * @param messages 流式消息
	 * @return 合并后的字符串内容
	 * @throws InterruptedException 当线程等待被中断时抛出
	 */
	public String formatStr(String chatId, Flux<String> messages) throws InterruptedException {
		// 创建列表用于收集流中的元素
		List<String> str = new ArrayList<>();

		// 使用 CountDownLatch 实现线程同步，等待流处理完成
		CountDownLatch latch = new CountDownLatch(1);

		// 订阅流并收集所有元素
		messages.collectList()
				.subscribe(
						// 成功回调：将收集到的列表添加到结果中
						list -> {
							System.out.println("收集到的列表: " + list);
							str.addAll(list);
							latch.countDown();  // 计数减一，释放等待的线程
						},
						// 错误回调：打印错误信息
						error -> {
							System.err.println("错误: " + error);
							latch.countDown();  // 即使出错也要释放等待的线程
						}
				);

		// 阻塞当前线程，直到 latch 计数为0（即流处理完成）
		latch.await();

		// 将收集到的所有字符串片段合并为一个完整字符串
		return String.join("", str);
	}

}