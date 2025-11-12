package org.springai.ticketagent.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单数据
 */
public class BookingData {
	/**
	 * 顾客列表
	 */
	private List<Customer> customers = new ArrayList<>();
	/**
	 * 订单列表
	 */
	private List<Booking> bookings = new ArrayList<>();

	public List<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(List<Customer> customers) {
		this.customers = customers;
	}

	public List<Booking> getBookings() {
		return bookings;
	}

	public void setBookings(List<Booking> bookings) {
		this.bookings = bookings;
	}

}
