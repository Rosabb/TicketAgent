package org.springai.ticketagent.data;

import java.time.LocalDate;

/**
 * 订单信息
 */
public class Booking {
	//订单ID
	private String bookingNumber;
	//预定日期
	private LocalDate date;
	//起飞日期
	private LocalDate bookingTo;
	//顾客信息
	private Customer customer;
	//出发城市
	private String from;
	//目的地城市
	private String to;
	//订单状态
	private BookingStatus bookingStatus;
	//座位类型
	private BookingClass bookingClass;

	public Booking(String bookingNumber, LocalDate date, Customer customer, BookingStatus bookingStatus, String from,
			String to, BookingClass bookingClass) {
		this.bookingNumber = bookingNumber;
		this.date = date;
		this.customer = customer;
		this.bookingStatus = bookingStatus;
		this.from = from;
		this.to = to;
		this.bookingClass = bookingClass;
	}

	public String getBookingNumber() {
		return bookingNumber;
	}

	public void setBookingNumber(String bookingNumber) {
		this.bookingNumber = bookingNumber;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalDate getBookingTo() {
		return bookingTo;
	}

	public void setBookingTo(LocalDate bookingTo) {
		this.bookingTo = bookingTo;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public BookingStatus getBookingStatus() {
		return bookingStatus;
	}

	public void setBookingStatus(BookingStatus bookingStatus) {
		this.bookingStatus = bookingStatus;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public BookingClass getBookingClass() {
		return bookingClass;
	}

	public void setBookingClass(BookingClass bookingClass) {
		this.bookingClass = bookingClass;
	}

}