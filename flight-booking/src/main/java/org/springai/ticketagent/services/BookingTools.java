package org.springai.ticketagent.services;

import org.springai.ticketagent.data.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * 航班预订工具配置类
 * 主要功能：将航班预订服务封装为AI模型可调用的函数
 * 通过Spring AI的函数调用(Function Calling)功能，让AI模型能够直接操作预订系统
 */
@Configuration
public class BookingTools {

	// 日志记录器，用于记录操作日志和错误信息
	private static final Logger logger = LoggerFactory.getLogger(BookingTools.class);

	// 注入航班预订服务，包含实际的业务逻辑
	@Autowired
	private FlightBookingService flightBookingService;

	/**
	 * 获取预订详情请求参数记录类
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 */
	public record BookingDetailsRequest(String bookingNumber, String name) {
	}

	/**
	 * 修改预订请求参数记录类
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 * @param date 新日期
	 * @param from 新出发地
	 * @param to 新目的地
	 */
	public record ChangeBookingDatesRequest(String bookingNumber, String name, String date, String from, String to) {
	}

	/**
	 * 取消预订请求参数记录类
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 */
	public record CancelBookingRequest(String bookingNumber, String name) {
	}
//	上述record的传统写法： public final class CancelBookingRequest {
//		private final String bookingNumber;
//		private final String name;
//
//		public CancelBookingRequest(String bookingNumber, String name) {
//			this.bookingNumber = bookingNumber;
//			this.name = name;
//		}
//
//		public String bookingNumber() {
//			return bookingNumber;
//		}
//		省略...
//	}

	/**
	 * 预订详情响应记录类
	 * 包含完整的预订信息，用于返回给AI模型
	 * @JsonInclude(Include.NON_NULL) 表示当字段为null时不序列化
	 */
	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, LocalDate date, BookingStatus bookingStatus,
								 String from, String to, String bookingClass) {
	}

	/**
	 * 注册"获取机票预定详细信息"函数
	 * 该函数将被AI模型调用，用于查询特定预订的详细信息
	 *
	 * @return Function<BookingDetailsRequest, BookingDetails> 接收请求参数，返回预订详情
	 *
	 * @Bean 将函数注册为Spring Bean，供Spring AI框架发现和使用
	 * @Description 提供函数描述，AI模型根据描述决定何时调用此函数
	 */
	@Bean
	@Description("获取机票预定详细信息")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				// 调用实际的业务服务获取预订详情
				return flightBookingService.getBookingDetails(request.bookingNumber(), request.name());
			}
			catch (Exception e) {
				// 异常处理：记录错误日志并返回空的预订详情对象，使用NestedExceptionUtils获取最具体的异常原因
				logger.warn("获取订单详情失败: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());

				// 返回包含请求基本信息但其他字段为null的对象
				return new BookingDetails(request.bookingNumber(), request.name(), null, null, null, null, null);
			}
		};
	}

	/**
	 * 注册"修改机票预定日期"函数
	 * AI模型调用此函数来修改已存在的预订信息
	 *
	 * @return Function<ChangeBookingDatesRequest, String> 接收修改请求，返回空字符串表示成功
	 */
	@Bean
	@Description("修改机票预定日期")
	public Function<ChangeBookingDatesRequest, String> changeBooking() {
		return request -> {
			// 调用业务服务执行修改操作
			flightBookingService.changeBooking(
					request.bookingNumber(),
					request.name(),
					request.date(),
					request.from(),
					request.to()
			);
			// 返回空字符串表示操作成功（AI模型通常只需要知道操作是否成功）
			return "";
		};
//		return request -> {}的等价写法：new Function<ChangeBookingDatesRequest, String>() {
//			@Override
//			public String apply(ChangeBookingDatesRequest request) {
//				flightBookingService.changeBooking(request.bookingNumber(), request.name(), request.date(), request.from(), request.to());
//				return "";
//			}
//		}
	}

	/**
	 * 注册"取消机票预定"函数
	 * AI模型调用此函数来取消指定的预订
	 *
	 * @return Function<CancelBookingRequest, String> 接收取消请求，返回空字符串表示成功
	 */
	@Bean
	@Description("取消机票预定")
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			// 调用业务服务执行取消操作
			flightBookingService.cancelBooking(request.bookingNumber(), request.name());
			// 返回空字符串表示操作成功
			return "";
		};
	}
}