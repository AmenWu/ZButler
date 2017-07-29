package com.linestore.action;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;

import com.github.binarywang.wxpay.bean.WxPayOrderNotifyResponse;
import com.github.binarywang.wxpay.bean.request.WxEntPayQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxEntPayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayBaseRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxEntPayResult;
import com.github.binarywang.wxpay.bean.result.WxPayBaseResult;
import com.github.binarywang.wxpay.bean.result.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.binarywang.wxpay.util.SignUtils;
import com.linestore.WxUtils.Sha1Util;
import com.linestore.WxUtils.XMLUtil;
import com.linestore.service.CtaTradingService;
import com.linestore.service.CusAccountService;
import com.linestore.service.CustomerService;
import com.linestore.vo.CtaTrading;
import com.linestore.vo.CusAccount;
import com.linestore.vo.Customer;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

import jodd.http.HttpResponse;
import jodd.http.net.SSLSocketHttpConnectionProvider;
import me.chanjar.weixin.mp.api.WxMpService;
import net.sf.json.JSONObject;

public class WxPayAction extends WeiXinPayConfigAction implements ServletRequestAware, ServletResponseAware {
	private HttpServletRequest request;
	private HttpServletResponse response;
	private String result;
	
	private CtaTradingService ctaTradingService;
	private CusAccountService cusAccountService;
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
		int min = 1000;
		int max = 9999;
		Random random = new Random();
		// 获取业务类型 R-充值/P-支付商品
		String service = request.getParameter("service").toUpperCase();
		String out_trade_no = new java.util.Date().getTime() + service + random.nextInt(max) % (max - min + 1);
		// 获取金额
		String payNum = request.getParameter("payNum");

		// 构建订单标题
		String orderTitle;
		if (service.equals("R")) {
			orderTitle = "众邦管家-零钱充值";
		} else if (service.equals("P")) {
			orderTitle = "众邦管家-商品支付";
		} else {
			orderTitle = "其他业务";
		}

		Map<String, String> payInfo = this.wxPayService.getPayInfo(
				WxPayUnifiedOrderRequest.newBuilder().body(orderTitle).totalFee(WxPayBaseRequest.yuanToFee(payNum))
						.spbillCreateIp(ServletActionContext.getRequest().getRemoteAddr())
						.notifyURL("http://yanglan520.com/ZButler/WxPay!payNotify.action").tradeType("JSAPI") // 交易类型
						.outTradeNo(out_trade_no) // 唯一订单
						.openid((String) ActionContext.getContext().getSession().get("SCOPE_BASE_OPENID")).build());
		this.result = JSONObject.fromObject(payInfo).toString();
		
		System.out.println(this.result.toString());
		return SUCCESS;
	}

	// 回调处理

	public void payNotify() {

		try {
			synchronized (this) {
				Map<String, String> kvm = XMLUtil.parseRequestXmlToMap(request);
				if (SignUtils.checkSign(kvm, this.payConfig.getMchKey())) {
					if (kvm.get("result_code").equals("SUCCESS")) {
						// 区分业务逻辑
						String out_trade_no = kvm.get("out_trade_no");
						String service = out_trade_no.substring(out_trade_no.length() - 5, out_trade_no.length() - 4);
						System.out.println(service);
						switch (service) {
						// 获取业务类型 R-充值/P-支付商品
						case "P":
							// 转账
							
							break;
						case "R":
							CtaTrading cta = new CtaTrading();
							String openId = kvm.get("openid");
							System.out.println(kvm.toString());
							System.out.println(Float.valueOf(kvm.get("total_fee")) / 100);
							Customer cus = (Customer) customerService.findByOpenId(openId).get(0);
							CusAccount cusAccount = cusAccountService.findByCusId(cus.getCusId());
							Float money = cusAccount.getCacChange() + Float.valueOf(kvm.get("total_fee")) / 100;
							cusAccountService.updateField("cacChange", String.valueOf(money), cusAccount.getCacId());
							cta.setCustomer(cus);
							cta.setCtaMoney(Float.valueOf(kvm.get("total_fee")) / 100);
							cta.setCtaId(kvm.get("out_trade_no"));
							cta.setCtaType(1);
							Date date = new Date();
							cta.setCtaTime(new Timestamp(date.getTime()));
							ctaTradingService.addCtaTrading(cta);
							break;

						default:
							break;
						}

						System.out.println("out_trade_no: " + kvm.get("out_trade_no") + " pay SUCCESS!");
						response.getWriter().write(
								"<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[ok]]></return_msg></xml>");
					} else {
						System.out.println("out_trade_no: " + kvm.get("out_trade_no") + " result_code is FAIL");
						response.getWriter().write(
								"<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[result_code is FAIL]]></return_msg></xml>");
					}
				} else {
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

	public void payToIndividual() throws WxPayException {
		String partner_trade_no = new java.util.Date().getTime() + "";
		WxEntPayRequest wxEntPayRequest = new WxEntPayRequest();
		wxEntPayRequest.setPartnerTradeNo(partner_trade_no);
		wxEntPayRequest.setOpenid("ojOQA0y9o-Eb6Aep7uVTdbkJqrP4");
		wxEntPayRequest.setCheckName("NO_CHECK");
		wxEntPayRequest.setAmount(10);
		wxEntPayRequest.setDescription("test");
		wxEntPayRequest.setSpbillCreateIp("10.10.10.10");

		try {
			WxEntPayResult wxEntPayResult = this.wxPayService.entPay(wxEntPayRequest);
			if ("SUCCESS".equals(wxEntPayResult.getResultCode().toUpperCase())
					&& "SUCCESS".equals(wxEntPayResult.getReturnCode().toUpperCase())) {
				System.out.println("企业对个人付款成功！\n付款信息：\n" + wxEntPayResult.toString());
			} else {

				System.out.println("err_code: " + wxEntPayResult.getErrCode() + "  err_code_des: "
						+ wxEntPayResult.getErrCodeDes());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}
