/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.data;

import java.util.List;

import io.pergasus.util.collection.ECCardData;

public class CardData implements ECCardData<Purchase> {
	private String headTitle;
	private Integer headBackgroundResource;
	private Integer mainBackgroundResource;
	
	private String orderNumber;
	private String orderDate;
	private String orderPrice;
	private List<Purchase> listItems;
	
	public CardData() {
	}
	
	@Override
	public List<Purchase> getListItems() {
		return listItems;
	}
	
	public void setListItems(List<Purchase> listItems) {
		this.listItems = listItems;
	}
	
	public String getHeadTitle() {
		
		return headTitle;
	}
	
	public void setHeadTitle(String headTitle) {
		this.headTitle = headTitle;
	}
	
	@Override
	public Integer getHeadBackgroundResource() {
		return headBackgroundResource;
	}
	
	public void setHeadBackgroundResource(Integer headBackgroundResource) {
		this.headBackgroundResource = headBackgroundResource;
	}
	
	@Override
	public Integer getMainBackgroundResource() {
		return mainBackgroundResource;
	}
	
	public void setMainBackgroundResource(Integer mainBackgroundResource) {
		this.mainBackgroundResource = mainBackgroundResource;
	}
	
	public String getOrderNumber() {
		return orderNumber;
	}
	
	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}
	
	public String getOrderDate() {
		return orderDate;
	}
	
	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}
	
	public String getOrderPrice() {
		return orderPrice;
	}
	
	public void setOrderPrice(String orderPrice) {
		this.orderPrice = orderPrice;
	}
}
