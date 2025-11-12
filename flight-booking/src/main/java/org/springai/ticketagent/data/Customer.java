package org.springai.ticketagent.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 顾客信息
 */
public class Customer {
	//顾客名称
	private String name;
	//个人订单列表
	private List<Booking> bookings = new ArrayList<>();

	public Customer() {
	}

	public Customer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Booking> getBookings() {
		return bookings;
	}

	public void setBookings(List<Booking> bookings) {
		this.bookings = bookings;
	}

}