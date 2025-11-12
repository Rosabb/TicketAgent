package org.springai.ticketagent.services;

import org.springai.ticketagent.data.*;
import org.springai.ticketagent.services.BookingTools.BookingDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 模拟航班预定系统
 * 主要功能：
 * - 航班预订管理：创建、查询、修改、取消预订
 * - 数据初始化：启动时生成模拟数据
 * - 业务规则验证：如时间限制（48小时内不可取消）
 */
@Service
public class FlightBookingService {

	// 内存数据存储，模拟数据库
	private final BookingData db;

	/**
	 * 构造函数：初始化服务时创建数据存储并生成演示数据
	 */
	public FlightBookingService() {
		db = new BookingData();  // 初始化数据存储
		initDemoData();  // 生成初始演示数据
	}

	/**
	 * 初始化演示数据：随机生成5条订单记录
	 * 包含客户信息、航班信息、预订状态等
	 */
	private void initDemoData() {
		// 预定义测试数据
		List<String> names = List.of("张三", "李四", "王五", "赵六", "伍小宝");
		List<String> airportCodes = List.of("北京", "上海", "广州", "深圳", "杭州", "南京", "青岛", "成都", "武汉", "西安", "重庆", "大连", "天津");
		Random random = new Random();

		// 创建客户和预订列表
		List<Customer> customers = new ArrayList<>();
		List<Booking> bookings = new ArrayList<>();

		// 生成5条模拟预订记录
		for (int i = 0; i < 5; i++) {
			String name = names.get(i);  // 获取客户姓名
			String from = airportCodes.get(random.nextInt(airportCodes.size()));  // 随机出发地
			String to = airportCodes.get(random.nextInt(airportCodes.size()));  // 随机目的地

			// 随机选择舱位等级
			BookingClass bookingClass = BookingClass.values()[random.nextInt(BookingClass.values().length)];

			// 创建客户对象
			Customer customer = new Customer();
			customer.setName(name);

			// 设置航班日期：当前日期 + 2*(i+1)天，确保日期递增
			LocalDate date = LocalDate.now().plusDays(2 * (i + 1));

			// 创建预订记录
			Booking booking = new Booking("10" + (i + 1), date, customer, BookingStatus.CONFIRMED, from, to, bookingClass);

			// 建立客户与预订的双向关联
			customer.getBookings().add(booking);

			// 添加到列表
			customers.add(customer);
			bookings.add(booking);
		}

		// 将数据设置到数据库存储中
		db.setCustomers(customers);
		db.setBookings(bookings);
	}

	/**
	 * 获取所有预订记录
	 * @return 所有预订的详细信息列表
	 */
	public List<BookingDetails> getBookings() {
		// 将内部Booking对象转换为对外暴露的BookingDetails对象
		return db.getBookings().stream().map(this::toBookingDetails).toList();
	}

	/**
	 * 根据预订编号和客户姓名查找预订记录
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 * @return 找到的Booking对象
	 * @throws IllegalArgumentException 当预订不存在时抛出异常
	 */
	private Booking findBooking(String bookingNumber, String name) {
		return db.getBookings()
				.stream()
				.filter(b -> b.getBookingNumber().equalsIgnoreCase(bookingNumber))  // 匹配预订编号（忽略大小写）
				.filter(b -> b.getCustomer().getName().equalsIgnoreCase(name))  // 匹配客户姓名（忽略大小写）
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("订单不存在"));  // 未找到时抛出异常
	}

	/**
	 * 获取特定预订的详细信息
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 * @return 预订详细信息
	 */
	public BookingDetails getBookingDetails(String bookingNumber, String name) {
		var booking = findBooking(bookingNumber, name);  // 查找预订记录
		return toBookingDetails(booking);  // 转换为详细信息对象
	}

	/**
	 * 修改预订信息（日期、出发地、目的地）
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 * @param newDate 新日期（字符串格式，需可解析为LocalDate）
	 * @param from 新出发地
	 * @param to 新目的地
	 * @throws IllegalArgumentException 当航班起飞前24小时内不允许修改时抛出异常
	 */
	public void changeBooking(String bookingNumber, String name, String newDate, String from, String to) {
		var booking = findBooking(bookingNumber, name);  // 查找预订记录

		// 业务规则验证：起飞前24小时内不允许修改
		if (booking.getDate().isBefore(LocalDate.now().plusDays(1))) {
			throw new IllegalArgumentException("航班起飞前24小时内不允许修改预订");
		}

		// 更新预订信息
		booking.setDate(LocalDate.parse(newDate));  // 解析并设置新日期
		booking.setFrom(from);  // 设置新出发地
		booking.setTo(to);  // 设置新目的地
	}

	/**
	 * 取消预订
	 * @param bookingNumber 预订编号
	 * @param name 客户姓名
	 * @throws IllegalArgumentException 当航班起飞前48小时内不允许取消时抛出异常
	 */
	public void cancelBooking(String bookingNumber, String name) {
		var booking = findBooking(bookingNumber, name);  // 查找预订记录

		// 业务规则验证：起飞前48小时内不允许取消
		if (booking.getDate().isBefore(LocalDate.now().plusDays(2))) {
			throw new IllegalArgumentException("航班起飞前48小时内不允许取消预订");
		}

		// 更新预订状态为已取消
		booking.setBookingStatus(BookingStatus.CANCELLED);
	}

	/**
	 * 将内部Booking对象转换为对外暴露的BookingDetails对象
	 * 用于数据封装和接口返回
	 * @param booking 内部预订对象
	 * @return 对外预订详细信息对象
	 */
	private BookingDetails toBookingDetails(Booking booking) {
		return new BookingDetails(
				booking.getBookingNumber(),        // 预订编号
				booking.getCustomer().getName(),   // 客户姓名
				booking.getDate(),                 // 航班日期
				booking.getBookingStatus(),        // 预订状态
				booking.getFrom(),                 // 出发地
				booking.getTo(),                   // 目的地
				booking.getBookingClass().toString()  // 舱位等级（转换为字符串）
		);
	}
}