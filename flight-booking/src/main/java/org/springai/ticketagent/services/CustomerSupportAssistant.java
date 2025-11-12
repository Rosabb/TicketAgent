package org.springai.ticketagent.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 模拟的是一个航空公司的智能客户支持助手，具备：
 * 自然语言交互（ChatClient）
 * 记忆能力（ChatMemory）
 * 知识检索（RAG via VectorStore）
 * 函数调用（Function Calling）
 */
@Service
public class CustomerSupportAssistant {

	//Spring AI的核心聊天客户端，用于与大模型交互
	private final ChatClient chatClient;

	/**
	 * 构造函数：构建配置完整的ChatClient
	 *
	 * @param modelBuilder ChatClient构建器，用于配置聊天客户端
	 * @param vectorStore 向量存储，用于RAG知识检索
	 * @param chatMemory 聊天记忆，用于维护对话上下文
	 * @param toolCallbackProvider Mcp服务配置
	 */
	public CustomerSupportAssistant(ChatClient.Builder modelBuilder, VectorStore vectorStore,
									ChatMemory chatMemory) { //, ToolCallbackProvider toolCallbackProvider

		this.chatClient = modelBuilder
				// 配置系统提示词，定义AI助手的角色和行为规范
				.defaultSystem("""
						您是航空公司的智能客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
						您正在通过在线聊天系统与客户互动。
						您能够支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作，其余功能将在后续版本中添加，如果用户问的问题不支持请告知详情。
					   	在提供有关机票预订详情查询、机票日期改签、机票预订取消等操作之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
					   	在询问用户之前，请检查消息历史记录以获取预订号、客户姓名等信息，尽量避免重复询问给用户造成困扰。
					   	在更改预订之前，您必须确保条款允许这样做。
					   	如果更改需要收费，您必须在继续之前征得用户同意。
					   	使用提供的功能获取预订详细信息、更改预订和取消预订。
					   	如果需要，您可以调用相应函数辅助完成。
					   	请讲中文。
					   	今天的日期是 {current_date}.
					""")
				// 配置拦截器（Advisors），增强AI能力
				.defaultAdvisors(
						new PromptChatMemoryAdvisor(chatMemory), //短期聊天记忆默认存储在内存，维护对话历史
						// new VectorStoreChatMemoryAdvisor(vectorStore)), 向量数据库存储中期记忆 or JdbcChatMemory 使用MySQL等数据库存储长期记忆

						// 前置日志
						new SimpleLoggerAdvisor(),

						// RAG知识库，从向量库中检索相关知识
						new QuestionAnswerAdvisor(vectorStore,
								SearchRequest.builder()
										.topK(4) // 检索最相关的4条知识
										.similarityThresholdAll() // 使用相似度阈值过滤
										.build()),
						// 只检索文档类型为“服务条款”的文档,只检索区域为欧盟或美国的文档
						// new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()
						// 	.withFilterExpression("'documentType' == 'terms-of-service' && region in ['EU', 'US']")),

						// 后置日志
						new SimpleLoggerAdvisor()
				)
				//配置function calling
				.defaultTools("getBookingDetails", "changeBooking", "cancelBooking")
				//配置Mcp服务
				//.defaultTools(toolCallbackProvider)
				.build();
	}

	/**
	 * 处理用户聊天消息
	 * 这是一个响应式方法，返回消息流（Flux）
	 *
	 * @param chatId 会话ID，用于区分不同用户的对话，维护独立的记忆上下文
	 * @param userMessageContent 用户发送的消息内容
	 * @return Flux<String> 响应式流，包含AI的回复内容
	 */
	public Flux<String> chat(String chatId, String userMessageContent) {
		return this.chatClient.prompt()
				// 设置系统参数：当前日期，用于系统提示词中的{current_date}占位符
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				// 设置用户消息
				.user(userMessageContent)
				// 配置拦截器参数
				.advisors(a -> a
						.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)  // 设置会话ID，关联记忆
						.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)       // 检索最近100条对话记录
				)
				// 流式输出内容
				.stream()
				.content();
	}

}