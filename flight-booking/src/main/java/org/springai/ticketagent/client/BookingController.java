package org.springai.ticketagent.client;


import org.springai.ticketagent.services.BookingTools.BookingDetails;
import org.springai.ticketagent.services.FlightBookingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 航空订单展示页面接口
 */
@Controller
@RequestMapping("/")
@CrossOrigin
public class BookingController {

	private final FlightBookingService flightBookingService;

	public BookingController(FlightBookingService flightBookingService) {
		this.flightBookingService = flightBookingService;
	}

	/**
	 * 首页请求处理方法
	 * 返回应用的主页面
	 */
	@RequestMapping("/")
	public String index() {
		return "index";
	}

	@RequestMapping("/api/bookings")
	@ResponseBody  //返回值直接作为HTTP响应体，不经过视图解析器
	public List<BookingDetails> getBookings() {
		return flightBookingService.getBookings();
	}

}
