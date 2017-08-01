package com.linestore.action;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.linestore.service.BusinessService;
import com.linestore.service.CateLineService;
import com.linestore.util.ReturnSelectHql;
import com.linestore.util.ReturnUpdateHql;
import com.linestore.vo.Business;
import com.linestore.vo.CateLine;
import com.linestore.vo.ThinkUser;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

public class BusinessAction extends ActionSupport implements ModelDriven<Business>{
	private Business business = new Business();
	
	HttpServletRequest request = ServletActionContext.getRequest ();
	private List<Business> businessList;
	private Business businessResult;
	
	private List<CateLine> cateLineList;
	private CateLineService cateLineService;
	private ThinkUser thinkUser = new ThinkUser();
	
	public void setCateLineService(CateLineService cateLineService) {
		this.cateLineService = cateLineService;
	}

	@Override
	public Business getModel() {
		// TODO Auto-generated method stub
		return business;
	}
	
	private BusinessService businessService;
	
	public void setBusinessService(BusinessService businessService) {
		this.businessService = businessService;
	}

	public String add(){
		business.setBaCountry("中国");
//		business.getBusAddress().add(busAddress);
//		business.getBusBanks().add(busBank);
//		
//		busAddressService.add(busAddress);
//		busBankService.add(busBank);
		String str = business.getBaProvince();
		String strs[] = str.split(" ");
		business.setBaProvince(strs[0]);
		business.setBaCity(strs[1]);
		business.setBaCounty(strs[2]);
		business.setBusStatus(0);
		business.setBusOrgUrl("Public/Uploads/store.png");
		businessService.add(business);
		return SUCCESS;
	}
	
	public String update(){
		
//		if(business.getBusStatus().equals("on")){
//			business.setBusStatus(1);
//		} else {
//			business.setBusStatus(0);
//		}
		
			int id = business.getBusId();
//			business.setBusId(null);
			
			String hql;
			try {
				
				hql = ReturnUpdateHql.ReturnHql(business.getClass(), business, id);
//				System.out.println(business.getBusStatus());
				businessService.update(hql);
				
				
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		return "select";
	}
	
	public String delete(){
		businessService.delete(business);
		businessList = businessService.selectAll();
		ActionContext.getContext().getValueStack().set("list", businessList);
		
		return "selectAll";
	}
	
	public String selectAll(){
		ThinkUser think = new ThinkUser();
		think = (ThinkUser) ActionContext.getContext().getSession().get("admin");
		System.out.println("thinkUser:"+think.getThuArea());
		
		if(think.getThuPid() == 0){
			businessList = businessService.selectAll();
		}else{
			business.setBaCounty(think.getThuArea());
			business.setBaCity(think.getThuArea());
			business.setBaProvince(think.getThuArea());
			
			businessList = (List<Business>) businessService.selectByArea(business);
		}
		
//		System.out.println("list:"+businessList);
//		HttpServletRequest request = ServletActionContext.getRequest ();
//		request.setAttribute("businessList", businessList);
//		System.out.println(businessList.get(0).getBusDistrict());
//		request.setAttribute("list", businessList);
		ActionContext.getContext().getValueStack().set("list", businessList);
		return "selectAll";
	}
	
	public Business selectById(){
		String hql;
		try {
			hql = ReturnSelectHql.ReturnHql(business.getClass(), business);
			businessResult = businessService.select(hql).get(0);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return businessResult;
	}
	
	
	public String read(){
		businessResult = selectById();
		cateLineList = cateLineService.selectAll();
		
		if(businessResult == null){
			return ERROR;
		}else{
			request.setAttribute("roots", cateLineList);
			request.setAttribute("businessResult", businessResult);
			return "read";
		}
	}
	
	public String edit(){
		read();
		return "edit";
	}
	
	public String select(){
		businessResult = businessService.select(business).get(0);
		
		cateLineList = cateLineService.selectAll();
		Map<String, Object> req = (Map<String, Object>) ActionContext.getContext().get("request");
		req.put("roots", cateLineList);
		
		if(businessResult == null){
			return ERROR;
		}else{
			return "select";
		}
	}
	
	public String jump() {
		String js = "<script>YDUI.dialog.alert('申请成功！');</script>";
		Map<String, Object> map = (Map<String, Object>) ActionContext.getContext().get("request");
		map.put("js", js);
		return "gotoCustomer";
	}

	public List<Business> getBusinessList() {
		return businessList;
	}

	public void setBusinessList(List<Business> businessList) {
		this.businessList = businessList;
	}
	
	public Business getBusinessResult() {
		return businessResult;
	}

	public void setBusinessResult(Business businessResult) {
		this.businessResult = businessResult;
	}
}
