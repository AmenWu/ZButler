package com.linestore.action;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;

import com.github.binarywang.wxpay.bean.request.WxEntPayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayBaseRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxEntPayQueryResult;
import com.github.binarywang.wxpay.bean.result.WxEntPayResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.util.SignUtils;
import com.linestore.WxUtils.Sha1Util;
import com.linestore.WxUtils.TemplateMessage;
import com.linestore.WxUtils.XMLUtil;
import com.linestore.service.BusMemberService;
import com.linestore.service.BusTradingService;
import com.linestore.service.BusinessService;
import com.linestore.service.CtaTradingService;
import com.linestore.service.CusAccountService;
import com.linestore.service.CustomerService;
import com.linestore.service.FriendsService;
import com.linestore.service.SettingService;
import com.linestore.vo.BusMember;
import com.linestore.vo.BusTrading;
import com.linestore.vo.Business;
import com.linestore.vo.CtaTrading;
import com.linestore.vo.CusAccount;
import com.linestore.vo.Customer;
import com.linestore.vo.Template;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import com.linestore.vo.Friends;
import com.opensymphony.xwork2.ActionContext;

import jodd.http.HttpResponse;
import jodd.http.net.SSLSocketHttpConnectionProvider;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import net.sf.json.JSONObject;

public class WxPayAction extends WeiXinPayConfigAction implements ServletRequestAware, ServletResponseAware {
	private HttpServletRequest request;
	private HttpServletResponse response;
	protected WxMpService wxService;
	protected WxMpInMemoryConfigStorage config;
	private String result;

	private CtaTradingService ctaTradingService;
	private CusAccountService cusAccountService;
	private BusTradingService busTradingService;
	private BusinessService businessService;
	private BusMemberService busMemberService;
	private SettingService settingService;
	private FriendsService friendsService;

	public FriendsService getFriendsService() {
		return friendsService;
	}

	public void setFriendsService(FriendsService friendsService) {
		this.friendsService = friendsService;
	}

	public SettingService getSettingService() {
		return settingService;
	}

	public void setSettingService(SettingService settingService) {
		this.settingService = settingService;
	}

	public BusMemberService getBusMemberService() {
		return busMemberService;
	}

	public void setBusMemberService(BusMemberService busMemberService) {
		this.busMemberService = busMemberService;
	}

	public BusinessService getBusinessService() {
		return businessService;
	}

	public void setBusinessService(BusinessService businessService) {
		this.businessService = businessService;
	}

	public BusTradingService getBusTradingService() {
		return busTradingService;
	}

	public void setBusTradingService(BusTradingService busTradingService) {
		this.busTradingService = busTradingService;
	}

	public CusAccountService getCusAccountService() {
		return cusAccountService;
	}

	public void setCusAccountService(CusAccountService cusAccountService) {
		this.cusAccountService = cusAccountService;
	}

	public CtaTradingService getCtaTradingService() {
		return ctaTradingService;
	}

	public void setCtaTradingService(CtaTradingService ctaTradingService) {
		this.ctaTradingService = ctaTradingService;
	}

	public CustomerService getCustomerService() {
		return customerService;
	}

	public void setCustomerService(CustomerService customerService) {
		this.customerService = customerService;
	}

	private CustomerService customerService;

	public WxPayAction() {
		super();
		// TODO Auto-generated constructor stub
		config = new WxMpInMemoryConfigStorage();
		config.setAppId("wx5b69c56ac01ed858"); // 设置微信公众号的appid
		config.setSecret("4ad3ebbd02e8f82aede3a22d1a3335a6"); // 设置微信公众号的
																// appsecret
		config.setToken("wxdev"); // 设置微信公众号的token

		config.setOauth2redirectUri("http://yanglan520.com/ZButler/WxOauthRedirect!oauth.action");
		this.wxService = new WxMpServiceImpl();
		wxService.setWxMpConfigStorage(config);
	}

	@Override
	public void setServletResponse(HttpServletResponse response) {
		// TODO Auto-generated method stub
		this.response = response;

	}

	@Override
	public void setServletRequest(HttpServletRequest request) {
		// TODO Auto-generated method stub
		this.request = request;

	}

	// 统一下单
	public String getPayInfo() throws WxPayException {
		// 产生订单号（订单号重复）
		// 获取业务类型 R-充值/P-支付商品
		String service = request.getParameter("service").toUpperCase();
		String out_trade_no = null;
		// 获取金额
		String payNum = request.getParameter("payNum");

		// 构建订单标题
		String orderTitle;
		if (service.equals("R")) {
			orderTitle = "众邦管家-零钱充值";
			out_trade_no = new java.util.Date().getTime() + service + this.RandomStr();
		} else if (service.equals("P")) {
			orderTitle = "众邦管家-商品支付";
			out_trade_no = new java.util.Date().getTime() + service + this.RandomStr() + "D"
					+ request.getParameter("busId");

		} else {
			orderTitle = "其他业务";
			out_trade_no = new java.util.Date().getTime() + this.RandomStr();
		}

		Map<String, String> payInfo = this.wxPayService.getPayInfo(
				WxPayUnifiedOrderRequest.newBuilder().body(orderTitle).totalFee(WxPayBaseRequest.yuanToFee(payNum))
						.spbillCreateIp(ServletActionContext.getRequest().getRemoteAddr())
						.notifyURL("http://yanglan520.com/ZButler/WxPay!payNotify.action").tradeType("JSAPI") // 交易类型
						.outTradeNo(out_trade_no) // 唯一订单
						.openid((String) ActionContext.getContext().getSession().get("SCOPE_BASE_OPENID")).build());
		this.result = JSONObject.fromObject(payInfo).toString();
		return SUCCESS;
	}

	// 回调处理

	public void payNotify() {
		String returnString = null;
		try {
			synchronized (this) {
				Map<String, String> kvm = XMLUtil.parseRequestXmlToMap(request);
				if (SignUtils.checkSign(kvm, this.payConfig.getMchKey())) {
					if (kvm.get("result_code").equals("SUCCESS")) {
						// 应答微信
						response.setContentType(" text/xml");
						response.setCharacterEncoding("utf-8");
						response.getWriter().write(
								"<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>");

						// 区分业务逻辑
						String out_trade_no = kvm.get("out_trade_no");
						String service = out_trade_no.substring(out_trade_no.length() - 5, out_trade_no.length() - 4);

						switch (service) {
						// 获取业务类型 R-充值/P-支付商品
						case "P":
							// 转账
							//
							// 存数据库+转账
							Date Pdate = new Date();
							String openIdbus = kvm.get("openid");
							Business bus = businessService.select(subString(kvm.get("out_trade_no"))); // 通过商家ID获取商家
							// 付款订单
							BusTrading bta = new BusTrading();
							bta.setBtaId(kvm.get("out_trade_no"));
							bta.setBtaAddress(bus.getBaCity());
							bta.setBtaMoney(Float.valueOf(kvm.get("total_fee")));
							bta.setBtaStatus(1);
							bta.setBtaTime(new Timestamp(Pdate.getTime()));
							bta.setBtaType(1);
							bta.setBusiness(bus);
							busTradingService.addBusTrading(bta);
							List<Customer> Pcus = customerService.findByOpenId(openIdbus);
							if (Pcus != null && Pcus.size() > 0) {
								if (Pcus.get(0).getCusPhone() != null && "".equals(Pcus.get(0).getCusPhone())) {
									Friends fri = friendsService.queryByPhone(Pcus.get(0).getCusPhone());
									if (fri != null) {
										if (fri.getFriType() == 2) {
											CusAccount addChangeCac = cusAccountService
													.findByCusId(fri.getCustomer().getCusId());
											float addChange = Float.valueOf(kvm.get("total_fee"))
													* Float.valueOf(settingService.queryById(3).getSetValue());
											addChangeCac.setCacChange(addChangeCac.getCacChange() + addChange);
											CtaTrading addChangeCta = new CtaTrading();
											addChangeCta.setCtaMoney(addChange);
											addChangeCta.setCtaTime(new Timestamp(Pdate.getTime()));
											addChangeCta.setCtaType(2);
											addChangeCta.setCustomer(fri.getCustomer());
											addChangeCta.setCtaId(Pdate.getTime() + "F" + this.RandomStr()); // 付款返零钱
											ctaTradingService.addCtaTrading(addChangeCta);
											cusAccountService.updateCusAccount(addChangeCac);

										}
									}
								}
								Business addBus = businessService.select(Pcus.get(0).getCusId());
								if (addBus != null) {
									if (addBus.getBusLevel() != 1) {
										CusAccount addCac = cusAccountService.findByCusId(Pcus.get(0).getCusId());
										float addChange = Float.valueOf(kvm.get("total_fee"))
												* Float.valueOf(settingService.queryById(5).getSetValue());
										addCac.setCacPoints(addCac.getCacPoints() + addChange);
										CtaTrading addChangeCta = new CtaTrading();
										addChangeCta.setCtaMoney(addChange);
										addChangeCta.setCtaTime(new Timestamp(Pdate.getTime()));
										addChangeCta.setCtaType(12);
										addChangeCta.setCustomer(Pcus.get(0));
										addChangeCta.setCtaId(Pdate.getTime() + "J" + this.RandomStr()); // 付款返积分
										ctaTradingService.addCtaTrading(addChangeCta);
										cusAccountService.updateCusAccount(addCac);
									}
								}
							}
							// 存数据库+转账
							WxEntPayRequest wxEntPayRequest = new WxEntPayRequest();
							wxEntPayRequest.setAmount(Integer.parseInt(kvm.get("total_fee")));
							wxEntPayRequest.setDescription("");
							wxEntPayRequest.setOpenid("");
							this.payToIndividual(wxEntPayRequest, wxPayService);
							break;
						case "R":
							CtaTrading cta = new CtaTrading();
							String openId = kvm.get("openid");
							Customer cus = (Customer) customerService.findByOpenId(openId).get(0);
							CusAccount cusAccount = cusAccountService.findByCusId(cus.getCusId());
							Float money = cusAccount.getCacChange() + Float.valueOf(kvm.get("total_fee")) / 100;
							cusAccountService.updateField("cacChange", String.valueOf(money), cusAccount.getCacId());
							cta.setCustomer(cus);
							cta.setCtaMoney(Float.valueOf(kvm.get("total_fee")) / 100);
							cta.setCtaId(kvm.get("out_trade_no"));
							cta.setCtaType(1);
							Date Rdate = new Date();
							cta.setCtaTime(new Timestamp(Rdate.getTime()));
							ctaTradingService.addCtaTrading(cta);
							

							if (cus.getCusPhone() != null && "".equals(cus.getCusPhone())) {
								// 充值零钱返积分
								Friends fris = friendsService.queryByPhone(cus.getCusPhone());
								if (fris != null) {
									CusAccount addPointAcc = cusAccountService
											.findByCusId(fris.getCustomer().getCusId());
									CtaTrading addPointCta = new CtaTrading();
									addPointAcc.setCacPoints(addPointAcc.getCacPoints()
											+ money * Float.valueOf(settingService.queryById(1).getSetValue()));
									cusAccountService.updateCusAccount(addPointAcc);
									addPointCta.setCtaMoney(
											money * Float.valueOf(settingService.queryById(1).getSetValue()));
									addPointCta.setCtaTime(new Timestamp(Rdate.getTime()));
									addPointCta.setCtaType(11);
									addPointCta.setCustomer(fris.getCustomer());
									addPointCta.setCtaId(Rdate.getTime() + "Z" + this.RandomStr());
									ctaTradingService.addCtaTrading(addPointCta);
								}
							}
                
                	// 构建模板消息
							Template template = new Template();
							template.setFirst("众邦管家---零钱充值");
							Map<String, String> map = new HashMap<String, String>();
							map.put("keyword1", cus.getCusNickname());
							map.put("keyword2", kvm.get("out_trade_no"));
							map.put("keyword3", (Float.toString(Float.valueOf(kvm.get("total_fee")) / 100)));
							map.put("keyword4", "零钱充值");
							template.setKeyword(map);
							template.setOpenId(kvm.get("openid"));
							template.setRemark("零钱已经到账，请注意查收");
							TemplateMessage.RechargeMoneyNotify(template, this.wxService);
							break;
						default:
							break;
						}

						System.out.println("out_trade_no: " + kvm.get("out_trade_no") + " pay SUCCESS!");

					} else {
						response.setContentType(" text/xml");
						response.setCharacterEncoding("utf-8");
						System.out.println("out_trade_no: " + kvm.get("out_trade_no") + " result_code is FAIL");
						response.getWriter().write(
								"<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[result_code is FAIL]]></return_msg></xml>");
					}
				} else {
					response.setContentType(" text/xml");
					response.setCharacterEncoding("utf-8");
					response.getWriter().write(
							"<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[check signature FAIL]]></return_msg></xml>");
					System.out.println("out_trade_no: " + kvm.get("out_trade_no") + " check signature FAIL");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// 企业付款到个人

	// public void payToIndividual() throws WxPayException {
	//
	//
	// // 转出
	// // 查询余额
	// String partner_trade_no = new java.util.Date().getTime() + "";
	// WxEntPayRequest wxEntPayRequest = new WxEntPayRequest();
	// wxEntPayRequest.setPartnerTradeNo(partner_trade_no);
	// wxEntPayRequest.setOpenid("ojOQA0y9o-Eb6Aep7uVTdbkJqrP4");
	// wxEntPayRequest.setCheckName("NO_CHECK");
	// wxEntPayRequest.setAmount(10);
	// wxEntPayRequest.setDescription("test");
	// wxEntPayRequest.setSpbillCreateIp("10.10.10.10");
	//
	//// this.wxPayService.queryEntPay(arg0);
	//
	// try {
	// WxEntPayResult wxEntPayResult =
	// this.wxPayService.entPay(wxEntPayRequest);
	// if ("SUCCESS".equals(wxEntPayResult.getResultCode().toUpperCase())
	// && "SUCCESS".equals(wxEntPayResult.getReturnCode().toUpperCase())) {
	// System.out.println("企业对个人付款成功！\n付款信息：\n" + wxEntPayResult.toString());
	// } else {
	//
	// System.out.println("err_code: " + wxEntPayResult.getErrCode() + "
	// err_code_des: "
	// + wxEntPayResult.getErrCodeDes());
	// }
	// } catch (Exception e) {
	//// e.printStackTrace();
	//
	// System.out.println(e.toString());
	// }
	// }

	public void paymentToMerchant(WxEntPayRequest wxEntPayRequest, WxPayService wxPayService) {
		String resutl = payToIndividual(wxEntPayRequest, wxPayService);
		if (resutl.equals("SUCCESS")) {
			System.out.println("SUCCESS");
		} else {
			System.out.println(resutl);
		}

	}

	public void postal() {
		// 构建提现 WxEntPayRequest
		WxEntPayRequest request = new WxEntPayRequest();
		String partner_trade_no = new java.util.Date().getTime() + "" + "";
		WxEntPayRequest wxEntPayRequest = new WxEntPayRequest();
		wxEntPayRequest.setPartnerTradeNo(partner_trade_no);
		wxEntPayRequest.setOpenid("ojOQA0y9o-Eb6Aep7uVTdbkJqrP4");
		wxEntPayRequest.setAmount(10);
		wxEntPayRequest.setDescription("test");
		String resutl = payToIndividual(wxEntPayRequest, this.wxPayService);

		if (resutl.equals("SUCCESS")) {
			// System.out.println("SUCCESS");

		} else {
			System.out.println(resutl);
		}
	}

	public String payToIndividual(WxEntPayRequest wxEntPayRequest, WxPayService wxPayService) {
		wxEntPayRequest.setCheckName("NO_CHECK");
		wxEntPayRequest.setSpbillCreateIp(this.request.getRemoteAddr());
		WxEntPayResult wxEntPayResult = null;
		try {
			wxEntPayResult = wxPayService.entPay(wxEntPayRequest);
			if ("SUCCESS".equals(wxEntPayResult.getResultCode().toUpperCase())
					&& "SUCCESS".equals(wxEntPayResult.getReturnCode().toUpperCase())) {
				return "SUCCESS";
			}
		} catch (WxPayException e) {
			// TODO Auto-generated catch block
			System.out.println("错误异常代码" + e);
		}

		return "err_code: " + wxEntPayResult.getErrCode() + "err_code_des: " + wxEntPayResult.getErrCodeDes();
	}

	public void queryResult() throws WxPayException {
		// 构建查询体
		String partner_trade_no = "";
		WxEntPayQueryResult wxEntPayQueryResult = this.wxPayService.queryEntPay(partner_trade_no);
		if ("SUCCESS".equals(wxEntPayQueryResult.getResultCode().toUpperCase())
				&& "SUCCESS".equals(wxEntPayQueryResult.getReturnCode().toUpperCase())) {
			// 构建返回结果
		}
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	private String RandomStr() {
		Random random = new Random();
		String radnString = "";
		for (int i = 0; i < 4; i++) {
			radnString += (int) (Math.random() * 10);
		}

		return radnString;
	}

	public int subString(String a) {
		int index = a.length();
		for (int i = 0; i < index; i++) {
			if (a.charAt(i) == 'D') {
				index = i;
			}
		}
		return Integer.parseInt(a.substring(index + 1));
	}

}
